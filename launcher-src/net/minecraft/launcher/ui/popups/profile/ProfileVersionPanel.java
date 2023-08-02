package net.minecraft.launcher.ui.popups.profile;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.google.common.collect.Sets;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.versions.Version;

import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.profile.Profile;

public class ProfileVersionPanel extends JPanel implements RefreshedVersionsListener {

	private static final long serialVersionUID = 1L;

	private final ProfileEditorPopup editor;

	private final JComboBox versionList = new JComboBox();

	private final List<ReleaseTypeCheckBox> customVersionTypes = new ArrayList<ReleaseTypeCheckBox>();
	
	private final JCheckBox installed = new JCheckBox("Already installed versions");

	public ProfileVersionPanel(ProfileEditorPopup editor) {

		this.editor = editor;

		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createTitledBorder("Version Selection"));

		createInterface();
		addEventHandlers();

		List<VersionSyncInfo> versions = editor.getMinecraftLauncher().getLauncher().getVersionManager().getVersions(editor.getProfile().getVersionFilter());

		Collections.sort(versions, new DateComparator());

		if (versions.isEmpty()) {

			editor.getMinecraftLauncher().getLauncher().getVersionManager().addRefreshedVersionsListener(this);

		} else {

			populateVersions(versions);
		} 
	}

	protected void createInterface() {

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(2, 2, 2, 2);
		constraints.anchor = 17;
		constraints.gridy = 0;

		add(this.installed);
		this.installed.setEnabled(false);
		
		constraints.gridwidth = 1;
		constraints.weightx = 0.0D;
		constraints.fill = 0;
		constraints.gridy++;
		
		for (MinecraftReleaseType type : MinecraftReleaseType.values()) {

			ReleaseTypeCheckBox checkbox = new ReleaseTypeCheckBox(type);
			checkbox.setSelected(this.editor.getProfile().getVersionFilter().getTypes().contains(type));

			this.customVersionTypes.add(checkbox);

			constraints.fill = 2;
			constraints.weightx = 1.0D;
			constraints.gridwidth = 0;

			add(checkbox, constraints);

			constraints.gridwidth = 1;
			constraints.weightx = 0.0D;
			constraints.fill = 0;
			constraints.gridy++;
		}

		add(new JLabel("Use version:"), constraints);

		constraints.fill = 2;
		constraints.weightx = 1.0D;

		add(this.versionList, constraints);

		constraints.weightx = 0.0D;
		constraints.fill = 0;
		constraints.gridy++;

		this.versionList.setRenderer(new VersionListRenderer());
	}

	protected void addEventHandlers() {

		this.versionList.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {

				ProfileVersionPanel.this.updateVersionSelection();
			}
		});

		for (ReleaseTypeCheckBox type : this.customVersionTypes) {

			type.addItemListener(new ItemListener() {

				private boolean isUpdating = false;

				public void itemStateChanged(ItemEvent e) {

					if (this.isUpdating) return;

					if (e.getStateChange() == 1 && type.getType().getPopupWarning() != null) {

						int result = JOptionPane.showConfirmDialog(((SwingUserInterface)ProfileVersionPanel.this.editor.getMinecraftLauncher().getUserInterface()).getFrame(), type.getType().getPopupWarning() + "\n\nAre you sure you want to continue?");

						this.isUpdating = true;

						if (result == 0) {

							type.setSelected(true);

							ProfileVersionPanel.this.updateCustomVersionFilter();

						} else {

							type.setSelected(false);
						}

						this.isUpdating = false;

					} else {

						ProfileVersionPanel.this.updateCustomVersionFilter();
					}
				}
			});
		} 
	}

	private void updateCustomVersionFilter() {

		Profile profile = this.editor.getProfile();

		Set<MinecraftReleaseType> newTypes = Sets.newHashSet(Profile.DEFAULT_RELEASE_TYPES);

		boolean any = true;
		for (ReleaseTypeCheckBox type : this.customVersionTypes) {

			if (type.isSelected()) {

				any = false;
				newTypes.add(type.getType());
				continue;
			}

			newTypes.remove(type.getType());
		}
		
		this.installed.setSelected(any);

		if (any && !this.installed.isSelected()) {

			this.versionList.removeAllItems();
			return;
		}

		if (newTypes.equals(Profile.DEFAULT_RELEASE_TYPES)) {

			profile.setAllowedReleaseTypes(null);

		} else {

			profile.setAllowedReleaseTypes(newTypes);
		}

		populateVersions(this.editor.getMinecraftLauncher().getLauncher().getVersionManager().getVersions(this.editor.getProfile().getVersionFilter()));

		this.editor.getMinecraftLauncher().getLauncher().getVersionManager().removeRefreshedVersionsListener(this);
	}

	private void updateVersionSelection() {

		Object selection = this.versionList.getSelectedItem();

		if (selection instanceof VersionSyncInfo) {

			Version version = ((VersionSyncInfo)selection).getLatestVersion();
			this.editor.getProfile().setLastVersionId(version.getId());

		} else {

			if (selection == null) {

				this.editor.getProfile().setLastVersionId(null);

			} else {

				this.editor.getProfile().setLastVersionId(selection == null ? null : ((String)selection).substring(((String)selection).indexOf(" ") + 1));
			}
		} 
	}

	private void populateVersions(List<VersionSyncInfo> versions) {

		Collections.sort(versions, new DateComparator());
		
		String previous = this.editor.getProfile().getLastVersionId();

		VersionSyncInfo selected = null;

		this.versionList.removeAllItems();

		boolean latestRelease = false;
		boolean latestSnapshot = false;

		for (VersionSyncInfo vsi : versions) {

			if (vsi.getRemoteVersion() != null) {

				if (vsi.getRemoteVersion().getType() == MinecraftReleaseType.RELEASE) latestRelease = true;
				if (vsi.getRemoteVersion().getType() == MinecraftReleaseType.SNAPSHOT) latestSnapshot = true;
			}
		}

		if (latestRelease) this.versionList.addItem("Use Latest Version");
		if (latestSnapshot) this.versionList.addItem("Use Latest Snapshot");

		for (VersionSyncInfo version : versions) {

			if (version.getLatestVersion().getId().equals(previous)) selected = version; 

			this.versionList.addItem(version);
		}

		Profile profile = this.editor.getMinecraftLauncher().getProfileManager().getSelectedProfile();
		
		if (selected == null && !versions.isEmpty()) {
			
			for (int i = 0; i < this.versionList.getModel().getSize(); i++) {
				
				Object o = this.versionList.getModel().getElementAt(i);
				
				if (!(o instanceof String)) continue;
				
				if (((String)o).equals("Use " + profile.getLastVersionId())) {
					
					this.versionList.setSelectedIndex(i);
					return;
				}
			}
			
			this.versionList.setSelectedIndex(0);

		} else {

			this.versionList.setSelectedItem(selected);
		}
	}

	public void onVersionsRefreshed(final VersionManager manager) {

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				List<VersionSyncInfo> versions = manager.getVersions(ProfileVersionPanel.this.editor.getProfile().getVersionFilter());

				ProfileVersionPanel.this.populateVersions(versions);
				ProfileVersionPanel.this.editor.getMinecraftLauncher().getLauncher().getVersionManager().removeRefreshedVersionsListener(ProfileVersionPanel.this);
			}
		});
	}

	private static class ReleaseTypeCheckBox extends JCheckBox {

		private static final long serialVersionUID = 1L;

		private final MinecraftReleaseType type;

		private ReleaseTypeCheckBox(MinecraftReleaseType type) {

			super(type.getDescription());

			this.type = type;
		}

		public MinecraftReleaseType getType() {

			return this.type;
		}
	}

	private static class VersionListRenderer extends BasicComboBoxRenderer {

		private static final long serialVersionUID = 1L;

		private VersionListRenderer() {}

		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

			if (value instanceof VersionSyncInfo) {

				VersionSyncInfo syncInfo = (VersionSyncInfo)value;

				Version version = syncInfo.getLatestVersion();

				value = String.format("%s %s", new Object[] { version.getType().getName(), version.getId() });
			}

			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

			return this;
		}
	}

	public static class DateComparator implements Comparator<VersionSyncInfo> {

		@Override
		public int compare(VersionSyncInfo o1, VersionSyncInfo o2) {

			if (o2.getRemoteVersion() == null) return -1;
			if (o1.getRemoteVersion() == null) return 1;
			
			Date o2d = /*o2.getRemoteVersion() == null ? o2.getLocalVersion().getReleaseTime() :*/ o2.getRemoteVersion().getReleaseTime();
			Date o1d = /*o1.getRemoteVersion() == null ? o1.getLocalVersion().getReleaseTime() :*/ o1.getRemoteVersion().getReleaseTime();

			return o2d.compareTo(o1d);  
		}
	}
}
