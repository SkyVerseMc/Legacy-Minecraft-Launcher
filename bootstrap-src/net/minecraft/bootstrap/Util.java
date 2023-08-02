package net.minecraft.bootstrap;

import java.io.File;

public class Util {
	
	public static final String APPLICATION_NAME = "minecraft";

	public enum OS {
		
		WINDOWS, MACOS, SOLARIS, LINUX, UNKNOWN;
	}

	public static OS getPlatform() {
		
		String osName = System.getProperty("os.name").toLowerCase();
		
		if (osName.contains("win")) return OS.WINDOWS; 
		if (osName.contains("mac")) return OS.MACOS;
		if (osName.contains("linux")) return OS.LINUX; 
		if (osName.contains("unix")) return OS.LINUX; 
		
		return OS.UNKNOWN;
	}
	
	public static File getWorkingDirectory() {

		String applicationData, folder, userHome = System.getProperty("user.home", ".");
		File workingDirectory = new File(userHome, "minecraft/");

		switch (getPlatform()) {

		case LINUX:
			workingDirectory = new File(userHome, ".minecraft/");
			return workingDirectory;

		case WINDOWS:
			applicationData = System.getenv("APPDATA");
			folder = (applicationData != null) ? applicationData : userHome;

			workingDirectory = new File(folder, ".minecraft/");

			return workingDirectory;

		case MACOS:
			workingDirectory = new File(userHome, "Library/Application Support/minecraft");
			return workingDirectory;

		default:
			break;
		}

		return workingDirectory;
	}
}
