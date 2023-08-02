package com.mojang.launcher.updater.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;

public class ChecksummedDownloadable extends Downloadable {

	private String localHash;

	private String expectedHash;

	public ChecksummedDownloadable(Proxy proxy, URL remoteFile, File localFile, boolean forceDownload) {

		super(proxy, remoteFile, localFile, forceDownload);
	}

	public String download() throws IOException {

		this.numAttempts++;

		ensureFileWritable(getTarget());

		File target = getTarget();

		if (this.localHash == null && target.isFile())

			this.localHash = getDigest(target, "SHA-1", 40);

		if (this.expectedHash == null) {

			try {

				HttpURLConnection connection = makeConnection(new URL(getUrl().toString() + ".sha1"));

				int status = connection.getResponseCode();

				if (status / 100 == 2) {

					InputStream inputStream = connection.getInputStream();

					try {

						this.expectedHash = IOUtils.toString(inputStream, Charsets.UTF_8).trim();

					} catch (IOException e) {

						this.expectedHash = "";

					} finally {

						IOUtils.closeQuietly(inputStream);
					}

				} else {

					this.expectedHash = "";
				}

			} catch (IOException e) {

				this.expectedHash = "";
			}
		}

		if (this.expectedHash.length() == 0 && target.isFile()) return "Couldn't find a checksum so assuming our copy is good";

		if (this.expectedHash.equalsIgnoreCase(this.localHash)) return "Remote checksum matches local file";

		try {

			HttpURLConnection connection = makeConnection(getUrl());

			int status = connection.getResponseCode();

			if (status / 100 == 2) {

				updateExpectedSize(connection);

				InputStream inputStream = new MonitoringInputStream(connection.getInputStream(), getMonitor());

				FileOutputStream outputStream = new FileOutputStream(getTarget());

				String digest = copyAndDigest(inputStream, outputStream, "SHA", 40);

				if (this.expectedHash.length() == 0) return "Didn't have checksum so assuming the downloaded file is good";

				if (this.expectedHash.equalsIgnoreCase(digest)) return "Downloaded successfully and checksum matched";

				throw new RuntimeException(String.format("Checksum did not match downloaded file (Checksum was %s, downloaded %s)", new Object[] { this.expectedHash, digest }));
			}

			if (getTarget().isFile()) return "Couldn't connect to server (responded with " + status + ") but have local file, assuming it's good";

			throw new RuntimeException("Server responded with " + status);

		} catch (IOException e) {

			if (getTarget().isFile() && (this.expectedHash == null || this.expectedHash.length() == 0)) {
				
				return "Couldn't connect to server (" + e.getClass().getSimpleName() + ": '" + e.getMessage() + "') but have local file, assuming it's good";
			}

			throw e;
		}
	}
}
