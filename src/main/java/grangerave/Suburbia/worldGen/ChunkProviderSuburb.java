package grangerave.Suburbia.worldGen;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.feature.WorldGenLakes;

public class ChunkProviderSuburb implements IChunkGenerator {
    private final World world;
    //random generators
    private final Random random;
    private final NoiseGeneratorPerlin xRandMap;
    private final NoiseGeneratorPerlin yRandMap;
    private final NoiseGeneratorPerlin terrainMap;
    //stonetype generators
    private final NoiseGeneratorPerlin stoneMap1;
    private final NoiseGeneratorPerlin stoneMap2;
    
    private final int minY = 20;
    private int groundLevel = 20;
    
    //default chunk blocks
    private final Block[] cachedBlocks = {Blocks.BEDROCK,Blocks.STONE,Blocks.STONE,Blocks.DIRT,Blocks.DIRT,Blocks.GRASS};
    private final Block[][] cachedBlocksMulti = {
    		{Blocks.BEDROCK},
    		{Blocks.STONE,Blocks.STONE,Blocks.STONE},//hardest rock mix //MIX#2
    		{Blocks.STONE,Blocks.STONE,Blocks.STONE},//hardest rock mix //MIX#2
    		{Blocks.STONE,Blocks.STONE},//hard rock mix
    		{Blocks.STONE,Blocks.STONE,Blocks.STONE},//hard rock mix / rock //MIX#1
    		{Blocks.STONE},//rock
    		{Blocks.DIRT,Blocks.STONE}, //MIX#2
    		{Blocks.AIR},//{Blocks.DIRT,Blocks.DIRT,Blocks.SAND,Blocks.HARDENED_CLAY}, //MIX#1
    		{Blocks.AIR}//{Blocks.GRASS}
    		};
    //specify any terrain blocks that need properties (in order of hardness)
    private final BlockStone.EnumType StoneTypes[] = {BlockStone.EnumType.GRANITE,BlockStone.EnumType.DIORITE,BlockStone.EnumType.ANDESITE,BlockStone.EnumType.STONE};
    private final int[][] cachedStoneTypes = {
    		{0},
    		{0,1,2},
    		{0,1,2},
    		{1,2},
    		{3,1,2},
    		{3},
    		{3,3}};
    //road block definitions
    private final Block[] roadBlocks = {Blocks.GRAVEL,Blocks.STONE_SLAB}; //needs to be length 2
    private final Block roadCurbStair = Blocks.STONE_BRICK_STAIRS;
    private final Block sidewalk = Blocks.STONE_SLAB;
    
    //generation stuff
    public double px = 0.41;    //chance to remove 2 vertical roads
    public double py= 0.43;  //chance to remove 2 horizontal roads
	

	public ChunkProviderSuburb(World worldIn, long seed, boolean generateStructures, String jsonSettings){
		System.out.println("init ChunkProviderSuburb");
		this.world = worldIn;
        this.random = new Random(seed);
        this.xRandMap = new NoiseGeneratorPerlin(random, 1);
        this.yRandMap = new NoiseGeneratorPerlin(random, 1);
        this.terrainMap = new NoiseGeneratorPerlin(random,10);
        this.stoneMap1 = new NoiseGeneratorPerlin(random,6);
        this.stoneMap2 = new NoiseGeneratorPerlin(random,6);
        
        this.groundLevel = minY + cachedBlocksMulti.length;
        for(int j=-11;j<12;j+=4) {
        	System.out.println(Math.floorDiv(j,10));
        }
	}
	
