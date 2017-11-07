package grangerave.Suburbia.worldGen;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biome.SpawnListEntry;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.chunk.IChunkGenerator;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.MapGenBase;

public class ChunkProviderSuburb implements IChunkGenerator {
    private final World worldObj;
    private final Random random;
    //private final IBlockState[] cachedBlockIDs = new IBlockState[32];	//is 256 the right height?
    private final Block[] cachedBlocks = {Blocks.BEDROCK,Blocks.STONE,Blocks.STONE,Blocks.DIRT,Blocks.DIRT,Blocks.GRASS};
    private final int minY = 60;
	

	public ChunkProviderSuburb(World worldIn, long seed, boolean generateStructures, String jsonSettings){
		this.worldObj = worldIn;
        this.random = new Random(seed);
        
	}
	

	@Override
	public Chunk provideChunk(int x, int z) {
			//currently just a flatland
	    
	        ChunkPrimer chunkprimer = new ChunkPrimer();
	        
	        /*
	        //go through cachedBlockIDs and set vertical slices to that
	        for (int i = 0; i < this.cachedBlocks.length; ++i)
	        {
	            IBlockState iblockstate = this.cachedBlocks[i].getDefaultState();

	            if (iblockstate != null)
	            {
	                for (int j = 0; j < 16; ++j)
	                {
	                    for (int k = 0; k < 16; ++k)
	                    {
	                        chunkprimer.setBlockState(j, i + minY, k, iblockstate);
	                    }
	                }
	            }
	        }
	        */
	        
	        if(x%4==0||z%4==0){//road
	        	roadChunk(chunkprimer,x%4==0,z%4==0);//xroad
	        }else{ //regular chunk
	        	genericChunk(chunkprimer);
	        }

	        Chunk chunk = new Chunk(this.worldObj, chunkprimer, x, z);
	        /*
	        //biome stuff
	        Biome[] abiome = this.worldObj.getBiomeProvider().getBiomes((Biome[])null, x * 16, z * 16, 16, 16);
	        byte[] abyte = chunk.getBiomeArray();

	        for (int l = 0; l < abyte.length; ++l)
	        {
	            abyte[l] = (byte)Biome.getIdForBiome(abiome[l]);
	        }
			*/
	        
	        chunk.generateSkylightMap();
	        return chunk;
	}
	
	void genericChunk(ChunkPrimer cp){
		//generate a generic flatworld chunk
		//go through cachedBlockIDs and set vertical slices to that
        for (int i = 0; i < this.cachedBlocks.length; ++i)
        {
            IBlockState iblockstate = this.cachedBlocks[i].getDefaultState();

            if (iblockstate != null)
            {
                for (int j = 0; j < 16; ++j)
                {
                    for (int k = 0; k < 16; ++k)
                    {
                        cp.setBlockState(j, i + minY, k, iblockstate);
                    }
                }
            }
        }
	}

	void roadChunk(ChunkPrimer cp,boolean xRoad,boolean zRoad){
		//generate a road chunk
		int len = this.cachedBlocks.length;
		for(int j = 0; j < 16; ++j){
			for(int k = 0; k < 16; ++k){
				for(int i = 0; i< len-2;i++){
					cp.setBlockState(j, i + minY, k, this.cachedBlocks[i].getDefaultState());
				}
				if((xRoad && j>2 && j<13)||(zRoad && k>2 && k<13)){
					//inside of the road
					cp.setBlockState(j, len + minY-2, k, Blocks.GRAVEL.getDefaultState());
					cp.setBlockState(j, len + minY-1, k, Blocks.STONE_SLAB.getDefaultState());
				}else if((xRoad && (j==0 || j==15))||(zRoad && (k==0 || k==15))){
					cp.setBlockState(j, len + minY-2, k, Blocks.DIRT.getDefaultState());
					cp.setBlockState(j, len + minY-1, k, Blocks.COBBLESTONE.getDefaultState());
				}else{
					cp.setBlockState(j, len + minY-2, k, this.cachedBlocks[len-2].getDefaultState());
					cp.setBlockState(j, len + minY-1, k, this.cachedBlocks[len-1].getDefaultState());
				}
			}
		}
	}
	
	
	@Override
	public void populate(int x, int z) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean generateStructures(Chunk chunkIn, int x, int z) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
		Biome biome = this.worldObj.getBiome(pos);
        return biome.getSpawnableList(creatureType);
	}

	@Override
	public void recreateStructures(Chunk chunkIn, int x, int z) {
		// TODO Auto-generated method stub
		
	}


	@Override
	@Nullable
	public BlockPos getStrongholdGen(World worldIn, String structureName,
			BlockPos position) {
		// TODO Auto-generated method stub
		return null;
	}

}
