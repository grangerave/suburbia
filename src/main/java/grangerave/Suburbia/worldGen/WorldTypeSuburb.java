package grangerave.Suburbia.worldGen;

import grangerave.Suburbia.Suburbia;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class WorldTypeSuburb extends WorldType{

	public WorldTypeSuburb(String name) {
		super(name);
		System.out.println("initializing worldTypeSuburb");
	}

    @Override
    public boolean isCustomizable()
    {
        return false;
    }
	
    
    @Override
    public net.minecraft.world.gen.IChunkGenerator getChunkGenerator(World world, String generatorOptions) {
    	return new ChunkProviderSuburb(world, world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(), generatorOptions);
    }
    
    @SideOnly(Side.CLIENT)
    public void onCustomizeButton(net.minecraft.client.Minecraft mc, net.minecraft.client.gui.GuiCreateWorld guiCreateWorld)
    {
        if (this == WorldType.FLAT)
        {
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiCreateFlatWorld(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson));
        }
        else if (this == Suburbia.getSuburbs())
        {
            mc.displayGuiScreen(new net.minecraft.client.gui.GuiCustomizeWorldScreen(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson));
        }
    }

}
