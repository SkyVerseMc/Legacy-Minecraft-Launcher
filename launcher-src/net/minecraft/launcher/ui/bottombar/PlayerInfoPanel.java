package net.minecraft.launcher.ui.bottombar;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;

import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.profile.Profile;
import net.minecraft.launcher.profile.ProfileManager;
import net.minecraft.launcher.profile.RefreshedProfilesListener;
import net.minecraft.launcher.profile.UserChangedListener;
import net.minecraft.launcher.ui.popups.profile.ProfileVersionPanel.DateComparator;

public class PlayerInfoPanel extends JPanel implements RefreshedVersionsListener, RefreshedProfilesListener, UserChangedListener {

	private static final long serialVersionUID = 1L;

	private final Launcher minecraftLauncher;

	private final JLabel welcomeText = new JLabel("", 0);

	private final JLabel versionText = new JLabel("", 0);

	private final JButton switchUserButton = new JButton("Switch User");

	public PlayerInfoPanel(final Launcher minecraftLauncher) {

		this.minecraftLauncher = minecraftLauncher;

		minecraftLauncher.getProfileManager().addRefreshedProfilesListener(this);
		minecraftLauncher.getProfileManager().addUserChangedListener(this);

		checkState();
		createInterface();

		this.switchUserButton.setEnabled(false);

		this.switchUserButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {

				minecraftLauncher.getUserInterface().showLoginPrompt();
			}
		});
	}

	protected void createInterface() {


		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = 2;
		constraints.gridy = 0;
		constraints.weightx = 1.0D;
		constraints.gridwidth = 2;

		add(this.welcomeText, constraints);

		constraints.gridwidth = 1;
		constraints.weightx = 0.0D;
		constraints.gridy++;
		constraints.weightx = 1.0D;
		constraints.gridwidth = 2;

		add(this.versionText, constraints);

		constraints.gridwidth = 1;
		constraints.weightx = 0.0D;
		constraints.gridy++;
		constraints.weightx = 0.5D;
		constraints.fill = 0;

		add(this.switchUserButton, constraints);

		constraints.weightx = 0.0D;
		constraints.gridy++;
	}

	public void onProfilesRefreshed(ProfileManager manager) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				PlayerInfoPanel.this.checkState();
			}
		});
	}

	public void checkState() {

		ProfileManager profileManager = this.minecraftLauncher.getProfileManager();

		UserAuthentication auth = (profileManager.getSelectedUser() == null) ? null : profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());

		if (auth == null || !auth.isLoggedIn()) {

			this.welcomeText.setText("Welcome, guest! Please log in.");

		} else if (auth.getSelectedProfile() == null) {

			this.welcomeText.setText("<html>Welcome, player!</html>");

		} else {

			this.welcomeText.setText("<html>Welcome, <b>" + auth.getSelectedProfile().getName() + "</b></html>");
		}

		Profile profile = profileManager.getProfiles().isEmpty() ? null : profileManager.getSelectedProfile();

		List<VersionSyncInfo> versions = (profile == null) ? null : this.minecraftLauncher.getLauncher().getVersionManager().getVersions(profile.getVersionFilter());

		VersionSyncInfo version = (profile == null || versions.isEmpty()) ? null : versions.get(0);

		if (profile != null && profile.getLastVersionId() != null) {

			VersionSyncInfo requestedVersion = this.minecraftLauncher.getLauncher().getVersionManager().getVersionSyncInfo(profile.getLastVersionId());

			if (requestedVersion == null || requestedVersion.getLatestVersion() == null) {

				Collections.sort(versions, new DateComparator());
				version = versions.get(0);
				for (int i = 0; i < versions.size(); i++) {

					VersionSyncInfo v = versions.get(i);

					if (v.getRemoteVersion() != null) {

						if (profile.getLastVersionId().startsWith("Latest ")) {

							if (v.getRemoteVersion().getType().getName().equals(profile.getLastVersionId().split(" ")[1].toLowerCase())) {

								version = v;
								break;
							}
						}
					}
				}
			}
			if (requestedVersion != null && requestedVersion.getLatestVersion() != null) version = requestedVersion; 
		}

		if (version == null) {

			this.versionText.setText("Loading versions...");

		} else if (version.isUpToDate()) {

			this.versionText.setText("Ready to play Minecraft " + version.getLatestVersion().getId());

		} else if (version.isInstalled()) {

			this.versionText.setText("Ready to update & play Minecraft " + version.getLatestVersion().getId());

		} else if (version.isOnRemote()) {

			this.versionText.setText("Ready to download & play Minecraft " + version.getLatestVersion().getId());
		}

		this.switchUserButton.setEnabled(true);
	}

	public void onVersionsRefreshed(VersionManager manager) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				PlayerInfoPanel.this.checkState();
			}
		});
	}

	public Launcher getMinecraftLauncher() {

		return this.minecraftLauncher;
	}

	public void onUserChanged(ProfileManager manager) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				PlayerInfoPanel.this.checkState();
			}
		});
	}
}
