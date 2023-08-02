package com.mojang.authlib.yggdrasil;

import java.net.URL;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Multimap;
import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.HttpUserAuthentication;
import com.mojang.authlib.UserType;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.yggdrasil.request.RefreshRequest;
import com.mojang.authlib.yggdrasil.request.ValidateRequest;
import com.mojang.authlib.yggdrasil.response.RefreshResponse;
import com.mojang.authlib.yggdrasil.response.Response;
import com.mojang.authlib.yggdrasil.response.User;
import com.mojang.util.ProfileUtil;

import fr.litarvan.openauth.microsoft.AuthTokens;
import fr.litarvan.openauth.microsoft.MicrosoftAuthResult;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;
import fr.litarvan.openauth.microsoft.MicrosoftAuthenticator;
import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;
import net.minecraft.launcher.Launcher;

public class YggdrasilUserAuthentication extends HttpUserAuthentication {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final String BASE_URL = "https://authserver.mojang.com/";

	private static final URL ROUTE_REFRESH = HttpAuthenticationService.constantURL(BASE_URL + "refresh");
	private static final URL ROUTE_VALIDATE = HttpAuthenticationService.constantURL(BASE_URL + "validate");

	private static final String STORAGE_KEY_ACCESS_TOKEN = "accessToken";

	private final Agent agent;

	private GameProfile[] profiles;

	private String accessToken;
	private String minecraftToken;

	private boolean isOnline;

	public YggdrasilUserAuthentication(YggdrasilAuthenticationService authenticationService, Agent agent) {

		super(authenticationService);

		this.agent = agent;
	}

	public boolean canLogIn() {

		return (!canPlayOnline() && StringUtils.isNotBlank(getUsername()) && (StringUtils.isNotBlank(getPassword()) || StringUtils.isNotBlank(getAuthenticatedToken())));
	}

	public void logIn() throws AuthenticationException, MicrosoftAuthenticationException {

		if (StringUtils.isBlank(getUsername())) throw new InvalidCredentialsException("Invalid username");

		if (StringUtils.isNotBlank(this.accessToken)) {

			try {

				logInWithToken();
				LOGGER.info("Logged as '" + getSelectedProfile().getName() + "'");

			} catch(Exception e) {

				e.printStackTrace();
			}

		} else if (StringUtils.isNotBlank(getPassword())) {

			try {

				logInWithPassword();
				LOGGER.info("Logged as '" + getSelectedProfile().getName() + "'");

			} catch(Exception e) {

				e.printStackTrace();
			}

		} else {

			throw new InvalidCredentialsException("Invalid password");
		}
		
		Launcher.getCurrentInstance().starting = false;
	}

	protected void logInWithPassword() throws AuthenticationException, MicrosoftAuthenticationException {

		if (StringUtils.isBlank(getUsername())) throw new InvalidCredentialsException("Invalid username");

		if (StringUtils.isBlank(getPassword())) throw new InvalidCredentialsException("Invalid password");

		LOGGER.info("Logging in with username & password");

		MicrosoftAuthResult response = new MicrosoftAuthenticator().loginWithCredentials(getUsername(), getPassword());

		MinecraftProfile profile = response.getProfile();

		setSelectedProfile(ProfileUtil.minecraftProfileToGameProfile(profile));

		if (getSelectedProfile() != null) {

			setUserType(getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG);
		}

		User user = new User(getUserID());

		if (user != null && user.getId() != null) {

			setUserid(user.getId());

		} else {

			setUserid(getUsername());
		}

		this.isOnline = true;
		this.accessToken = response.getAccessToken();
		this.minecraftToken = response.getMinecraftToken();
		this.profiles = new GameProfile[] { getSelectedProfile() };
		getModifiableUserProperties().clear();

		updateUserProperties(user);
	}

	protected void updateUserProperties(User user) {

		if (user == null) return;

		if (user.getProperties() != null) getModifiableUserProperties().putAll((Multimap)user.getProperties());
	}

