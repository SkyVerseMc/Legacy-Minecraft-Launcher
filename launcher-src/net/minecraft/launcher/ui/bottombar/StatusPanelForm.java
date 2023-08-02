package net.minecraft.launcher.ui.bottombar;

import java.awt.GridBagConstraints;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.mojang.launcher.Http;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;

import net.minecraft.launcher.Launcher;

public class StatusPanelForm extends SidebarGridForm {

	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LogManager.getLogger();

	private static final String SERVER_SESSION = "session.minecraft.net";

	private static final String SERVER_LOGIN = "login.minecraft.net";

	private final Launcher minecraftLauncher;

	private final JLabel sessionStatus = new JLabel("???");

	private final JLabel loginStatus = new JLabel("???");

	private final Gson gson = (new GsonBuilder()).registerTypeAdapterFactory((TypeAdapterFactory)new LowerCaseEnumTypeAdapterFactory()).create();

	public StatusPanelForm(Launcher minecraftLauncher) {
		
		this.minecraftLauncher = minecraftLauncher;
		
		createInterface();
		refreshStatuses();
	}

	protected void populateGrid(GridBagConstraints constraints) {
		
		add(new JLabel("Multiplayer:", 2), constraints, 0, 0, 0, 1, 17);
		add(this.sessionStatus, constraints, 1, 0, 1, 1);
		add(new JLabel("Login:", 2), constraints, 0, 1, 0, 1, 17);
		add(this.loginStatus, constraints, 1, 1, 1, 1);
	}

	public JLabel getSessionStatus() {
		
		return this.sessionStatus;
	}

	public JLabel getLoginStatus() {
		
		return this.loginStatus;
	}

	@SuppressWarnings("unchecked")
	public void refreshStatuses() {
		
		this.minecraftLauncher.getLauncher().getVersionManager().getExecutorService().submit(new Runnable() {
		
			public void run() {
			
				try {
				
					TypeToken<List<Map<String, StatusPanelForm.ServerStatus>>> token = new TypeToken<List<Map<String, StatusPanelForm.ServerStatus>>>(){};
					
					List<Map<String, StatusPanelForm.ServerStatus>> statuses = (List<Map<String, StatusPanelForm.ServerStatus>>) StatusPanelForm.this.gson.fromJson(Http.performGet(new URL("https://status.mojang.com/check"), StatusPanelForm.this.minecraftLauncher.getLauncher().getProxy()), token.getType());
					
					for (Map<String, StatusPanelForm.ServerStatus> serverStatusInformation : statuses) {
					
						if (serverStatusInformation.containsKey(SERVER_LOGIN)) {
						
							StatusPanelForm.this.loginStatus.setText((serverStatusInformation.get(SERVER_LOGIN)).title);
							continue;
						}
						
						if (serverStatusInformation.containsKey(SERVER_SESSION)) {
							
							StatusPanelForm.this.sessionStatus.setText((serverStatusInformation.get(SERVER_SESSION).title)); 
						}
					} 
				} catch (Exception e) {
					StatusPanelForm.LOGGER.error("Couldn't get server status", e);
				} 
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
