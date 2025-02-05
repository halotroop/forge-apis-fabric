package net.minecraftforge.fml.loading;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static net.minecraftforge.ForgeConfigAPIPort.CORE;

@SuppressWarnings("unused")
public class FileUtils {
	private static final Logger LOGGER = LogManager.getLogger();

	public static Path getOrCreateDirectory(Path dirPath, String dirLabel) {
		if (!Files.isDirectory(dirPath.getParent())) {
			getOrCreateDirectory(dirPath.getParent(), "parent of " + dirLabel);
		}
		if (!Files.isDirectory(dirPath)) {
			LOGGER.debug(CORE, "Making {} directory : {}", dirLabel, dirPath);
			try {
				Files.createDirectory(dirPath);
			} catch (IOException e) {
				if (e instanceof FileAlreadyExistsException) {
					LOGGER.fatal(CORE, "Failed to create {} directory - there is a file in the way", dirLabel);
				} else {
					LOGGER.fatal(CORE, "Problem with creating {} directory (Permissions?)", dirLabel, e);
				}
				throw new RuntimeException("Problem creating directory", e);
			}
			LOGGER.debug(CORE, "Created {} directory : {}", dirLabel, dirPath);
		} else {
			LOGGER.debug(CORE, "Found existing {} directory : {}", dirLabel, dirPath);
		}
		return dirPath;
	}


	public static String fileExtension(final Path path) {
		String fileName = path.getFileName().toString();
		int idx = fileName.lastIndexOf('.');
		if (idx > -1) {
			return fileName.substring(idx + 1);
		} else {
			return "";
		}
	}
}