	public IBlockState getCachedBlockstate(int x, int i, int z) {
		
		if(cachedBlocksMulti[i].length>1)  {
			//return weighted mix of block array
			for(int j=0;j<cachedBlocksMulti[i].length;j++) {
				if(x==0 && z==0 && i==0)
					System.out.println(this.stoneMap1.getValue(x, z));
				if((this.stoneMap1.getValue((double)x, (double)z) + 2)*0.5 < 0.5 + 0.2*j) {
					if (cachedBlocksMulti[i][j]==Blocks.STONE){
						return cachedBlocksMulti[i][j].getDefaultState().withProperty(BlockStone.VARIANT, StoneTypes[cachedStoneTypes[i][j]]);
					}else
						return cachedBlocksMulti[i][j].getDefaultState();
				}
			}
		}
		if (cachedBlocksMulti[i][0]==Blocks.STONE){
			return cachedBlocksMulti[i][0].getDefaultState().withProperty(BlockStone.VARIANT, StoneTypes[cachedStoneTypes[i][0]]);
		}else
			return cachedBlocksMulti[i][0].getDefaultState();
	}
	
	
	public Chunk generateChunk(int x, int z) {
			//currently just a flatland
	    
	        ChunkPrimer chunkprimer = new ChunkPrimer();

	        //road logic
	        if(x%5==0) { //yroad
	        	if(z%5==0) { //intersection
	        		roadChunk(chunkprimer,true,true);
	        		//n e s w
	        		sidewalkChunk(chunkprimer, keepRoadsY(x,z-1),keepRoadsX(x+1,z),keepRoadsY(x,z+1),keepRoadsX(x-1,z));
	        	}
	        	else if(keepRoadsY(x,z)) {
	        		//roadY
	        		roadChunk(chunkprimer,true,false);
	        		sidewalkChunk(chunkprimer, true, false, true, false);
	        	} else {
	        		genericChunk(chunkprimer,x,z);
	        	}
	        }else if(z%5==0) { //xroad
	        	//remove this road? HORIZONTAL ROAD
	        	if(keepRoadsX(x,z)) {
	        		//roadX
	                roadChunk(chunkprimer,false,true);
	                sidewalkChunk(chunkprimer,  false, true, false,true);
	        	} else{
	        		genericChunk(chunkprimer,x,z);
	        	}
	        } else {
	        	genericChunk(chunkprimer,x,z);
	        }
	        

	        Chunk chunk = new Chunk(this.world, chunkprimer, x, z);
	        /*
	        //biome stuff
	        Biome[] abiome = this.world.getBiomeProvider().getBiomes((Biome[])null, x * 16, z * 16, 16, 16);

	        byte[] abyte = chunk.getBiomeArray();

	        for (int l = 0; l < abyte.length; ++l)
	        {
	            abyte[l] = (byte)Biome.getIdForBiome(abiome[l]);
	        }
			*/
	        
	        chunk.generateSkylightMap();
	        return chunk;
	}
	
	public boolean keepRoadsX(int Chunkx, int Chunkz){
		//did we remove these adjacent HORIZONTAL roads?
		return this.xRandMap.getValue(Math.floorDiv(Chunkx,10),Math.floorDiv(Chunkz,10))>this.px;
	}
	
	public boolean keepRoadsY(int Chunkx, int Chunkz) {
		//did we remove these adjacent VERTICAL roads?
		return 1.0 - this.xRandMap.getValue(Math.floorDiv(Chunkx,10),Math.floorDiv(Chunkz,10))>this.py;
	}
	
	public boolean isRoad(int Chunkx, int Chunkz) {
		if(Chunkx%5 == 0) { //yroad
			if(Chunkz%5 == 0) { //intersection
        		return true;
        	}
        	if(keepRoadsY(Chunkx,Chunkz)) {
        		//roadY
        		return true;
        	}
			return false;
		} else if (Chunkz%5 == 0 ) { //xroad
			if(keepRoadsX(Chunkx,Chunkz)) {
        		//roadY
        		return true;
        	}
			return false;
		}else {
			return false;
		}
	}
	