	protected void logInWithToken() throws AuthenticationException, MicrosoftAuthenticationException {

		if (StringUtils.isBlank(getUserID()))

			if (StringUtils.isBlank(getUsername())) {

				setUserid(getUsername());

			} else {

				throw new InvalidCredentialsException("Invalid uuid & username");
			}

		if (StringUtils.isBlank(this.accessToken)) throw new InvalidCredentialsException("Invalid access token");

		LOGGER.info("Logging in with access token");

		if (checkTokenValidity()) {

			LOGGER.debug("Skipping refresh call as we're safely logged in.");

			this.isOnline = true;

			return;
		}

		MicrosoftAuthResult result = new MicrosoftAuthenticator().loginWithTokens(new AuthTokens(this.accessToken, ""));

		MinecraftProfile profile = result.getProfile();

		setSelectedProfile(ProfileUtil.minecraftProfileToGameProfile(profile));

		if (getSelectedProfile() != null) {

			setUserType(getSelectedProfile().isLegacy() ? UserType.LEGACY : UserType.MOJANG);
		}

		User user = new User(getUserID());

		if (user != null && user.getId() != null) {

			setUserid(user.getId());

		} else {

			setUserid(getUsername());
		}

		this.isOnline = true;
		this.accessToken = result.getAccessToken();
		this.minecraftToken = result.getMinecraftToken();
		this.profiles = new GameProfile[] { getSelectedProfile() };

		getModifiableUserProperties().clear();

		updateUserProperties(user);
	}

	protected boolean checkTokenValidity() throws AuthenticationException {

		ValidateRequest request = new ValidateRequest(this);

		try {

			getAuthenticationService().makeRequest(ROUTE_VALIDATE, request, Response.class);

			return true;

		} catch (AuthenticationException ex) {

			return false;
		}
	}

	public void logOut() {

		super.logOut();

		this.accessToken = null;
		this.minecraftToken = null;
		this.profiles = null;
		this.isOnline = false;
	}

	public GameProfile[] getAvailableProfiles() {

		return this.profiles;
	}

	public boolean isLoggedIn() {

		return StringUtils.isNotBlank(this.minecraftToken);
	}

	public boolean canPlayOnline() {

		return (isLoggedIn() && getSelectedProfile() != null && this.isOnline);
	}

	public void selectGameProfile(GameProfile profile) throws AuthenticationException {

		if (!isLoggedIn()) throw new AuthenticationException("Cannot change game profile whilst not logged in");

		if (getSelectedProfile() != null) {

			throw new AuthenticationException("Cannot change game profile. You must log out and back in.");
		}

		if (profile == null || !ArrayUtils.contains((Object[])this.profiles, profile)) {

			throw new IllegalArgumentException("Invalid profile '" + profile + "'");
		}

		RefreshRequest request = new RefreshRequest(this, profile);

		RefreshResponse response = getAuthenticationService().<RefreshResponse>makeRequest(ROUTE_REFRESH, request, RefreshResponse.class);

		if (!response.getClientToken().equals(getAuthenticationService().getClientToken())) {

			throw new AuthenticationException("Server requested we change our client token. Don't know how to handle this!");
		}

		this.isOnline = true;
		this.accessToken = response.getAccessToken();

		setSelectedProfile(response.getSelectedProfile());
	}

	public void loadFromStorage(Map<String, Object> credentials) {

		super.loadFromStorage(credentials);

		this.accessToken = String.valueOf(credentials.get(STORAGE_KEY_ACCESS_TOKEN));
	}

	public Map<String, Object> saveForStorage() {

		Map<String, Object> result = super.saveForStorage();

		if (StringUtils.isNotBlank(this.accessToken)) {

			result.put(STORAGE_KEY_ACCESS_TOKEN, this.accessToken);
		}

		return result;
	}

	public String getAuthenticatedToken() {

		return this.minecraftToken;
	}

	public Agent getAgent() {

		return this.agent;
	}

	public String toString() {

		return "YggdrasilAuthenticationService{agent=" + this.agent + ", profiles=" + Arrays.toString((Object[])this.profiles) + ", selectedProfile=" + getSelectedProfile() + ", username='" + getUsername() + '\'' + ", isLoggedIn=" + isLoggedIn() + ", userType=" + getUserType() + ", canPlayOnline=" + canPlayOnline() + ", accessToken='" + this.accessToken + '\'' + ", clientToken='" + getAuthenticationService().getClientToken() + '\'' + '}';
	}

	public YggdrasilAuthenticationService getAuthenticationService() {

		return (YggdrasilAuthenticationService)super.getAuthenticationService();
	}
}
