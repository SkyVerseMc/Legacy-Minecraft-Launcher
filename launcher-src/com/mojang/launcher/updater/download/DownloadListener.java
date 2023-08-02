package com.mojang.launcher.updater.download;

public interface DownloadListener {

	void onDownloadJobFinished(DownloadJob paramDownloadJob);

	void onDownloadJobProgressChanged(DownloadJob paramDownloadJob);
}