	void genericChunk(ChunkPrimer cp,int Chunkx,int Chunkz){
		//generate a generic flatworld chunk
		
		//slope intercept x direction (a  + bx)
		double a = (double) ((this.isRoad(Chunkx-1, Chunkz) ? 0 : 1) + (this.isRoad(Chunkx, Chunkz-1) ? 0 : 1 )) - 1.0 ;
		double b = (double) ((this.isRoad(Chunkx+1, Chunkz) ? 0 : 1 ) - (this.isRoad(Chunkx-1, Chunkz) ? 0 : 1 ));
		//slope intercept y direction (c  + dy)
		double d = (double) ((this.isRoad(Chunkx, Chunkz+1) ? 0 : 1 ) - (this.isRoad(Chunkx, Chunkz-1) ? 0 : 1 ));
		//account for corners
		if(b == 0.0 && d==0.0) {
			if(this.isRoad(Chunkx+1, Chunkz+1)) {
				a+=1.0;
				b-=1.0;
				d-=1.0;
			}else if(this.isRoad(Chunkx+1, Chunkz-1)) {
				b-=1.0;
				d+=1.0;
			}else if(this.isRoad(Chunkx-1, Chunkz+1)) {
				b+=1.0;
				d-=1.0;
			}else if(this.isRoad(Chunkx-1, Chunkz-1)) {
				a-=1.0;
				b+=1.0;
				d+=1.0;
			}
		}
		
		//go through cachedBlockIDs and set vertical slices to that
        for (int i = 0; i < this.cachedBlocksMulti.length; ++i)
        {
            IBlockState iblockstate = this.getCachedBlockstate(Chunkx, i, Chunkz);

            if (iblockstate != null)
            {
                for (int j = 0; j < 16; ++j)
                {
                    for (int k = 0; k < 16; ++k)
                    {
                    	iblockstate = this.getCachedBlockstate(Chunkx*16+j, i, Chunkz*16+k);
                    	//calculate the terrain height
                    	//weight for random noise vs. flat
                    	double weight = Math.min(Math.max((15*a + b*j + d*k - 5.0),0.0),15.0 - 5.0) / 15.0;
                    	int height = (int)((0.011*this.terrainMap.getValue(j + 16*Chunkx, k + 16*Chunkz)-1.0)*weight);

                        cp.setBlockState(j, i + minY + height, k, iblockstate);
                    }
                }
            }
        }
	}

	void roadChunk(ChunkPrimer cp,boolean xRoad,boolean zRoad){
		//generate a road chunk
		// TODO rewrite into Xroad and Yroad
		int len = this.cachedBlocksMulti.length;
		for(int j = 0; j < 16; ++j){
			for(int k = 0; k < 16; ++k){
				for(int i = 0; i< len-2;i++){
					//this.cachedBlocks[i].getDefaultState()
					cp.setBlockState(j, i + minY, k, this.getCachedBlockstate(j, i, k));
				}
				if((xRoad && j>2 && j<13)||(zRoad && k>2 && k<13)){
					//inside of the road
					//under road
					cp.setBlockState(j, groundLevel-2, k, this.roadBlocks[0].getDefaultState());
					//top of road
					cp.setBlockState(j, groundLevel-1, k, this.roadBlocks[1].getDefaultState());
				}else{
					cp.setBlockState(j, groundLevel-2, k, this.cachedBlocksMulti[len-2][0].getDefaultState());
					cp.setBlockState(j, groundLevel-1, k, this.cachedBlocksMulti[len-1][0].getDefaultState());

				}
			}
		}
	}
	
