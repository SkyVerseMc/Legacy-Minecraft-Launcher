package com.mojang.authlib;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.properties.PropertyMap;

import fr.litarvan.openauth.microsoft.MicrosoftAuthenticationException;

import java.util.Map;

public interface UserAuthentication {

	boolean canLogIn();

	void logIn() throws AuthenticationException, MicrosoftAuthenticationException;

	void logOut();

	boolean isLoggedIn();

	boolean canPlayOnline();

	GameProfile[] getAvailableProfiles();

	GameProfile getSelectedProfile();

	void selectGameProfile(GameProfile paramGameProfile) throws AuthenticationException;

	void loadFromStorage(Map<String, Object> paramMap);

	Map<String, Object> saveForStorage();

	void setUsername(String paramString);

	void setPassword(String paramString);

	String getAuthenticatedToken();

	String getUserID();

	PropertyMap getUserProperties();

	UserType getUserType();
}
