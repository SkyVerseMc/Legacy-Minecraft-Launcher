package com.mojang.launcher;

import java.io.File;

import com.mojang.launcher.updater.DownloadProgress;
import com.mojang.launcher.versions.Version;

public interface UserInterface {

	void showLoginPrompt();

	void setVisible(boolean paramBoolean);

	void shutdownLauncher();

	void hideDownloadProgress();

	void setDownloadProgress(DownloadProgress paramDownloadProgress);

	void showCrashReport(Version paramVersion, File paramFile, String paramString);

	void gameLaunchFailure(String paramString);

	void updatePlayState();
}
