package net.minecraft.launcher.ui.popups.login;

import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.launcher.OperatingSystem;
import com.mojang.util.UUIDTypeAdapter;

import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.profile.AuthenticationDatabase;

public class LogInForm extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger();

	private final LogInPopup popup;

	private final JTextField usernameField = new JTextField();

	private final JPasswordField passwordField = new JPasswordField();

	private final JComboBox<String> userDropdown = new JComboBox<String>();

	private final JPanel userDropdownPanel = new JPanel();

	private final UserAuthentication authentication;

	public LogInForm(LogInPopup popup) {

		this.popup = popup;
		this.authentication = popup.getMinecraftLauncher().getProfileManager().getAuthDatabase().getAuthenticationService().createUserAuthentication(Agent.MINECRAFT);
		this.usernameField.addActionListener(this);
		this.passwordField.addActionListener(this);

		createInterface();
	}

	protected void createInterface() {

		setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = 2;
		constraints.gridx = 0;
		constraints.gridy = -1;
		constraints.weightx = 1.0D;

		add(Box.createGlue());

		JLabel usernameLabel = new JLabel("Email Address or Username:");

		Font labelFont = usernameLabel.getFont().deriveFont(1);
		Font smalltextFont = usernameLabel.getFont().deriveFont(labelFont.getSize() - 2.0F);

		usernameLabel.setFont(labelFont);

		add(usernameLabel, constraints);
		add(this.usernameField, constraints);

		JLabel forgotUsernameLabel = new JLabel("(Which do I use?)");

		forgotUsernameLabel.setCursor(new Cursor(12));
		forgotUsernameLabel.setFont(smalltextFont);
		forgotUsernameLabel.setHorizontalAlignment(4);

		forgotUsernameLabel.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {

				OperatingSystem.openLink(LauncherConstants.URL_FORGOT_USERNAME);
			}
		});

		add(forgotUsernameLabel, constraints);
		add(Box.createVerticalStrut(10), constraints);

		JLabel passwordLabel = new JLabel("Password:");

		passwordLabel.setFont(labelFont);

		add(passwordLabel, constraints);
		add(this.passwordField, constraints);

		JLabel forgotPasswordLabel = new JLabel("(Forgot Password?)");

		forgotPasswordLabel.setCursor(new Cursor(12));
		forgotPasswordLabel.setFont(smalltextFont);
		forgotPasswordLabel.setHorizontalAlignment(4);

		forgotPasswordLabel.addMouseListener(new MouseAdapter() {

			public void mouseClicked(MouseEvent e) {

				OperatingSystem.openLink(LauncherConstants.URL_FORGOT_PASSWORD_MINECRAFT);
			}
		});

		add(forgotPasswordLabel, constraints);

		createUserDropdownPanel(labelFont);

		add(this.userDropdownPanel, constraints);
		add(Box.createVerticalStrut(10), constraints);
	}

	protected void createUserDropdownPanel(Font labelFont) {

		this.userDropdownPanel.setLayout(new GridBagLayout());

		GridBagConstraints constraints = new GridBagConstraints();
		constraints.fill = 2;
		constraints.gridx = 0;
		constraints.gridy = -1;
		constraints.weightx = 1.0D;

		this.userDropdownPanel.add(Box.createVerticalStrut(8), constraints);

		JLabel userDropdownLabel = new JLabel("Character Name:");
		userDropdownLabel.setFont(labelFont);

		this.userDropdownPanel.add(userDropdownLabel, constraints);
		this.userDropdownPanel.add(this.userDropdown, constraints);
		this.userDropdownPanel.setVisible(false);
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == this.usernameField || e.getSource() == this.passwordField) tryLogIn(); 
	}

	public void tryLogIn() {

		if (this.authentication.isLoggedIn() && this.authentication.getSelectedProfile() == null && ArrayUtils.isNotEmpty((Object[])this.authentication.getAvailableProfiles())) {

			this.popup.setCanLogIn(false);
			GameProfile selectedProfile = null;

			for (GameProfile profile : this.authentication.getAvailableProfiles()) {

				if (profile.getName().equals(this.userDropdown.getSelectedItem())) {

					selectedProfile = profile;
					break;
				} 
			} 

			if (selectedProfile == null) selectedProfile = this.authentication.getAvailableProfiles()[0]; 

			final GameProfile finalSelectedProfile = selectedProfile;

			this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {

				public void run() {

					try {

						LogInForm.this.authentication.selectGameProfile(finalSelectedProfile);
						LogInForm.this.popup.getMinecraftLauncher().getProfileManager().getAuthDatabase().register(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()), LogInForm.this.authentication);
						LogInForm.this.popup.setLoggedIn(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()));

					} catch (InvalidCredentialsException ex) {

						LogInForm.LOGGER.error("Couldn't log in", (Throwable)ex);
						LogInForm.this.popup.getErrorForm().displayError((Throwable)ex, new String[] { "Sorry, but we couldn't log you in right now.", "Please try again later." });
						LogInForm.this.popup.setCanLogIn(true);

					} catch (AuthenticationException ex) {

						LogInForm.LOGGER.error("Couldn't log in", (Throwable)ex);
						LogInForm.this.popup.getErrorForm().displayError((Throwable)ex, new String[] { "Sorry, but we couldn't connect to our servers.", "Please make sure that you are online and that Minecraft is not blocked." });
						LogInForm.this.popup.setCanLogIn(true);
					} 
				}
			});
		} else {

			this.popup.setCanLogIn(false);
			this.authentication.logOut();
			this.authentication.setUsername(this.usernameField.getText());
			this.authentication.setPassword(String.valueOf(this.passwordField.getPassword()));

			final int passwordLength = (this.passwordField.getPassword()).length;

			this.passwordField.setText("");

			this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().execute(new Runnable() {

				public void run() {

					try {

						LogInForm.this.authentication.logIn();

						AuthenticationDatabase authDatabase = LogInForm.this.popup.getMinecraftLauncher().getProfileManager().getAuthDatabase();

						if (LogInForm.this.authentication.getSelectedProfile() == null) {

							if (ArrayUtils.isNotEmpty((Object[])LogInForm.this.authentication.getAvailableProfiles())) {

								for (GameProfile profile : LogInForm.this.authentication.getAvailableProfiles()) {

									LogInForm.this.userDropdown.addItem(profile.getName()); 
								}

								SwingUtilities.invokeLater(new Runnable() {

									public void run() {

										LogInForm.this.usernameField.setEditable(false);
										LogInForm.this.passwordField.setEditable(false);
										LogInForm.this.userDropdownPanel.setVisible(true);
										LogInForm.this.popup.repack();
										LogInForm.this.popup.setCanLogIn(true);
										LogInForm.this.passwordField.setText(StringUtils.repeat('*', passwordLength));
									}
								});

							} else {

								String uuid = "demo-" + LogInForm.this.usernameField.getText();

								authDatabase.register(uuid, LogInForm.this.authentication);

								LogInForm.this.popup.setLoggedIn(uuid);
							}

						} else {

							authDatabase.register(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()), LogInForm.this.authentication);

							LogInForm.this.popup.setLoggedIn(UUIDTypeAdapter.fromUUID(LogInForm.this.authentication.getSelectedProfile().getId()));
						}

					} catch (UserMigratedException | MicrosoftAuthenticationException ex) {

						LogInForm.LOGGER.error("Couldn't log in", (Throwable)ex);
						LogInForm.this.popup.getErrorForm().displayError((Throwable)ex, new String[] { "Sorry, but we can't log you in with your username.", "You have migrated your account, please use your email address." });
						LogInForm.this.popup.setCanLogIn(true);

					} catch (InvalidCredentialsException ex) {

						LogInForm.LOGGER.error("Couldn't log in", (Throwable)ex);
						LogInForm.this.popup.getErrorForm().displayError((Throwable)ex, new String[] { "Sorry, but your username or password is incorrect!", "Please try again. If you need help, try the 'Forgot Password' link." });
						LogInForm.this.popup.setCanLogIn(true);

					} catch (AuthenticationException ex) {

						LogInForm.LOGGER.error("Couldn't log in", (Throwable)ex);
						LogInForm.this.popup.getErrorForm().displayError((Throwable)ex, new String[] { "Sorry, but we couldn't connect to our servers.", "Please make sure that you are online and that Minecraft is not blocked." });
						LogInForm.this.popup.setCanLogIn(true);
					} 
				}
			});
		} 
	}
}
