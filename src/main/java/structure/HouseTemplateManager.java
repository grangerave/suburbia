package structure;

import java.util.Map;

import com.google.common.collect.Maps;

import net.minecraft.util.datafix.DataFixer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;

public class HouseTemplateManager extends TemplateManager {
	/*
	 * Manager class for storing references to structure HouseTemplates.
	 * (Used to specifically store templates that need dynamic block replacement)
	 */
	
	private final Map<String, HouseTemplate> templates = Maps.<String, HouseTemplate>newHashMap();

	public HouseTemplateManager(String baseFolder, DataFixer fixer) {
		super(baseFolder, fixer);
		
	}

}
