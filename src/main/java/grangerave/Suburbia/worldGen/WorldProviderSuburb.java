package grangerave.Suburbia.worldGen;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkGenerator;

public class WorldProviderSuburb extends WorldProvider {

	@Override
	public DimensionType getDimensionType() {
		//fake as being an overworld dimension
		return DimensionType.OVERWORLD;
	}
	

	@Override
	public BlockPos getSpawnPoint(){
		//force spawn to be 0 90 0
		return new BlockPos(0, 90, 0); 
	}
	
	@Override
    public boolean canCoordinateBeSpawn(int x, int z){
        return true;
	}
	
	@Override
    public boolean canRespawnHere(){
        return true;
	}

    public String getDimensionName(){
        return "Overworld";
    }
    
    @Override
	public IChunkGenerator createChunkGenerator(){
		return new ChunkProviderSuburb(worldObj, worldObj.getSeed(), worldObj.getWorldInfo().isMapFeaturesEnabled(), worldObj.getWorldInfo().getGeneratorOptions());
    }
    
}
