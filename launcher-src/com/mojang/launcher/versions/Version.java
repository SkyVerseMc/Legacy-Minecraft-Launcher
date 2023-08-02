package com.mojang.launcher.versions;

import java.util.Date;

import net.minecraft.launcher.game.MinecraftReleaseType;

public interface Version {

	String getId();

	MinecraftReleaseType getType();
	
	void setType(MinecraftReleaseType type);

	Date getUpdatedTime();

	Date getReleaseTime();
	
	String getURL();
	
	String getSha1();
	
	int getComplianceLevel();
}
