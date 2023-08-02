package net.minecraft.launcher.ui.tabs;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.IntrospectionException;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.JPanel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.website.Browser;
import net.minecraft.launcher.ui.tabs.website.JFXBrowser;
import net.minecraft.launcher.ui.tabs.website.LegacySwingBrowser;

public class WebsiteTab extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger();

	private final Browser browser = selectBrowser();

	private final Launcher minecraftLauncher;

	public WebsiteTab(Launcher minecraftLauncher) {

		this.minecraftLauncher = minecraftLauncher;

		setLayout(new BorderLayout());

		add(this.browser.getComponent(), "Center");

		this.browser.resize(getSize());

		addComponentListener(new ComponentAdapter() {

			public void componentResized(ComponentEvent e) {

				WebsiteTab.this.browser.resize(e.getComponent().getSize());
			}
		});
	}

	private Browser selectBrowser() {

		if (hasJFX()) {

			LOGGER.info("JFX is already initialized");

			return (Browser)new JFXBrowser();
		}

		File jfxrt = new File(System.getProperty("java.home") + "/lib/ext/jfxrt.jar");

		if (jfxrt.isFile()) {

			LOGGER.debug("Attempting to load {}...", new Object[] { jfxrt });

			try {

				addToSystemClassLoader(jfxrt);

				LOGGER.info("JFX has been detected & successfully loaded");

				return (Browser)new JFXBrowser();

			} catch (Throwable e) {

				LOGGER.debug("JFX has been detected but unsuccessfully loaded", e);

				return (Browser)new LegacySwingBrowser();
			} 
		}

		LOGGER.debug("JFX was not found at {}", new Object[] { jfxrt });

		return (Browser)new LegacySwingBrowser();
	}

	public void setPage(String url) {

		this.browser.loadUrl(url);
	}

	public Launcher getMinecraftLauncher() {

		return this.minecraftLauncher;
	}

	public static void addToSystemClassLoader(File file) throws IntrospectionException {

		if (ClassLoader.getSystemClassLoader() instanceof URLClassLoader) {

			URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();

			try {

				Method method = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
				method.setAccessible(true);
				method.invoke(classLoader, new Object[] { file.toURI().toURL() });

			} catch (Throwable t) {

				LOGGER.warn("Couldn't add " + file + " to system classloader", t);
			} 
		} 
	}

	public boolean hasJFX() {

		try {

			getClass().getClassLoader().loadClass("javafx.embed.swing.JFXPanel");
			
			return true;

		} catch (ClassNotFoundException e) {

			return false;
		} 
	}
}
