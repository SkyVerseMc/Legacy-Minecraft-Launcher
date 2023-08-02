package com.mojang.launcher.updater.download.assets;

import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.MonitoringInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.zip.GZIPInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AssetDownloadable extends Downloadable {

	private static final Logger LOGGER = LogManager.getLogger();

	private final String name;

	private final AssetIndex.AssetObject asset;

	private final String urlBase;

	private final File destination;

	private Status status = Status.DOWNLOADING;

	public AssetDownloadable(Proxy proxy, String name, AssetIndex.AssetObject asset, String urlBase, File destination) throws MalformedURLException {

		super(proxy, new URL(urlBase + createPathFromHash(asset.getHash())), new File(destination, createPathFromHash(asset.getHash())), false);

		this.name = name;
		this.asset = asset;
		this.urlBase = urlBase;
		this.destination = destination;
	}

	protected static String createPathFromHash(String hash) {

		return hash.substring(0, 2) + "/" + hash;
	}

	public String download() throws IOException {

		this.status = Status.DOWNLOADING;
		this.numAttempts++;

		File localAsset = getTarget();
		File localCompressed = this.asset.hasCompressedAlternative() ? new File(this.destination, createPathFromHash(this.asset.getCompressedHash())) : null;

		URL remoteAsset = getUrl();
		URL remoteCompressed = this.asset.hasCompressedAlternative() ? new URL(this.urlBase + createPathFromHash(this.asset.getCompressedHash())) : null;

		ensureFileWritable(localAsset);

		if (localCompressed != null)

			ensureFileWritable(localCompressed);

		if (localAsset.isFile()) {

			if (FileUtils.sizeOf(localAsset) == this.asset.getSize()) return "Have local file and it's the same size; assuming it's okay!";

			LOGGER.warn("Had local file but it was the wrong size... had {} but expected {}", new Object[] { Long.valueOf(FileUtils.sizeOf(localAsset)), Long.valueOf(this.asset.getSize()) });

			FileUtils.deleteQuietly(localAsset);

			this.status = Status.DOWNLOADING;
		}

		if (localCompressed != null && localCompressed.isFile()) {

			String localCompressedHash = getDigest(localCompressed, "SHA", 40);

			if (localCompressedHash.equalsIgnoreCase(this.asset.getCompressedHash())) return decompressAsset(localAsset, localCompressed);

			LOGGER.warn("Had local compressed but it was the wrong hash... expected {} but had {}", new Object[] { this.asset.getCompressedHash(), localCompressedHash });

			FileUtils.deleteQuietly(localCompressed);
		}

		if (remoteCompressed != null && localCompressed != null) {

			HttpURLConnection httpURLConnection = makeConnection(remoteCompressed);

			int i = httpURLConnection.getResponseCode();

			if (i / 100 == 2) {

				updateExpectedSize(httpURLConnection);

				MonitoringInputStream monitoringInputStream = new MonitoringInputStream(httpURLConnection.getInputStream(), getMonitor());

				FileOutputStream outputStream = new FileOutputStream(localCompressed);

				String hash = copyAndDigest((InputStream)monitoringInputStream, outputStream, "SHA", 40);

				if (hash.equalsIgnoreCase(this.asset.getCompressedHash())) return decompressAsset(localAsset, localCompressed);

				FileUtils.deleteQuietly(localCompressed);

				throw new RuntimeException(String.format("Hash did not match downloaded compressed asset (Expected %s, downloaded %s)", new Object[] { this.asset.getCompressedHash(), hash }));
			}

			throw new RuntimeException("Server responded with " + i);
		}

		HttpURLConnection connection = makeConnection(remoteAsset);

		int status = connection.getResponseCode();

		if (status / 100 == 2) {

			updateExpectedSize(connection);

			MonitoringInputStream monitoringInputStream = new MonitoringInputStream(connection.getInputStream(), getMonitor());

			FileOutputStream outputStream = new FileOutputStream(localAsset);

			String hash = copyAndDigest((InputStream)monitoringInputStream, outputStream, "SHA", 40);

			if (hash.equalsIgnoreCase(this.asset.getHash())) return "Downloaded asset and hash matched successfully";

			FileUtils.deleteQuietly(localAsset);

			throw new RuntimeException(String.format("Hash did not match downloaded asset (Expected %s, downloaded %s)", new Object[] { this.asset.getHash(), hash }));
		}

		throw new RuntimeException("Server responded with " + status);
	}

	public String getStatus() {

		return this.status.name + " " + this.name;
	}

	protected String decompressAsset(File localAsset, File localCompressed) throws IOException {

		String hash;

		this.status = Status.EXTRACTING;

		OutputStream outputStream = FileUtils.openOutputStream(localAsset);

		InputStream inputStream = new GZIPInputStream(FileUtils.openInputStream(localCompressed));

		try {

			hash = copyAndDigest(inputStream, outputStream, "SHA", 40);

		} finally {

			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(inputStream);
		}

		this.status = Status.DOWNLOADING;

		if (hash.equalsIgnoreCase(this.asset.getHash())) return "Had local compressed asset, unpacked successfully and hash matched";

		FileUtils.deleteQuietly(localAsset);

		throw new RuntimeException("Had local compressed asset but unpacked hash did not match (expected " + this.asset.getHash() + " but had " + hash + ")");
	}

	private enum Status {

		DOWNLOADING("Downloading"),
		EXTRACTING("Extracting");

		private final String name;

		Status(String name) {

			this.name = name;
		}
	}
}
