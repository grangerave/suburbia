package grangerave.Suburbia;

import grangerave.Suburbia.worldGen.*;

import net.minecraft.init.Blocks;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;

@Mod(modid = Suburbia.MODID, version = Suburbia.VERSION)
public class Suburbia {
	public static final String MODID = "suburbia";
    public static final String VERSION = "0.1.1";
    //suburb dimension id number
    public static int dimId = 0;
    //suburb world
    static WorldType SUBURBS;
    
    @EventHandler
    public void preInit(FMLInitializationEvent event){
    	//PRE initialization
    	System.out.println("=====================================");
        System.out.println("PRE-Initializing Suburbia...");
    }
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        //initialization
        System.out.println("=====================================");
        System.out.println("Initializing SubUrbia...");
        System.out.println("Registering SubUrban Dimension");
        //register the dimension
        DimensionManager.unregisterDimension(dimId);
        DimensionManager.registerDimension(dimId, DimensionType.register("SUBURB", "_suburb", dimId, WorldProviderSuburb.class, true));
    }
    
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		//post initialization
		SUBURBS = new WorldTypeSuburb("SUBURBS");
	}
	
	public static WorldType getSuburbs() {
		return SUBURBS;
	}

}
