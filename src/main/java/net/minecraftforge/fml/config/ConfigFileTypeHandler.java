package net.minecraftforge.fml.config;

import com.electronwill.nightconfig.core.ConfigFormat;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileWatcher;
import com.electronwill.nightconfig.core.io.ParsingException;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlFormat;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraftforge.api.ConfigPaths;
import net.minecraftforge.api.fml.event.config.ModConfigEvent;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

public class ConfigFileTypeHandler {
	private static final Logger LOGGER = LogManager.getLogger();
	static ConfigFileTypeHandler TOML = new ConfigFileTypeHandler();
	private static final Path defaultConfigPath = FabricLoader.getInstance().getGameDir().resolve(ConfigPaths.DEFAULT_CONFIGS_PATH);

	public Function<ModConfig, CommentedFileConfig> reader(Path configBasePath) {
		return (c) -> {
			final Path configPath = configBasePath.resolve(c.getFileName());
			// force toml format which is normally auto-detected by night config from the file extension
			// there have been reports of this failing and the auto-detection not working
			// my guess is this is caused by the toml format not having been registered as the registration is done in a static initializer in the class itself
			// due to the clazz not having been initialized by the classloader (see java language specification 12.2)
			// forcing the format or calling any static method on the class will guarantee it to be initialized
			final CommentedFileConfig configData = CommentedFileConfig.builder(configPath, TomlFormat.instance()).sync().
					preserveInsertionOrder().
					autosave().
					onFileNotFound((newfile, configFormat) -> setupConfigFile(c, newfile, configFormat)).
					writingMode(WritingMode.REPLACE).
					build();
			LOGGER.debug(ConfigTracker.CONFIG, "Built TOML config for {}", configPath.toString());
			try {
				configData.load();
			} catch (ParsingException ex) {
				throw new ConfigLoadingException(c, ex);
			}
			LOGGER.debug(ConfigTracker.CONFIG, "Loaded TOML config file {}", configPath.toString());
			try {
				FileWatcher.defaultInstance().addWatch(configPath, new ConfigWatcher(c, configData, Thread.currentThread().getContextClassLoader()));
				LOGGER.debug(ConfigTracker.CONFIG, "Watching TOML config file {} for changes", configPath.toString());
			} catch (IOException e) {
				throw new RuntimeException("Couldn't watch config file", e);
			}
			return configData;
		};
	}

	public void unload(Path configBasePath, ModConfig config) {
		Path configPath = configBasePath.resolve(config.getFileName());
		try {
			FileWatcher.defaultInstance().removeWatch(configBasePath.resolve(config.getFileName()));
		} catch (RuntimeException e) {
			LOGGER.error("Failed to remove config {} from tracker!", configPath.toString(), e);
		}
	}

	private boolean setupConfigFile(final ModConfig modConfig, final Path file, final ConfigFormat<?> conf) throws IOException {
		Files.createDirectories(file.getParent());
		Path p = defaultConfigPath.resolve(modConfig.getFileName());
		if (Files.exists(p)) {
			LOGGER.info(ConfigTracker.CONFIG, "Loading default config file from path {}", p);
			Files.copy(p, file);
		} else {
			Files.createFile(file);
			conf.initEmptyFile(file);
		}
		return true;
	}

	public static void backUpConfig(final CommentedFileConfig commentedFileConfig) {
		backUpConfig(commentedFileConfig, 5);
	}

	public static void backUpConfig(final CommentedFileConfig commentedFileConfig, final int maxBackups) {
		Path bakFileLocation = commentedFileConfig.getNioPath().getParent();
		String bakFileName = FilenameUtils.removeExtension(commentedFileConfig.getFile().getName());
		String bakFileExtension = FilenameUtils.getExtension(commentedFileConfig.getFile().getName()) + ".bak";
		Path bakFile = bakFileLocation.resolve(bakFileName + "-1" + "." + bakFileExtension);
		try {
			for (int i = maxBackups; i > 0; i--) {
				Path oldBak = bakFileLocation.resolve(bakFileName + "-" + i + "." + bakFileExtension);
				if (Files.exists(oldBak)) {
					if (i >= maxBackups)
						Files.delete(oldBak);
					else
						Files.move(oldBak, bakFileLocation.resolve(bakFileName + "-" + (i + 1) + "." + bakFileExtension));
				}
			}
			Files.copy(commentedFileConfig.getNioPath(), bakFile);
		} catch (IOException exception) {
			LOGGER.warn(ConfigTracker.CONFIG, "Failed to back up config file {}", commentedFileConfig.getNioPath(), exception);
		}
	}

	private static class ConfigWatcher implements Runnable {
		private final ModConfig modConfig;
		private final CommentedFileConfig commentedFileConfig;
		private final ClassLoader realClassLoader;

		ConfigWatcher(final ModConfig modConfig, final CommentedFileConfig commentedFileConfig, final ClassLoader classLoader) {
			this.modConfig = modConfig;
			this.commentedFileConfig = commentedFileConfig;
			this.realClassLoader = classLoader;
		}

		@Override
		public void run() {
			// Force the regular classloader onto the special thread
			Thread.currentThread().setContextClassLoader(realClassLoader);
			if (!this.modConfig.getSpec().isCorrecting()) {
				try {
					this.commentedFileConfig.load();
					if (!this.modConfig.getSpec().isCorrect(commentedFileConfig)) {
						LOGGER.warn(ConfigTracker.CONFIG, "Configuration file {} is not correct. Correcting", commentedFileConfig.getFile().getAbsolutePath());
						ConfigFileTypeHandler.backUpConfig(commentedFileConfig);
						this.modConfig.getSpec().correct(commentedFileConfig);
						commentedFileConfig.save();
					}
				} catch (ParsingException ex) {
					throw new ConfigLoadingException(modConfig, ex);
				}
				LOGGER.debug(ConfigTracker.CONFIG, "Config file {} changed, sending notifies", this.modConfig.getFileName());
				this.modConfig.getSpec().afterReload();
				ModConfigEvent.RELOADING.invoker().onModConfigReloading(this.modConfig);
			}
		}
	}

	private static class ConfigLoadingException extends RuntimeException {
		public ConfigLoadingException(ModConfig config, Exception cause) {
			super("Failed loading config file " + config.getFileName() + " of type " + config.getType() + " for modid " + config.getModId(), cause);
		}
	}
}
