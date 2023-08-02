package net.minecraft.launcher.updater;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.updater.DateTypeAdapter;
import com.mojang.launcher.updater.LowerCaseEnumTypeAdapterFactory;
import com.mojang.launcher.versions.ReleaseTypeAdapterFactory;
import com.mojang.launcher.versions.ReleaseTypeFactory;
import com.mojang.launcher.versions.Version;

import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.game.MinecraftReleaseTypeFactory;

public abstract class VersionList {

	protected final Gson gson;

	protected final Map<String, Version> versionsByName = new HashMap<String, Version>();

	protected final List<Version> versions = new ArrayList<Version>();

	protected final Map<MinecraftReleaseType, Version> latestVersions = Maps.newEnumMap(MinecraftReleaseType.class);

	public VersionList() {

		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapterFactory((TypeAdapterFactory)new LowerCaseEnumTypeAdapterFactory());
		builder.registerTypeAdapter(Date.class, new DateTypeAdapter());
		builder.registerTypeAdapter(MinecraftReleaseType.class, new ReleaseTypeAdapterFactory((ReleaseTypeFactory) MinecraftReleaseTypeFactory.instance()));
		builder.registerTypeAdapter(Argument.class, new Argument.Serializer());
		builder.enableComplexMapKeySerialization();
		builder.setPrettyPrinting();

		this.gson = builder.create();
	}

	public Collection<Version> getVersions() {

		return this.versions;
	}

	public Version getLatestVersion(MinecraftReleaseType type) {

		if (type == null) throw new IllegalArgumentException("Type cannot be null"); 

		return this.latestVersions.get(type);
	}

	public Version getVersion(String name) {

		if (name == null || name.length() == 0) throw new IllegalArgumentException("Name cannot be null or empty"); 

		return this.versionsByName.get(name);
	}

	public abstract CompleteMinecraftVersion getCompleteVersion(Version paramVersion) throws IOException;

	protected void replacePartialWithFull(PartialVersion version, CompleteMinecraftVersion complete) {

		Collections.replaceAll(this.versions, version, complete);

		this.versionsByName.put(version.getId(), complete);

		if (this.latestVersions.get(version.getType()) == version) this.latestVersions.put(version.getType(), complete); 
	}

	protected void clearCache() {

		this.versionsByName.clear();
		this.versions.clear();
		this.latestVersions.clear();
	}

	public abstract void refreshVersions() throws IOException;

	public Version addVersion(Version version) {

		if (version.getId() == null) throw new IllegalArgumentException("Cannot add blank version"); 

		if (getVersion(version.getId()) != null) throw new IllegalArgumentException("Version '" + version.getId() + "' is already tracked"); 

		this.versions.add(version);
		this.versionsByName.put(version.getId(), version);

		return version;
	}

	public void removeVersion(String name) {

		if (name == null || name.length() == 0) throw new IllegalArgumentException("Name cannot be null or empty"); 

		Version version = getVersion(name);
		if (version == null) throw new IllegalArgumentException("Unknown version - cannot remove null"); 

		removeVersion(version);
	}

	public void removeVersion(Version version) {

		if (version == null) throw new IllegalArgumentException("Cannot remove null version"); 

		this.versions.remove(version);
		this.versionsByName.remove(version.getId());

		for (MinecraftReleaseType type : MinecraftReleaseType.values()) {

			if (getLatestVersion(type) == version) this.latestVersions.remove(type); 
		} 
	}

	public void setLatestVersion(Version version) {

		if (version == null) throw new IllegalArgumentException("Cannot set latest version to null"); 

		this.latestVersions.put((MinecraftReleaseType)version.getType(), version);
	}

	public void setLatestVersion(String name) {

		if (name == null || name.length() == 0) throw new IllegalArgumentException("Name cannot be null or empty"); 
		Version version = getVersion(name);

		if (version == null) throw new IllegalArgumentException("Unknown version - cannot set latest version to null"); 

		setLatestVersion(version);
	}

	public String serializeVersion(Version version) {

		if (version == null) throw new IllegalArgumentException("Cannot serialize null!"); 

		return this.gson.toJson(version);
	}

	public abstract boolean hasAllFiles(CompleteMinecraftVersion paramCompleteMinecraftVersion, OperatingSystem paramOperatingSystem);

	public void uninstallVersion(Version version) {

		removeVersion(version);
	}
}
