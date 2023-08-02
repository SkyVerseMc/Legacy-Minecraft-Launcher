package net.minecraft.bootstrap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.HttpsURLConnection;

public class Downloader implements Runnable {

	private static final int MAX_RETRIES = 2;

	private final Proxy proxy;

	private final String md5;

	private final File targetFile;

	private final Controller controller;

	private Bootstrap bootstrap;

	private String downloadURL;

	public static class Controller {

		public final CountDownLatch foundUpdateLatch = new CountDownLatch(1);

		public final AtomicBoolean foundUpdate = new AtomicBoolean(false);

		public final CountDownLatch hasDownloadedLatch = new CountDownLatch(1);
	}

	public Downloader(String downloadURL, Controller controller, Bootstrap bootstrap, Proxy proxy, String Md5, File targetFile) {

		this.controller = controller;
		this.bootstrap = bootstrap;
		this.proxy = proxy;
		this.md5 = Md5;
		this.targetFile = targetFile;
		this.downloadURL = downloadURL;
	}

	public void run() {

		int retries = 0;

		while (++retries <= MAX_RETRIES) {

			try {

				URL url = new URL(this.downloadURL);

				HttpsURLConnection connection = getConnection(url);
				connection.setUseCaches(false);
				connection.setDefaultUseCaches(false);
				connection.setRequestProperty("Cache-Control", "no-store,max-age=0,no-cache");
				connection.setRequestProperty("Expires", "0");
				connection.setRequestProperty("Pragma", "no-cache");

				if (this.md5 != null) connection.setRequestProperty("If-None-Match", this.md5.toLowerCase());

				connection.setConnectTimeout(30000);
				connection.setReadTimeout(10000);

				log("Downloading: " + url.getPath() + ((retries > 1) ? String.format(" (try %d/%d)", new Object[] { Integer.valueOf(retries), Integer.valueOf(10) }) : ""));

				long start = System.nanoTime();

				connection.connect();

				long elapsed = System.nanoTime() - start;

				log("Got reply in: " + (elapsed / 1000000L) + "ms");

				int code = connection.getResponseCode() / 100;

				if (code == 2) {

					this.controller.foundUpdate.set(true);
					this.controller.foundUpdateLatch.countDown();

					InputStream inputStream = connection.getInputStream();

					FileOutputStream outputStream = new FileOutputStream(this.targetFile);

					MessageDigest digest = MessageDigest.getInstance("MD5");

					long startDownload = System.nanoTime();
					long bytesRead = 0L;

					byte[] buffer = new byte[65536];

					try {

						int read = inputStream.read(buffer);

						while (read >= 1) {

							bytesRead += read;
							digest.update(buffer, 0, read);
							outputStream.write(buffer, 0, read);
							read = inputStream.read(buffer);
						} 

					} finally {

						inputStream.close();
						outputStream.close();
					} 

					long elapsedDownload = System.nanoTime() - startDownload;

					float elapsedSeconds = (float)(1L + elapsedDownload) / 1.0E9F;
					float kbRead = (float)bytesRead / 1024.0F;

					log(String.format("Downloaded %.1fkb in %ds at %.1fkb/s", new Object[] { Float.valueOf(kbRead), Integer.valueOf((int)elapsedSeconds), Float.valueOf(kbRead / elapsedSeconds) }));

					String md5sum = String.format("%1$032x", new Object[] { new BigInteger(1, digest.digest()) });

					if (!this.md5.equalsIgnoreCase(md5sum)) {

						log("After downloading, the MD5 hash didn't match. Retrying");
						continue;
					} 

					this.controller.hasDownloadedLatch.countDown();
					return;
				} 

				if (code == 4) {

					log("Remote file not found.");
					continue;
				} 

				this.controller.foundUpdate.set(false);
				this.controller.foundUpdateLatch.countDown();

				log("No update found.");
				return;

			} catch (Exception e) {

				e.printStackTrace();
				log("Exception: " + e.toString());
				suggestHelp(e);
			} 
		} 
		log("Unable to download remote file. Check your internet connection/proxy settings.");
	}

	public void suggestHelp(Throwable t) {

		if (t instanceof java.net.BindException) {

			log("Recognized exception: the likely cause is a broken ipv4/6 stack. Check your TCP/IP settings.");

		} else if (t instanceof javax.net.ssl.SSLHandshakeException) {

			log("Recognized exception: the likely cause is a set of broken/missing root-certificates. Check your java install and perhaps reinstall it.");
		} 
	}

	public void log(String str) {

		this.bootstrap.println(str);
	}

	public HttpsURLConnection getConnection(URL url) throws IOException {

		return (HttpsURLConnection)url.openConnection(this.proxy);
	}
}