	void sidewalkChunk(ChunkPrimer cp, boolean n, boolean e, boolean s, boolean w) {
		//n,e,s,w: is road attached in corresponding direction?
		int len = this.cachedBlocks.length;
		boolean straightRoad = (n && s) || (e && w);
		for(int j = 0; j<16; ++j) {
			//sidewalk
			if(!n) {
				cp.setBlockState(j, groundLevel-2, 0, Blocks.DIRT.getDefaultState());
				cp.setBlockState(j, groundLevel-1, 0, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				if((w || j>1) && (e || j<14) || straightRoad) {
					//grass
					cp.setBlockState(j, groundLevel-2, 1, Blocks.DIRT.getDefaultState());
					cp.setBlockState(j, groundLevel-1, 1, Blocks.GRASS.getDefaultState());
					//stairs
					cp.setBlockState(j, groundLevel-2, 2, Blocks.DIRT.getDefaultState());
					cp.setBlockState(j, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				}
			}
			if(!s) {
				cp.setBlockState(j, groundLevel-2, 15, Blocks.DIRT.getDefaultState());
				cp.setBlockState(j, groundLevel-1, 15, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				if((w || j>1) && (e || j<14) || straightRoad) {
					//grass
					cp.setBlockState(j, groundLevel-2, 14, Blocks.DIRT.getDefaultState());
					cp.setBlockState(j, groundLevel-1, 14, Blocks.GRASS.getDefaultState());
					//stairs
					cp.setBlockState(j, groundLevel-2, 13, Blocks.DIRT.getDefaultState());
					cp.setBlockState(j, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				}
			}
			if(!e) {
				cp.setBlockState(15, groundLevel-2, j, Blocks.DIRT.getDefaultState());
				cp.setBlockState(15, groundLevel-1, j, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				if((n || j>1) && (s || j<14) || straightRoad) {
					//grass
					cp.setBlockState(14, groundLevel-2, j, Blocks.DIRT.getDefaultState());
					cp.setBlockState(14, groundLevel-1, j, Blocks.GRASS.getDefaultState());
					//stairs
					cp.setBlockState(13, groundLevel-2, j, Blocks.DIRT.getDefaultState());
					cp.setBlockState(13, groundLevel-1, j, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));
				}
			}
			if(!w) {
				cp.setBlockState(0, groundLevel-2, j, Blocks.DIRT.getDefaultState());
				cp.setBlockState(0, groundLevel-1, j, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				if((n || j>1) && (s || j<14)|| straightRoad) {
					//grass
					cp.setBlockState(1, groundLevel-2, j, Blocks.DIRT.getDefaultState());
					cp.setBlockState(1, groundLevel-1, j, Blocks.GRASS.getDefaultState());
					//stairs
					cp.setBlockState(2, groundLevel-2, j, Blocks.DIRT.getDefaultState());
					cp.setBlockState(2, groundLevel-1, j, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST));
				}
			}
			//check for corners
			if(n && w) { //NW
				//sidewalk
				cp.setBlockState(0, groundLevel-1, 0, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(0, groundLevel-1, 1, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(1, groundLevel-1, 0, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				//stairs
				cp.setBlockState(0, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(1, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(2, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(2, groundLevel-1, 1, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST));
				cp.setBlockState(2, groundLevel-1, 0, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST));
			}
			if(n && e) { //NE
				cp.setBlockState(15, groundLevel-1, 0, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(15, groundLevel-1, 1, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(14, groundLevel-1, 0, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				//stairs
				cp.setBlockState(15, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(14, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(13, groundLevel-1, 2, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(13, groundLevel-1, 1, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));
				cp.setBlockState(13, groundLevel-1, 0, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));
			}
			if(s && w) { //SW
				cp.setBlockState(0, groundLevel-1, 15, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(0, groundLevel-1, 14, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(1, groundLevel-1, 15, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				//stairs
				cp.setBlockState(0, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(1, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(2, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(2, groundLevel-1, 14, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST));
				cp.setBlockState(2, groundLevel-1, 15, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.WEST));
			}
			if(s && e) { //SE
				cp.setBlockState(15, groundLevel-1, 15, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(15, groundLevel-1, 14, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				cp.setBlockState(14, groundLevel-1, 15, Blocks.STONE_SLAB.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP));
				//stairs
				cp.setBlockState(15, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(14, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(13, groundLevel-1, 13, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(13, groundLevel-1, 14, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));
				cp.setBlockState(13, groundLevel-1, 15, roadCurbStair.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.EAST));
			}
		}
		
	}
	
	public boolean isHouseRoot(int Chunkx, int Chunkz) {
		//return true if valid chunk for bottom left of house
		//no rotation info returned!
		
		boolean flag = true;
		int bx = Math.floorMod(Chunkx,5);
		int bz = Math.floorMod(Chunkz,5);
		if(Math.floorMod(Chunkx,10)>4) {//odd chunk in X
			if(Math.floorMod(Chunkz,10)>4) {//odd chunk in Y (always square)
				//TODO: go through all 4 positions, test for houses
				//for now just return true if on a house slot
				//square
				flag = false;
				flag = flag || (bx==1 && bz==1) && (isRoad(Chunkx-1,Chunkz) || isRoad(Chunkx,Chunkz-1)); //SW
				flag = flag || (bx==1 && bz==3) && (isRoad(Chunkx-1,Chunkz) || isRoad(Chunkx,Chunkz+2)); //NW
				flag = flag || (bx==3 && bz==1) && (isRoad(Chunkx+2,Chunkz) || isRoad(Chunkx,Chunkz-1)); //SE
				flag = flag || (bx==3 && bz==3) && (isRoad(Chunkx+2,Chunkz) || isRoad(Chunkx,Chunkz+2)); //NE
				
			}else {//even chunk in Y (could be shifted in X)
				if(!keepRoadsX(Chunkx,Chunkz)) { //roads removed
					flag = ((Math.floorMod(Chunkx, 5)==3)&&keepRoadsY(Chunkx+5,Chunkz)) || ((Math.floorMod(Chunkx, 5)==1)&&keepRoadsY(Chunkx-5,Chunkz));
					return (flag && Math.floorMod(Chunkx,5)%2==1 && Math.floorMod(Chunkz,5)%2==0);
				}//roads remain
				//square
			}
		}else {//even chunk in X (potentially shifted)
			if(Math.floorMod(Chunkz,10)>4) {//odd chunk in Y (could be shifted in X)
				if(!keepRoadsY(Chunkx,Chunkz)) { //roads removed
					flag = ((Math.floorMod(Chunkz, 5)==3)&&keepRoadsX(Chunkx,Chunkz+5)) || ((Math.floorMod(Chunkz, 5)==1)&&keepRoadsX(Chunkx,Chunkz-5));
					return (flag && Math.floorMod(Chunkx,5)%2==0);
				}
			}else {//both chunks even
				if(!keepRoadsY(Chunkx,Chunkz)) { //roads Y removed
					if(keepRoadsX(Chunkx,Chunkz)) {//roads X remain
						return (flag && Math.floorMod(Chunkx,5)%2==0 && Math.floorMod(Chunkz,5)%2==1);
					}
				}else if(!keepRoadsX(Chunkx,Chunkz)) { //roads removed
					if(keepRoadsY(Chunkx,Chunkz)) {
						return (flag && Math.floorMod(Chunkx,5)%2==1 && Math.floorMod(Chunkz,5)%2==0);
					}
					//both roads removed
				}
				
			}
		}
		//default to square
		return (flag && Math.floorMod(Chunkx,5)%2==1 && Math.floorMod(Chunkz,5)%2==1);
	}
	
	public EnumFacing getHouseDirection(int Chunkx,int Chunkz) {
		int bx = Math.floorMod(Chunkx,5);
		int bz = Math.floorMod(Chunkz,5);
		
		//determine if even x
		if (bx==1) { //quad X spacing WEST
			if(isRoad(Chunkx-1,Chunkz))//is road adjacent?
				return EnumFacing.WEST;
			//if(bz==1 || bz==3)// quad Y spacing
				return isRoad(Chunkx,Chunkz-1) ? EnumFacing.NORTH : EnumFacing.SOUTH; //north?
				
		}else if (bx==3) {//quad spacing EAST
			if(isRoad(Chunkx+2,Chunkz))
				return EnumFacing.EAST;
			return isRoad(Chunkx,Chunkz-1) ? EnumFacing.NORTH : EnumFacing.SOUTH;
			
		}else {//long spacing, y spacing must be quad
			return isRoad(Chunkx,Chunkz-1) ? EnumFacing.NORTH : EnumFacing.SOUTH;
			
		}
	}
	
	
	private void makeDebugHouse(int Chunkx, int Chunkz,EnumFacing direction) {
		//temporary function to make a house shape out of bookcases
		//T shape with flat end facing road
		//remember, N and S are switched!
		final int TeeSize = 25;
		
		BlockPos blockpos = new BlockPos(Chunkx*16, 0, Chunkz*16);
		switch(direction) {
			case SOUTH:
				for(int i=0;i<TeeSize;i++) {
					this.world.setBlockState(blockpos.add(i,groundLevel,31), Blocks.BOOKSHELF.getDefaultState(), 2|16);
					this.world.setBlockState(blockpos.add(16,groundLevel,31-i), Blocks.BOOKSHELF.getDefaultState(), 2|16);
				}
				break;
			case EAST: //positive X
				for(int i=0;i<TeeSize;i++) {
					this.world.setBlockState(blockpos.add(31,groundLevel,i), Blocks.BOOKSHELF.getDefaultState(), 2|16);
					this.world.setBlockState(blockpos.add(31 - i,groundLevel,16), Blocks.BOOKSHELF.getDefaultState(), 2|16);
				}
				break;
			case WEST:
				for(int i=0;i<TeeSize;i++) {
					this.world.setBlockState(blockpos.add(0,groundLevel,i), Blocks.BOOKSHELF.getDefaultState(), 2|16);
					this.world.setBlockState(blockpos.add(i+1,groundLevel,16), Blocks.BOOKSHELF.getDefaultState(), 2|16);
				}
				break;
			default: //facing north
				for(int i=0;i<TeeSize;i++) {
					this.world.setBlockState(blockpos.add(i,groundLevel,0), Blocks.BOOKSHELF.getDefaultState(), 2|16);
					this.world.setBlockState(blockpos.add(16,groundLevel,i+1), Blocks.BOOKSHELF.getDefaultState(), 2|16);
				}
        		break;
        	
		}
		
	}
	
	
	
	@Override
	public void populate(int x, int z) {
		//generate structures etc. post main block generation (has to be within chunk) <- apparently not, if you set flag to 2|16
		BlockFalling.fallInstantly = true;
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        Biome biome = this.world.getBiome(blockpos.add(16, 0, 16));
        this.random.setSeed(this.world.getSeed());
        long k = this.random.nextLong() / 2L * 2L + 1L;
        long l = this.random.nextLong() / 2L * 2L + 1L;
        this.random.setSeed((long)x * k + (long)z * l ^ this.world.getSeed());
        ChunkPos chunkpos = new ChunkPos(x, z);
        
        
        /*
        if(x==0) {
        	for(i=0;i<20;i++) {
        		this.world.setBlockState(blockpos.add(i,this.minY + 10,0), Blocks.BOOKSHELF.getDefaultState(), 2|16);
        	}
	        //test lakes
	        int i1 = this.random.nextInt(16) + 8;
	        int j1 = this.random.nextInt(256);
	        int k1 = this.random.nextInt(16) + 8;
	        (new WorldGenLakes(Blocks.WATER)).generate(this.world, this.random, blockpos.add(i1, j1, k1));
        }*/
        if(this.isHouseRoot(x, z)) {
        	makeDebugHouse(x, z, getHouseDirection(x, z));
        }
        
		
	}

	@Override
	public boolean generateStructures(Chunk chunkIn, int x, int z) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		Biome biome = this.world.getBiome(pos);
        return biome.getSpawnableList(creatureType);
	}

	@Override
	public void recreateStructures(Chunk chunkIn, int x, int z) {
		// TODO Auto-generated method stub
		
	}


	@Nullable
	public BlockPos getStrongholdGen(World worldIn, String structureName,
			BlockPos position) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position,
			boolean findUnexplored) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
		// TODO Auto-generated method stub
		return false;
	}
	

}
