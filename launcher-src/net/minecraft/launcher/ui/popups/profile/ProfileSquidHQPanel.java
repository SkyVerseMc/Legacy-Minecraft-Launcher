package net.minecraft.launcher.ui.popups.profile;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.minecraft.launcher.SwingUserInterface;
import net.minecraft.launcher.profile.Profile;

public class ProfileSquidHQPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private static final String SQUIDHQ_WARNING_MESSAGE = "SquidHQ was a launcher that allowed you to bypass the blacklisting of Mojang servers, allowing you to connect to them anyway.\nThis option gives you more freedom, but a server isn't blacklisted for nothing and can be harmful.\nWe are not responsible for what you do with that.";

	private final ProfileEditorPopup editor;

	private final JCheckBox squidhq = new JCheckBox("Launch in SquidHQ Mode");

	public ProfileSquidHQPanel(ProfileEditorPopup editor) {

		this.editor = editor;

		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createTitledBorder("SquidHQ"));

		createInterface();
		this.squidhq.setSelected(this.editor.getProfile().squidHQMode());
	}

	protected void createInterface() {

		this.squidhq.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				Profile profile = ProfileSquidHQPanel.this.editor.getProfile();

				if (ProfileSquidHQPanel.this.squidhq.isSelected()) {

					int result = JOptionPane.showConfirmDialog(((SwingUserInterface)ProfileSquidHQPanel.this.editor.getMinecraftLauncher().getUserInterface()).getFrame(), SQUIDHQ_WARNING_MESSAGE + "\n\nAre you sure you want to continue?");

					if (result == 0) {

						profile.enableSquidHQ(true);

						ProfileSquidHQPanel.this.squidhq.setSelected(true);

						return;
					}
				}

				profile.enableSquidHQ(false);

				ProfileSquidHQPanel.this.squidhq.setSelected(false);
			}
		});

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(2, 2, 2, 2);
		constraints.anchor = 17;
		constraints.gridy = 0;

		constraints.weightx = 0.0D;
		constraints.fill = 0;
		constraints.gridy++;
		constraints.fill = 2;
		constraints.weightx = 1.0D;
		constraints.gridwidth = 0;

		add(this.squidhq, constraints);
	}
}
