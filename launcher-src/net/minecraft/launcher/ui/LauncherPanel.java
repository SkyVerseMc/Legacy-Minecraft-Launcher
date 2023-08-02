package net.minecraft.launcher.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.ui.tabs.LauncherTabPanel;

public class LauncherPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	
	public static final String MCUPDATE_URL = "https://mcupdate.tumblr.com";//"https://raw.githubusercontent.com/SkyVerseMc/Legacy-Minecraft-Launcher/main/mcupdate.html";

	public static final String CARD_DIRT_BACKGROUND = "loading";

	public static final String CARD_LOGIN = "login";

	public static final String CARD_LAUNCHER = "launcher";

	private final CardLayout cardLayout;

	private final LauncherTabPanel tabPanel;

	private final BottomBarPanel bottomBar;

	private final JProgressBar progressBar;

	private final Launcher minecraftLauncher;

	private final JPanel loginPanel;

//	private JLabel warningLabel;

	public LauncherPanel(Launcher minecraftLauncher) {

		this.minecraftLauncher = minecraftLauncher;
		this.cardLayout = new CardLayout();

		setLayout(this.cardLayout);

		this.progressBar = new JProgressBar();
		this.bottomBar = new BottomBarPanel(minecraftLauncher);
		this.tabPanel = new LauncherTabPanel(minecraftLauncher);
		this.loginPanel = new TexturedPanel("/dirt.png");

		createInterface();
	}

	protected void createInterface() {

		add(createLauncherInterface(), "launcher");
		add(createDirtInterface(), "loading");
		add(createLoginInterface(), "login");
	}

	protected JPanel createLauncherInterface() {

		JPanel result = new JPanel(new BorderLayout());

		this.tabPanel.getBlog().setPage(MCUPDATE_URL);

		/*boolean javaBootstrap = (getMinecraftLauncher().getBootstrapVersion().intValue() < 100);
		boolean upgradableOS = (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS);

		if (OperatingSystem.getCurrentPlatform() == OperatingSystem.OSX) {

			String ver = SystemUtils.OS_VERSION;

			if (ver != null && !ver.isEmpty()) {

				String[] split = ver.split("\\.", 3);

				if (split.length >= 2) {

					try {

						int major = Integer.parseInt(split[0]);
						int minor = Integer.parseInt(split[1]);

						if (major == 10) {

							upgradableOS = (minor >= 8);

						} else if (major > 10) {

							upgradableOS = true;
						} 
					} catch (NumberFormatException numberFormatException) {} 
				}
			}
		}

		if (oldversion) {

			final URI url;

			this.warningLabel = new JLabel();
			this.warningLabel.setForeground(Color.RED);
			this.warningLabel.setHorizontalAlignment(0);

			if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {

				url = LauncherConstants.URL_UPGRADE_WINDOWS;

			} else {

				url = LauncherConstants.URL_UPGRADE_OSX;
			}
			
			if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {

				this.warningLabel.setText("<html><p style='font-size: 1.1em'>You are running an old version of the launcher. <a href='\" + url + \"'>Update</a></p></html>");

			} else {

				this.warningLabel.setText("<html><p style='font-size: 1em'>You are running an old version of the launcher. <a href='\" + url + \"'>Update</a></p></html>");
			} 

			result.add(this.warningLabel, "North");

			result.addMouseListener(new MouseAdapter() {

				public void mouseClicked(MouseEvent e) {

					OperatingSystem.openLink(url);
				}
			});
		}*/

		JPanel center = new JPanel();
		center.setLayout(new BorderLayout());
		center.add((Component)this.tabPanel, "Center");
		center.add(this.progressBar, "South");

		this.progressBar.setVisible(false);
		this.progressBar.setMinimum(0);
		this.progressBar.setMaximum(100);
		this.progressBar.setStringPainted(true);

		result.add(center, "Center");
		result.add(this.bottomBar, "South");

		return result;
	}

	protected JPanel createDirtInterface() {

		return new TexturedPanel("/dirt.png");
	}

	protected JPanel createLoginInterface() {

		this.loginPanel.setLayout(new GridBagLayout());

		return this.loginPanel;
	}

	public LauncherTabPanel getTabPanel() {

		return this.tabPanel;
	}

	public BottomBarPanel getBottomBar() {

		return this.bottomBar;
	}

	public JProgressBar getProgressBar() {

		return this.progressBar;
	}

	public Launcher getMinecraftLauncher() {

		return this.minecraftLauncher;
	}

	public void setCard(String card, JPanel additional) {

		if (card.equals("login")) {

			this.loginPanel.removeAll();
			this.loginPanel.add(additional);
		}

		this.cardLayout.show(this, card);
	}
}
