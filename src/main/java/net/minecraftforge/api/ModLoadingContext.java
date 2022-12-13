package net.minecraftforge.api;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

/**
 * class to mimic Forge's ModLoadingContext, name is misleading in this case though, as this is only used for registering configs
 */
@SuppressWarnings("unused")
public final class ModLoadingContext {
	/**
	 * Register a new mod config, only difference from registering on Forge is <code>modId</code> has to be provided as there is no loading context to get that information from
	 *
	 * @param modId mod id of your mod
	 * @param type  type of this mod config (client, common, or server)
	 * @param spec  the built config spec
	 * @return the {@link ModConfig} instance
	 * @throws java.lang.IllegalArgumentException when no mod container is found for <code>modId</code>
	 */
	public static ModConfig registerConfig(String modId, ModConfig.Type type, IConfigSpec<?> spec) {
		return new ModConfig(type, spec, FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> new IllegalArgumentException(String.format("No mod with mod id %s", modId))));
	}

	/**
	 * Register a new mod config, only difference from registering on Forge is <code>modId</code> has to be provided as there is no loading context to get that information from
	 *
	 * @param modId    mod id of your mod
	 * @param type     type of this mod config (client, common, or server)
	 * @param spec     the built config spec
	 * @param fileName file name to use instead of default
	 * @return the {@link ModConfig} instance
	 * @throws java.lang.IllegalArgumentException when no mod container is found for <code>modId</code>
	 */
	public static ModConfig registerConfig(String modId, ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
		return new ModConfig(type, spec, FabricLoader.getInstance().getModContainer(modId).orElseThrow(() -> new IllegalArgumentException(String.format("No mod with mod id %s", modId))), fileName);
	}
}
