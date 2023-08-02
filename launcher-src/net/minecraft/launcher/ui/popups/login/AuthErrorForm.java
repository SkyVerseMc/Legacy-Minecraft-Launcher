package net.minecraft.launcher.ui.popups.login;

import java.net.URL;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.mojang.launcher.Http;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;

public class AuthErrorForm extends JPanel {

	private static final long serialVersionUID = 1L;

	private final LogInPopup popup;

	private final JLabel errorLabel = new JLabel();

	private final Gson gson = (new GsonBuilder()).registerTypeAdapterFactory((TypeAdapterFactory)new LowerCaseEnumTypeAdapterFactory()).create();

	public AuthErrorForm(LogInPopup popup) {
		
		this.popup = popup;
		
		createInterface();
		clear();
	}

	protected void createInterface() {
		
		setBorder(new EmptyBorder(0, 0, 15, 0));
		
		this.errorLabel.setFont(this.errorLabel.getFont().deriveFont(1));
		
		add(this.errorLabel);
	}

	public void clear() {
		
		setVisible(false);
	}

	public void setVisible(boolean value) {
		
		super.setVisible(value);
		
		this.popup.repack();
	}

	public void displayError(final Throwable throwable, String... lines) {
		
		if (SwingUtilities.isEventDispatchThread()) {
		
			String error = "";
			
			for (String line : lines) {
				
				error = error + "<p>" + line + "</p>"; 
			}
			
			if (throwable != null) {
				
				error = error + "<p style='font-size: 0.9em; font-style: italic;'>(" + ExceptionUtils.getRootCauseMessage(throwable) + ")</p>"; 
			}
			
			this.errorLabel.setText("<html><div style='text-align: center;'>" + error + " </div></html>");
			
			if (!isVisible()) refreshStatuses(); 
			
			setVisible(true);
		
		} else {
		
			SwingUtilities.invokeLater(new Runnable() {
			
				public void run() {
				
					AuthErrorForm.this.displayError(throwable, lines);
				}
			});
		} 
	}

	@SuppressWarnings("unchecked")
	public void refreshStatuses() {
		
		this.popup.getMinecraftLauncher().getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
		
			public void run() {
			
				try {
				
					TypeToken<Map<String, AuthErrorForm.ServerStatus>> token = new TypeToken<Map<String, AuthErrorForm.ServerStatus>>() {};
					
					Map<String, AuthErrorForm.ServerStatus> statuses = (Map<String, AuthErrorForm.ServerStatus>)AuthErrorForm.this.gson.fromJson(Http.performGet(new URL("https://status.mojang.com/check?service=authserver.mojang.com"), AuthErrorForm.this.popup.getMinecraftLauncher().getLauncher().getProxy()), token.getType());
					
					if (statuses.get("authserver.mojang.com") == AuthErrorForm.ServerStatus.RED) {
						
						AuthErrorForm.this.displayError((Throwable)null, new String[] { "It looks like our servers are down right now. Sorry!", "We're already working on the problem and will have it fixed soon.", "Please try again later!" }); 
					}
				} catch (Exception exception) {}
			}
		});
	}

	public enum ServerStatus {
		
		GREEN("Online, no problems detected."),
		YELLOW("May be experiencing issues."),
		RED("Offline, experiencing problems.");

		private final String title;

		ServerStatus(String title) {
		
			this.title = title;
		}
	}
}
