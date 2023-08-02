package net.minecraft.launcher.updater;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.MonitoringInputStream;

public class PreHashedDownloadable extends Downloadable {

	private final String expectedHash;

	public PreHashedDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload, String expectedHash) {

		super(proxy, remoteFile, localFile, forceDownload);

		this.expectedHash = expectedHash;
	}

	public String download() throws IOException {

		this.numAttempts++;
		ensureFileWritable(getTarget());

		File target = getTarget();
		String localHash = null;

		if (target.isFile()) {

			localHash = getDigest(target, "SHA-1", 40);

			if (this.expectedHash.equalsIgnoreCase(localHash)) return "Local file matches hash, using that"; 

			FileUtils.deleteQuietly(target);
		}

		try {

			HttpURLConnection connection = makeConnection(getUrl());

			int status = connection.getResponseCode();

			if (status / 100 == 2) {

				updateExpectedSize(connection);

				MonitoringInputStream monitoringInputStream = new MonitoringInputStream(connection.getInputStream(), getMonitor());

				FileOutputStream outputStream = new FileOutputStream(getTarget());

				String digest = copyAndDigest((InputStream)monitoringInputStream, outputStream, "SHA", 40);

				if (this.expectedHash.equalsIgnoreCase(digest)) return "Downloaded successfully and hash matched"; 

				throw new RuntimeException(String.format("Hash did not match downloaded file (Expected %s, downloaded %s)", new Object[] { this.expectedHash, digest }));
			}

			if (getTarget().isFile()) return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good"; 

			throw new RuntimeException("Server responded with " + status);

		} catch (IOException e) {

			if (getTarget().isFile()) return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good"; 

			throw e;
		} 
	}
}
