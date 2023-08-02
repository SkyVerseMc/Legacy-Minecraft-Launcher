package net.minecraft.launcher;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.launcher.OperatingSystem;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class Main {

	private static final Logger LOGGER = LogManager.getLogger();

	public static void main(String[] args) {

		LOGGER.debug("main() called!");
		startLauncher(args);
	}

	private static void startLauncher(String[] args) {

		OptionParser parser = new OptionParser();
		parser.allowsUnrecognizedOptions();
		parser.accepts("winTen");

		ArgumentAcceptingOptionSpec<String> argumentAcceptingOptionSpec1 = parser.accepts("proxyHost").withRequiredArg();
		ArgumentAcceptingOptionSpec<Integer> argumentAcceptingOptionSpec2 = parser.accepts("proxyPort").withRequiredArg().defaultsTo("8080", new String[0]).ofType(Integer.class);
		ArgumentAcceptingOptionSpec<File> argumentAcceptingOptionSpec3 = parser.accepts("workDir").withRequiredArg().ofType(File.class).defaultsTo(getWorkingDirectory(), new File[0]);

		NonOptionArgumentSpec<String> nonOptionArgumentSpec = parser.nonOptions();

		OptionSet optionSet = parser.parse(args);

		List<String> leftoverArgs = optionSet.valuesOf((OptionSpec<String>)nonOptionArgumentSpec);

		String hostName = optionSet.valueOf((OptionSpec<String>)argumentAcceptingOptionSpec1);

		Proxy proxy = Proxy.NO_PROXY;

		if (hostName != null)

			try {

				proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, optionSet.valueOf((OptionSpec<Integer>)argumentAcceptingOptionSpec2).intValue()));

			} catch (Exception exception) {} 

		File workingDirectory = optionSet.valueOf((OptionSpec<File>)argumentAcceptingOptionSpec3);
		workingDirectory.mkdirs();

		LOGGER.debug("About to create JFrame.");

		Proxy finalProxy = proxy;

		JFrame frame = new JFrame();
		frame.setTitle("Minecraft Launcher " + LauncherConstants.getVersionName() + LauncherConstants.PROPERTIES.getEnvironment().getTitle());
		frame.setPreferredSize(new Dimension(900, 580));

		try {

			InputStream in = Launcher.class.getResourceAsStream("/favicon.png");

			if (in != null) frame.setIconImage(ImageIO.read(in)); 

		} catch (IOException iOException) {}

		frame.pack();
		frame.setLocationRelativeTo((Component)null);
		frame.setVisible(true);

		if (optionSet.has("winTen")) {

			System.setProperty("os.name", "Windows 10");
			System.setProperty("os.version", "10.0");
		}

		LOGGER.debug("Starting up launcher.");

		Launcher launcher = new Launcher(frame, workingDirectory, finalProxy, null, leftoverArgs.<String>toArray(new String[leftoverArgs.size()]), Integer.valueOf(100));

		if (optionSet.has("winTen")) launcher.setWinTenHack(); 

		frame.setLocationRelativeTo((Component)null);

		LOGGER.debug("End of main.");
	}

	public static File getWorkingDirectory() {

		String applicationData, folder, userHome = System.getProperty("user.home", ".");
		File workingDirectory = new File(userHome, "minecraft/");

		switch (OperatingSystem.getCurrentPlatform()) {

		case LINUX:
			workingDirectory = new File(userHome, ".minecraft/");
			return workingDirectory;

		case WINDOWS:
			applicationData = System.getenv("APPDATA");
			folder = (applicationData != null) ? applicationData : userHome;

			workingDirectory = new File(folder, ".minecraft/");

			return workingDirectory;

		case OSX:
			workingDirectory = new File(userHome, "Library/Application Support/minecraft");
			return workingDirectory;

		default:
			break;
		}

		return workingDirectory;
	}
}
