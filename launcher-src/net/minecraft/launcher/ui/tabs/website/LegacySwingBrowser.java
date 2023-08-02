package net.minecraft.launcher.ui.tabs.website;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.net.URL;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.launcher.OperatingSystem;

public class LegacySwingBrowser implements Browser {

	private static final Logger LOGGER = LogManager.getLogger();

	private final JScrollPane scrollPane = new JScrollPane();

	private final JTextPane browser = new JTextPane();

	public LegacySwingBrowser() {

		this.browser.setEditable(false);
		this.browser.setMargin((Insets)null);
		this.browser.setBackground(Color.WHITE);
		this.browser.setContentType("text/html");
		this.browser.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Loading page..</h1></center></font></body></html>");

		this.browser.addHyperlinkListener(new HyperlinkListener() {

			public void hyperlinkUpdate(HyperlinkEvent he) {

				if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED)

					try {

						OperatingSystem.openLink(he.getURL().toURI());

					} catch (Exception e) {

						LegacySwingBrowser.LOGGER.error("Unexpected exception opening link " + he.getURL(), e);
					}  
			}
		});

		this.scrollPane.setViewportView(this.browser);
	}

	public void loadUrl(final String url) {

		Thread thread = new Thread("Update website tab") {

			public void run() {

				try {
					
					LegacySwingBrowser.this.browser.setPage(new URL(url));
					
				} catch (Exception e) {

					LegacySwingBrowser.LOGGER.error("Unexpected exception loading " + url, e);

					LegacySwingBrowser.this.browser.setText("<html><body><font color=\"#808080\"><br><br><br><br><br><br><br><center><h1>Failed to get page</h1><br>" + e.toString() + "</center></font></body></html>");
				} 
			}
		};

		thread.setDaemon(true);
		thread.start();
	}

	public Component getComponent() {

		return this.scrollPane;
	}

	public void resize(Dimension size) {}
}
