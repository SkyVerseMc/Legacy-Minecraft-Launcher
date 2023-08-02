package net.minecraft.launcher.updater;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.mojang.launcher.versions.Version;

import net.minecraft.launcher.game.MinecraftReleaseType;

public abstract class FileBasedVersionList extends VersionList {

	public String getContent(String path) throws IOException {

		return IOUtils.toString(getFileInputStream(path)).replaceAll("\\r\\n", "\r").replaceAll("\\r", "\n");
	}

	protected abstract InputStream getFileInputStream(String paramString) throws FileNotFoundException;

	public CompleteMinecraftVersion getCompleteVersion(Version version) throws IOException {

		if (version instanceof CompleteMinecraftVersion) return (CompleteMinecraftVersion)version; 

		if (!(version instanceof PartialVersion)) throw new IllegalArgumentException("Version must be a partial"); 

		PartialVersion partial = (PartialVersion)version;
		CompleteMinecraftVersion complete = (CompleteMinecraftVersion)this.gson.fromJson(getContent("versions/" + version.getId() + "/" + version.getId() + ".json"), CompleteMinecraftVersion.class);

		MinecraftReleaseType type = (MinecraftReleaseType)version.getType();

		replacePartialWithFull(partial, complete);

		return complete;
	}
}
