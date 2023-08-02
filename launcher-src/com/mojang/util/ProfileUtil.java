package com.mojang.util;

import com.mojang.authlib.GameProfile;

import fr.litarvan.openauth.microsoft.model.response.MinecraftProfile;

public class ProfileUtil {
	
	public static GameProfile minecraftProfileToGameProfile(MinecraftProfile profile) {
		
		return new GameProfile(UUIDTypeAdapter.fromString(profile.getId()), profile.getName());
	}
}
