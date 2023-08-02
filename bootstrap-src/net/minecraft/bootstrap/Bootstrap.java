package net.minecraft.bootstrap;

import java.awt.Component;
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.math.BigInteger;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

//import java.util.jar.Pack200;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.Document;

import LZMA.LzmaInputStream;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.NonOptionArgumentSpec;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.hopper.HopperService;

public class Bootstrap extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final Font MONOSPACED = new Font("Monospaced", 0, 12);
	
	public static final int BOOTSTRAP_VERSION_NUMBER = 6;

	public static final String LAUNCHER_URL = "https://api.github.com/repos/SkyVerseMc/Legacy-Minecraft-Launcher/releases/latest";

	public final File workDir;

	private final Proxy proxy;

	private final File launcherJar;

	private final File packedLauncherJar;

	private final File packedLauncherJarNew;

	private final JTextArea textArea;

	private final JScrollPane scrollPane;

	private final PasswordAuthentication proxyAuth;

	private final String[] remainderArgs;

	private final StringBuilder outputBuffer = new StringBuilder();

	public Bootstrap(File workDir, Proxy proxy, PasswordAuthentication proxyAuth, String[] remainderArgs) {

		super("Minecraft Launcher");

		this.workDir = workDir;
		this.proxy = proxy;
		this.proxyAuth = proxyAuth;
		this.remainderArgs = remainderArgs;
		this.launcherJar = new File(workDir, "launcher.jar");
		this.packedLauncherJar = new File(workDir, "launcher.jar.lzma");
		this.packedLauncherJarNew = new File(workDir, "launcher.jar.lzma.new");

		setSize(854, 480);
		setDefaultCloseOperation(3);

		this.textArea = new JTextArea();
		this.textArea.setLineWrap(true);
		this.textArea.setEditable(false);
		this.textArea.setFont(MONOSPACED);

		((DefaultCaret)this.textArea.getCaret()).setUpdatePolicy(1);

		this.scrollPane = new JScrollPane(this.textArea);
		this.scrollPane.setBorder((Border)null);
		this.scrollPane.setVerticalScrollBarPolicy(22);

		add(this.scrollPane);
		setLocationRelativeTo((Component)null);
		setVisible(true);

		println("Bootstrap (v" + BOOTSTRAP_VERSION_NUMBER + ")");
		println("Current time is " + DateFormat.getDateTimeInstance(2, 2, Locale.US).format(new Date()));
		println("System.getProperty('os.name') == '" + System.getProperty("os.name") + "'");
		println("System.getProperty('os.version') == '" + System.getProperty("os.version") + "'");
		println("System.getProperty('os.arch') == '" + System.getProperty("os.arch") + "'");
		println("System.getProperty('java.version') == '" + System.getProperty("java.version") + "'");
		println("System.getProperty('java.vendor') == '" + System.getProperty("java.vendor") + "'");
		println("System.getProperty('sun.arch.data.model') == '" + System.getProperty("sun.arch.data.model") + "'");
		println("");
	}

	public void execute(boolean force) {

		if (this.packedLauncherJarNew.isFile()) {

			println("Found cached update");
			renameNew();
		}

		Downloader.Controller controller = new Downloader.Controller();

		Object[] update = LauncherUpdateUtil.checkForUpdates(this);

		if ((boolean)update[0]) {

			Thread thread = new Thread(new Downloader((String)update[2], controller, this, this.proxy, (String)update[3], this.packedLauncherJarNew));

			thread.setName("Launcher downloader");
			thread.start();

			try {

				println("Looking for update");

				boolean wasInTime = controller.foundUpdateLatch.await(3L, TimeUnit.SECONDS);

				if (controller.foundUpdate.get()) {

					println("Found update in time, waiting to download");
					controller.hasDownloadedLatch.await();
					renameNew();

				} else if (!wasInTime) {

					println("Didn't find an update in time.");
				}

			} catch (InterruptedException e) {

				throw new FatalBootstrapError("Got interrupted: " + e.toString());
			}
		}
		if (this.packedLauncherJar.exists()) {

			if (!this.launcherJar.exists()) {

				unpack();

			} else {

				this.packedLauncherJar.delete();
			}
		}
		startLauncher(this.launcherJar);
	}

	public void unpack() {

		LzmaInputStream lzmaInputStream = null;

		File lzmaUnpacked = getUnpackedLzmaFile(this.packedLauncherJar);

		OutputStream outputHandle = null;

		println("Reversing LZMA on " + this.packedLauncherJar + " to " + lzmaUnpacked);

		try {

			lzmaInputStream = new LzmaInputStream(new FileInputStream(this.packedLauncherJar));
			outputHandle = new FileOutputStream(lzmaUnpacked);

			byte[] buffer = new byte[65536];
			int read = lzmaInputStream.read(buffer);

			while (read >= 1) {


				outputHandle.write(buffer, 0, read);
				read = lzmaInputStream.read(buffer);
			} 
		} catch (Exception e) {

			e.printStackTrace();

		} finally {

			if (lzmaInputStream != null) closeSilently((Closeable)lzmaInputStream);

			closeSilently(outputHandle);
		}
	}

	public static void closeSilently(Closeable closeable) {

		if (closeable != null)
			try {

				closeable.close();

			} catch (IOException ignored) {} 
	}

	private File getUnpackedLzmaFile(File packedLauncherJar) {

		String filePath = packedLauncherJar.getAbsolutePath();

		if (filePath.endsWith(".lzma")) filePath = filePath.substring(0, filePath.length() - 5);

		return new File(filePath);
	}

	public String getMd5(File file) {

		DigestInputStream stream = null;

		try {

			stream = new DigestInputStream(new FileInputStream(file), MessageDigest.getInstance("MD5"));

			byte[] buffer = new byte[65536];

			int read = stream.read(buffer);

			while (read >= 1) read = stream.read(buffer); 

		} catch (Exception ignored) {

			return null;

		} finally {

			closeSilently(stream);
		}

		return String.format("%1$032x", new Object[] { new BigInteger(1, stream.getMessageDigest().digest()) });
	}

	public void println(String string) {

		print(string + "\n");
	}

	public void print(String string) {

		System.out.print(string);

		this.outputBuffer.append(string);

		Document document = this.textArea.getDocument();

		final JScrollBar scrollBar = this.scrollPane.getVerticalScrollBar();

		boolean shouldScroll = (scrollBar.getValue() + scrollBar.getSize().getHeight() + (MONOSPACED.getSize() * 2) > scrollBar.getMaximum());

		try {

			document.insertString(document.getLength(), string, null);
		} catch (BadLocationException ignored) {}

		if (shouldScroll) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					scrollBar.setValue(2147483647);
				}
			});
		}
	}

	public void startLauncher(File launcherJar) {

		println("Starting launcher.");

		try {

			Class<?> aClass = (new URLClassLoader(new URL[] { launcherJar.toURI().toURL() })).loadClass("net.minecraft.launcher.Launcher");

			Constructor<?> constructor = aClass.getConstructor(new Class[] { JFrame.class, File.class, Proxy.class, PasswordAuthentication.class, String[].class, Integer.class });
			constructor.newInstance(new Object[] { this, this.workDir, this.proxy, this.proxyAuth, this.remainderArgs, BOOTSTRAP_VERSION_NUMBER });

		} catch (Exception e) {

			throw new FatalBootstrapError("Unable to start: " + e);
		} 
	}

	public void renameNew() {

		if (this.packedLauncherJar.exists() && !this.packedLauncherJar.isFile() && !this.packedLauncherJar.delete()) throw new FatalBootstrapError("while renaming, target path: " + this.packedLauncherJar.getAbsolutePath() + " is not a file and we failed to delete it"); 

		if (this.packedLauncherJarNew.isFile()) {

			println("Renaming " + this.packedLauncherJarNew.getAbsolutePath() + " to " + this.packedLauncherJar.getAbsolutePath());

			if (this.packedLauncherJarNew.renameTo(this.packedLauncherJar)) {

				println("Renamed successfully.");

			} else {

				if (this.packedLauncherJar.exists() && !this.packedLauncherJar.canWrite()) throw new FatalBootstrapError("unable to rename: target" + this.packedLauncherJar.getAbsolutePath() + " not writable"); 

				println("Unable to rename - could be on another filesystem, trying copy & delete.");

				if (this.packedLauncherJarNew.exists() && this.packedLauncherJarNew.isFile()) {

					try {

						copyFile(this.packedLauncherJarNew, this.packedLauncherJar);

						if (this.packedLauncherJarNew.delete()) {

							println("Copy & delete succeeded.");

						} else {

							println("Unable to remove " + this.packedLauncherJarNew.getAbsolutePath() + " after copy.");
						} 

					} catch (IOException e) {

						throw new FatalBootstrapError("unable to copy:" + e);
					} 

				} else {

					println("Nevermind... file vanished?");
				} 
			} 
		} 
	}

	public static void copyFile(File source, File target) throws IOException {

		if (!target.exists()) target.createNewFile(); 

		FileChannel sourceChannel = null;
		FileChannel targetChannel = null;

		try {

			sourceChannel = (new FileInputStream(source)).getChannel();

			targetChannel = (new FileOutputStream(target)).getChannel();
			targetChannel.transferFrom(sourceChannel, 0L, sourceChannel.size());

		} finally {

			if (sourceChannel != null) sourceChannel.close(); 
			if (targetChannel != null) targetChannel.close(); 
		} 
	}

	public static void main(String[] args) throws IOException {

		OptionSet optionSet;
		System.setProperty("java.net.preferIPv4Stack", "true");
		OptionParser optionParser = new OptionParser();
		optionParser.allowsUnrecognizedOptions();
		optionParser.accepts("help", "Show help").forHelp();
		optionParser.accepts("force", "Force updating");
		ArgumentAcceptingOptionSpec<String> argumentAcceptingOptionSpec1 = optionParser.accepts("proxyHost", "Optional").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> argumentAcceptingOptionSpec2 = optionParser.accepts("proxyPort", "Optional").withRequiredArg().defaultsTo("8080");
		ArgumentAcceptingOptionSpec<String> argumentAcceptingOptionSpec3 = optionParser.accepts("proxyUser", "Optional").withRequiredArg();
		ArgumentAcceptingOptionSpec<String> argumentAcceptingOptionSpec4 = optionParser.accepts("proxyPass", "Optional").withRequiredArg();
		ArgumentAcceptingOptionSpec argumentAcceptingOptionSpec5 = optionParser.accepts("workDir", "Optional").withRequiredArg().ofType(File.class).defaultsTo(Util.getWorkingDirectory());

		NonOptionArgumentSpec nonOptionArgumentSpec = optionParser.nonOptions();

		try {

			optionSet = optionParser.parse(args);

		} catch (OptionException e) {

			optionParser.printHelpOn(System.out);

			System.out.println("(to pass in arguments to minecraft directly use: '--' followed by your arguments");
			return;
		} 

		if (optionSet.has("help")) {

			optionParser.printHelpOn(System.out);
			return;
		} 

		String hostName = (String)optionSet.valueOf((OptionSpec)argumentAcceptingOptionSpec1);

		Proxy proxy = Proxy.NO_PROXY;

		if (hostName != null)

			try {

				proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(hostName, ((Integer)optionSet.valueOf((OptionSpec)argumentAcceptingOptionSpec2)).intValue()));

			} catch (Exception ignored) {} 

		String proxyUser = (String)optionSet.valueOf((OptionSpec)argumentAcceptingOptionSpec3);
		String proxyPass = (String)optionSet.valueOf((OptionSpec)argumentAcceptingOptionSpec4);

		PasswordAuthentication passwordAuthentication = null;

		if (!proxy.equals(Proxy.NO_PROXY) && stringHasValue(proxyUser) && stringHasValue(proxyPass)) {

			passwordAuthentication = new PasswordAuthentication(proxyUser, proxyPass.toCharArray());

			final PasswordAuthentication auth = passwordAuthentication;

			Authenticator.setDefault(new Authenticator() {

				protected PasswordAuthentication getPasswordAuthentication() {

					return auth;
				}
			});
		} 

		File workingDirectory = (File)optionSet.valueOf((OptionSpec)argumentAcceptingOptionSpec5);

		if (workingDirectory.exists() && !workingDirectory.isDirectory()) throw new FatalBootstrapError("Invalid working directory: " + workingDirectory); 
		if (!workingDirectory.exists() && !workingDirectory.mkdirs()) throw new FatalBootstrapError("Unable to create directory: " + workingDirectory); 

		List<String> strings = optionSet.valuesOf((OptionSpec)nonOptionArgumentSpec);

		String[] remainderArgs = strings.<String>toArray(new String[strings.size()]);

		boolean force = optionSet.has("force");

		Bootstrap frame = new Bootstrap(workingDirectory, proxy, passwordAuthentication, remainderArgs);

		try {

			frame.execute(force);

		} catch (Throwable t) {

			ByteArrayOutputStream stracktrace = new ByteArrayOutputStream();

			t.printStackTrace(new PrintStream(stracktrace));

			StringBuilder report = new StringBuilder();
			report.append(stracktrace).append("\n\n-- Head --\nStacktrace:\n").append(stracktrace).append("\n\n").append(frame.outputBuffer);
			report.append("\tMinecraft.Bootstrap Version: " + BOOTSTRAP_VERSION_NUMBER);

			try {

				HopperService.submitReport(proxy, report.toString(), "Minecraft.Bootstrap", BOOTSTRAP_VERSION_NUMBER + "");

			} catch (Throwable ignored) {}

			frame.println("FATAL ERROR: " + stracktrace.toString());
			frame.println("\nPlease fix the error and restart.");
		} 
	}

	public static boolean stringHasValue(String string) {

		return (string != null && !string.isEmpty());
	}
}
