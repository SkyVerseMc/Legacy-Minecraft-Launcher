package com.mojang.launcher.updater;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.versions.Version;

import net.minecraft.launcher.game.MinecraftReleaseType;

public interface VersionManager {

	void refreshVersions() throws IOException;

	List<VersionSyncInfo> getVersions();

	List<VersionSyncInfo> getVersions(VersionFilter<? extends MinecraftReleaseType> paramVersionFilter);

	VersionSyncInfo getVersionSyncInfo(Version paramVersion);

	VersionSyncInfo getVersionSyncInfo(String paramString);

	VersionSyncInfo getVersionSyncInfo(Version paramVersion1, Version paramVersion2);

	List<VersionSyncInfo> getInstalledVersions();

	Version getLatestVersion(VersionSyncInfo paramVersionSyncInfo) throws IOException;

	DownloadJob downloadVersion(VersionSyncInfo paramVersionSyncInfo, DownloadJob paramDownloadJob) throws IOException;

	DownloadJob downloadResources(DownloadJob paramDownloadJob, Version paramCompleteVersion) throws IOException;

	ThreadPoolExecutor getExecutorService();

	void addRefreshedVersionsListener(RefreshedVersionsListener paramRefreshedVersionsListener);

	void removeRefreshedVersionsListener(RefreshedVersionsListener paramRefreshedVersionsListener);

	VersionSyncInfo syncVersion(VersionSyncInfo paramVersionSyncInfo) throws IOException;

	void installVersion(Version paramCompleteVersion) throws IOException;

	void uninstallVersion(Version paramCompleteVersion) throws IOException;
}
