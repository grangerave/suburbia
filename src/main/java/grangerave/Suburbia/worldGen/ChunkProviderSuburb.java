package grangerave.Suburbia.worldGen;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import grangerave.Suburbia.Suburbia;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirt;
import net.minecraft.block.BlockFalling;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStone;
import net.minecraft.block.BlockStoneSlab;
import net.minecraft.block.BlockStainedHardenedClay;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.MapGenBase;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraft.world.gen.feature.WorldGenLakes;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

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
    
    //Iblockstate caches
    private final IBlockState dirt = Blocks.DIRT.getDefaultState();
    private final IBlockState grass = Blocks.GRASS.getDefaultState();
    private final IBlockState bricks = Blocks.BRICK_BLOCK.getDefaultState();
    private final IBlockState b_stairs = Blocks.BRICK_STAIRS.getDefaultState();
    private final IBlockState sewLiq = Blocks.WATER.getDefaultState();
    private final IBlockState waterPipe = Blocks.IRON_BARS.getDefaultState();
    private final IBlockState intCable = Blocks.NETHER_BRICK_FENCE.getDefaultState();
    
    
    //default chunk Block STATES
    private final IBlockState[][] cachedBlockStates = {
    		{Blocks.BEDROCK.getDefaultState()},
    		{Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.GRANITE)},//hardest rock mix //MIX#2
    		{Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.GRANITE)},//hardest rock mix //MIX#2
    		{Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.ANDESITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.GRANITE)},//hard rock mix
    		{Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.ANDESITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.ANDESITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE)},//hard rock mix / rock //MIX#1
    		{Blocks.STONE.getDefaultState(),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.ANDESITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.ANDESITE),
    			Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT,BlockStone.EnumType.DIORITE)},//rock
    		{Blocks.STONE.getDefaultState(),
    			Blocks.STONE.getDefaultState(),
    			Blocks.STONE.getDefaultState(),
    			Blocks.GRAVEL.getDefaultState()},//rock
    		{dirt.withProperty(BlockDirt.VARIANT, BlockDirt.DirtType.COARSE_DIRT),
    			Blocks.STONE.getDefaultState(),
    			Blocks.GRAVEL.getDefaultState(),
    			Blocks.STAINED_HARDENED_CLAY.getDefaultState().withProperty(BlockStainedHardenedClay.COLOR, EnumDyeColor.SILVER)}, //MIX#2
    		{dirt,
    			dirt,
    			dirt,
    			Blocks.SAND.getDefaultState()}, //MIX#1
    		{grass}
    };
    		
    
    //road block definitions
    private final IBlockState[] roadBlocks = {Blocks.GRAVEL.getDefaultState(),Blocks.STONE_SLAB.getDefaultState()}; //needs to be length 2
    private final IBlockState roadCurbStair = Blocks.STONE_BRICK_STAIRS.getDefaultState();
    private final IBlockState sidewalk = Blocks.DOUBLE_STONE_SLAB.getDefaultState();
    
    
    //generation stuff
    public double px = 0.41;    //chance to remove 2 vertical roads
    public double py= 0.43;  //chance to remove 2 horizontal roads
	

	public ChunkProviderSuburb(World worldIn, long seed, boolean generateStructures, String jsonSettings){
		System.out.println("init ChunkProviderSuburb");
		this.world = worldIn;
        this.random = new Random(seed);
        this.xRandMap = new NoiseGeneratorPerlin(random,1);
        this.yRandMap = new NoiseGeneratorPerlin(random,1);
        this.terrainMap = new NoiseGeneratorPerlin(random,10);
        this.stoneMap1 = new NoiseGeneratorPerlin(random,8);
        this.stoneMap2 = new NoiseGeneratorPerlin(random,4);//noise on top of stoneMap1
        
        this.groundLevel = minY + cachedBlockStates.length;
        
        //debug stuff:
        double runSum = 0.0;
        double runMax = 0.0;
        for(int j=-100;j<100;j++) {
        	double val = (this.stoneMap2.getValue((double)j, (double)0));
        	runSum += val;
        	runMax = val > runMax? val : runMax;
        }
        runSum = runSum / 200.0;
        System.out.print("average of NGP 6 octaves: ");
        System.out.println(runSum);
        System.out.print("max of NGP 6 octaves: ");
        System.out.println(runMax);
	}
	
	public IBlockState getCachedBlockstate(int x, int i, int z) {
		//go through jagged block array
		//if only one block type, set to that
		//if multiple, generate block type with weighted chance
		if((x-20)/10 == 0)
			return Blocks.AIR.getDefaultState();
		

		//return weighted mix of block array
		for(int j=0;j<cachedBlockStates[i].length;j++) {
			// should give 1st block with 50% chance, 2nd with 20%, 3d with 20% and 4th with ~10%
			// (or if no 3d block, 50/50) etc.
			// for some reason noiseGeneratorPerlin isn't normalized...
			// I believe 6 octaves gives a gaussian centered on 0 with sigma = 2*9 and tails reaching to +- ~33
			// 8 octaves gives same with sigma = 2*43 and tails at 115
			if((Math.abs(this.stoneMap1.getValue((double)x, (double)z)) + this.stoneMap2.getValue((double)(x + 2*i), (double)(z + 2*i)) )/130.0 < 0.35 + ((j == (cachedBlockStates[i].length -1)) ? 2.0 : 0.45*Math.sqrt((double)j))) {
				return cachedBlockStates[i][j];
			}
		}
		return Blocks.AIR.getDefaultState();
	}
	
	
	public Chunk generateChunk(int x, int z) {
			//currently just a flatland
	    
	        ChunkPrimer chunkprimer = new ChunkPrimer();

	        //road logic
	        if(x%5==0) { //yroad
	        	if(z%5==0) { //intersection
	        		roadChunk(chunkprimer,true,true,x,z);
	        		//n e s w
	        		sidewalkChunk(chunkprimer, keepRoadsY(x,z-1),keepRoadsX(x+1,z),keepRoadsY(x,z+1),keepRoadsX(x-1,z));
	        		sewerPipes(chunkprimer, keepRoadsY(x,z-1),keepRoadsX(x+1,z),keepRoadsY(x,z+1),keepRoadsX(x-1,z));
	        	}
	        	else if(keepRoadsY(x,z)) {
	        		//roadY
	        		roadChunk(chunkprimer,true,false,x,z);
	        		sidewalkChunk(chunkprimer, true, false, true, false);
	        		sewerPipes(chunkprimer, true, false, true, false);
	        	} else {
	        		genericChunk(chunkprimer,x,z);
	        	}
	        }else if(z%5==0) { //xroad
	        	//remove this road? HORIZONTAL ROAD
	        	if(keepRoadsX(x,z)) {
	        		//roadX
	                roadChunk(chunkprimer,false,true,x,z);
	                sidewalkChunk(chunkprimer,  false, true, false,true);
	                sewerPipes(chunkprimer,  false, true, false,true);
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
		return this.xRandMap.getValue(Math.floorDiv(Chunkx,5),Math.floorDiv(Chunkz,10))>this.px;
	}
	
	public boolean keepRoadsY(int Chunkx, int Chunkz) {
		//did we remove these adjacent VERTICAL roads?
		return 1.0 - this.xRandMap.getValue(Math.floorDiv(Chunkx,10),Math.floorDiv(Chunkz,5))>this.py;
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
        for (int i = 0; i < this.cachedBlockStates.length; ++i)
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

	void roadChunk(ChunkPrimer cp,boolean xRoad,boolean zRoad,int x, int z){
		//generate a road chunk
		// TODO rewrite into Xroad and Yroad
		int len = this.cachedBlockStates.length;
		for(int j = 0; j < 16; ++j){
			for(int k = 0; k < 16; ++k){
				for(int i = 0; i< len-2;i++){
					//this.cachedBlocks[i].getDefaultState()
					cp.setBlockState(j, i + minY, k, this.getCachedBlockstate(16*x + j, i, 16*z + k));
				}
				if((xRoad && j>2 && j<13)||(zRoad && k>2 && k<13)){
					//inside of the road
					//under road
					//cp.setBlockState(j, groundLevel-2, k, this.roadBlocks[0]);
					//top of road
					//cp.setBlockState(j, groundLevel-1, k, this.roadBlocks[1]);
				}else{
					cp.setBlockState(j, groundLevel-2, k, this.getCachedBlockstate(16*x + j, len-2, 16*z + k));
					cp.setBlockState(j, groundLevel-1, k, this.getCachedBlockstate(16*x + j, len-1, 16*z + k));

				}
			}
		}
	}
	
	void sidewalkChunk(ChunkPrimer cp, boolean n, boolean e, boolean s, boolean w) {
		//n,e,s,w: is road attached in corresponding direction?
		
		int len = this.cachedBlockStates.length;
		boolean straightRoad = (n && s) || (e && w);
		for(int j = 0; j<16; ++j) {
			//sidewalk
			if(!n) {
				cp.setBlockState(j, groundLevel-2, 0, dirt);
				cp.setBlockState(j, groundLevel-1, 0, sidewalk);
				if((w || j>1) && (e || j<14) || straightRoad) {
					//grass
					cp.setBlockState(j, groundLevel-2, 1, dirt);
					cp.setBlockState(j, groundLevel-1, 1, grass);
					//stairs
					cp.setBlockState(j, groundLevel-2, 2, dirt);
					cp.setBlockState(j, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				}
			}
			if(!s) {
				cp.setBlockState(j, groundLevel-2, 15, dirt);
				cp.setBlockState(j, groundLevel-1, 15, sidewalk);
				if((w || j>1) && (e || j<14) || straightRoad) {
					//grass
					cp.setBlockState(j, groundLevel-2, 14, dirt);
					cp.setBlockState(j, groundLevel-1, 14, grass);
					//stairs
					cp.setBlockState(j, groundLevel-2, 13, dirt);
					cp.setBlockState(j, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				}
			}
			if(!e) {
				cp.setBlockState(15, groundLevel-2, j, dirt);
				cp.setBlockState(15, groundLevel-1, j, sidewalk);
				if((n || j>1) && (s || j<14) || straightRoad) {
					//grass
					cp.setBlockState(14, groundLevel-2, j, dirt);
					cp.setBlockState(14, groundLevel-1, j, grass);
					//stairs
					cp.setBlockState(13, groundLevel-2, j, dirt);
					cp.setBlockState(13, groundLevel-1, j, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.EAST));
				}
			}
			if(!w) {
				cp.setBlockState(0, groundLevel-2, j, dirt);
				cp.setBlockState(0, groundLevel-1, j, sidewalk);
				if((n || j>1) && (s || j<14)|| straightRoad) {
					//grass
					cp.setBlockState(1, groundLevel-2, j, dirt);
					cp.setBlockState(1, groundLevel-1, j, grass);
					//stairs
					cp.setBlockState(2, groundLevel-2, j, dirt);
					cp.setBlockState(2, groundLevel-1, j, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.WEST));
				}
			}
			//check for corners
			if(n && w) { //NW
				//sidewalk
				cp.setBlockState(0, groundLevel-1, 0, sidewalk);
				cp.setBlockState(0, groundLevel-1, 1, sidewalk);
				cp.setBlockState(1, groundLevel-1, 0, sidewalk);
				//stairs
				cp.setBlockState(0, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(1, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(2, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(2, groundLevel-1, 1, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.WEST));
				cp.setBlockState(2, groundLevel-1, 0, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.WEST));
			}
			if(n && e) { //NE
				cp.setBlockState(15, groundLevel-1, 0, sidewalk);
				cp.setBlockState(15, groundLevel-1, 1, sidewalk);
				cp.setBlockState(14, groundLevel-1, 0, sidewalk);
				//stairs
				cp.setBlockState(15, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(14, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(13, groundLevel-1, 2, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
				cp.setBlockState(13, groundLevel-1, 1, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.EAST));
				cp.setBlockState(13, groundLevel-1, 0, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.EAST));
			}
			if(s && w) { //SW
				cp.setBlockState(0, groundLevel-1, 15, sidewalk);
				cp.setBlockState(0, groundLevel-1, 14, sidewalk);
				cp.setBlockState(1, groundLevel-1, 15, sidewalk);
				//stairs
				cp.setBlockState(0, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(1, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(2, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(2, groundLevel-1, 14, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.WEST));
				cp.setBlockState(2, groundLevel-1, 15, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.WEST));
			}
			if(s && e) { //SE
				cp.setBlockState(15, groundLevel-1, 15, sidewalk);
				cp.setBlockState(15, groundLevel-1, 14, sidewalk);
				cp.setBlockState(14, groundLevel-1, 15, sidewalk);
				//stairs
				cp.setBlockState(15, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(14, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(13, groundLevel-1, 13, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
				cp.setBlockState(13, groundLevel-1, 14, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.EAST));
				cp.setBlockState(13, groundLevel-1, 15, roadCurbStair.withProperty(BlockStairs.FACING, EnumFacing.EAST));
			}
		}
		
	}
	
	void sewerPipes(ChunkPrimer cp, boolean n, boolean e, boolean s, boolean w) {
		
		
		int center = 8;
		
		int sewerHeight = groundLevel-4;
		int waterMainHeight = groundLevel - 7;
		int intHeight = groundLevel - 9;
		
		//messy solution, involves overwriting blocks when overlap exists...
		for(int i=(w?0:center);i<(e?16:center);++i) {
			//sewer
			cp.setBlockState(i, sewerHeight+1, center, bricks);
			cp.setBlockState(i, sewerHeight+1, center-1, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
			cp.setBlockState(i, sewerHeight+1, center+1, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.NORTH));
			cp.setBlockState(i, sewerHeight, center-1, bricks);
			cp.setBlockState(i, sewerHeight, center, sewLiq);
			cp.setBlockState(i, sewerHeight, center+1, bricks);
			cp.setBlockState(i, sewerHeight-1, center, bricks);
			cp.setBlockState(i, sewerHeight-1, center-1, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.SOUTH).withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP));
			cp.setBlockState(i, sewerHeight-1, center+1, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.NORTH).withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP));
			
			//water main
			cp.setBlockState(i, waterMainHeight, center, waterPipe);
			
			//internet
			cp.setBlockState(i, intHeight, center, intCable);
			
		}
		
		for(int i=(n?0:center);i<(s?16:center);++i) {
			//sewer
			cp.setBlockState(center, sewerHeight+1, i, bricks);
			if(cp.getBlockState(center-1, sewerHeight+1, i) != bricks)
				cp.setBlockState(center-1, sewerHeight+1, i, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.EAST));
			if(cp.getBlockState(center+1, sewerHeight+1, i) != bricks)
				cp.setBlockState(center+1, sewerHeight+1, i, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.WEST));
				
			if(cp.getBlockState(center-1, sewerHeight, i) != sewLiq)
				cp.setBlockState(center-1, sewerHeight, i, bricks);
			cp.setBlockState(center, sewerHeight, i, sewLiq);
			if(cp.getBlockState(center+1, sewerHeight, i) != sewLiq)
				cp.setBlockState(center+1, sewerHeight, i, bricks);
			
			cp.setBlockState(center, sewerHeight-1, i, bricks);
			if(cp.getBlockState(center-1, sewerHeight-1, i) != bricks)
				cp.setBlockState(center-1, sewerHeight-1, i, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.EAST).withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP));
			if(cp.getBlockState(center+1, sewerHeight-1, i) != bricks)
				cp.setBlockState(center+1, sewerHeight-1, i, b_stairs.withProperty(BlockStairs.FACING, EnumFacing.WEST).withProperty(BlockStairs.HALF, BlockStairs.EnumHalf.TOP));
			
			//water main
			cp.setBlockState(center, waterMainHeight, i, waterPipe);
			
			//internet
			cp.setBlockState(center, intHeight, i, intCable);
			
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
	
	private void makeTestHouse(int Chunkx, int Chunkz,EnumFacing direction) {
		//HouseTemplate template = new HouseTemplate(new TemplateManager("House_A01", null), String "House_A01", BlockPos pos, Rotation rot);
		BlockPos pos = new BlockPos(Chunkx*16, groundLevel, Chunkz*16);
		
		WorldServer worldserver = (WorldServer) world;
		MinecraftServer minecraftserver = world.getMinecraftServer();
		TemplateManager templatemanager = worldserver.getStructureTemplateManager();
		Template template = templatemanager.getTemplate(minecraftserver, new ResourceLocation(Suburbia.MODID ,"house_a01"));
		if(template == null) {
			System.out.println(Suburbia.MODID);
			System.out.println("No Structure found named 'house_a01' !!");
			return;
		}
		
		//Figure out rotation of the house
		
		Rotation rotation = Rotation.NONE;
		switch(direction) {
			case EAST:
				rotation = Rotation.COUNTERCLOCKWISE_90;
				break;
			case WEST:
				rotation = Rotation.CLOCKWISE_90;
				break;
			case NORTH:
				rotation = Rotation.CLOCKWISE_180;
				break;
			default:
				rotation = Rotation.NONE;
				break;
				
				
		}
		BlockPos offset = new BlockPos(-10,0,-7).rotate(rotation);
		
		PlacementSettings placementsettings = (new PlacementSettings()).setMirror(Mirror.NONE)
				.setRotation(rotation).setIgnoreEntities(false).setChunk((ChunkPos) null)
				.setReplacedBlock((Block) null).setIgnoreStructureBlock(false);

		template.addBlocksToWorld(world, pos.add(16,0,16).add(offset), placementsettings, 2|16);
		
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
		//BlockFalling.fallInstantly = true;
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        Biome biome = this.world.getBiome(blockpos.add(16, 0, 16));
        this.random.setSeed(this.world.getSeed());
        long k = this.random.nextLong() / 2L * 2L + 1L;
        long l = this.random.nextLong() / 2L * 2L + 1L;
        this.random.setSeed((long)x * k + (long)z * l ^ this.world.getSeed());
        ChunkPos chunkpos = new ChunkPos(x, z);

        if(this.isHouseRoot(x, z)) {
        	//makeDebugHouse(x, z, getHouseDirection(x, z));
        	makeTestHouse(x, z, getHouseDirection(x, z));
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
