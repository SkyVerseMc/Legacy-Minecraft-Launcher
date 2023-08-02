package net.minecraft.bootstrap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class LauncherUpdateUtil {

	public static Object[] checkForUpdates(Bootstrap bootstrap) {

		try {

			URL url = new URL(Bootstrap.LAUNCHER_URL);

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

			if (urlConnection.getResponseCode() != 200) {

				bootstrap.println("Unexpected exception whilst looking for updates: " + urlConnection.getResponseMessage());

			} else {

				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));  

				StringBuilder content = new StringBuilder();
				String line;  

				while ((line = bufferedReader.readLine()) != null) {  

					content.append(line + "\n");  
				}

				bufferedReader.close();

				String releaseInfo = content.toString();
				String version = getVersionTag(releaseInfo);

				if (newer(getVersionTag(releaseInfo), getExistingLauncherVersion(bootstrap.workDir))) {

					bootstrap.println("Found new launcher version: " + version + "!");
					String downloadURL = findLauncherLZMAUrl(releaseInfo);

					return new Object[] {(boolean)(downloadURL != null), version, downloadURL, findMD5(releaseInfo)};

				} else {

					bootstrap.println("Launcher is up to date.");

					return new Object[] {false};
				}
			}

		} catch(Exception e) {

			e.printStackTrace();
		}

		return new Object[] {false};
	}

	public static boolean newer(String newer, String existing) {

		String[] v1 = newer.split("\\.");
		String[] v2 = existing.split("\\.");

		int i = 0;

		if (v1.equals(v2)) return false;

		while(i < v1.length && i < v2.length) {

			int n1 = Integer.parseInt(v1[i]);
			int n2 = Integer.parseInt(v2[i]);

			if(n1 > n2) {

				return true;

			} else if(n1 < n2) {

				return false;
			}

			i++;
		}

		return false;
	}

	private static String getVersionTag(String content) {

		return content.split("\"tag_name\"\\:\"")[1].split("\"")[0];
	}

	private static String findLauncherLZMAUrl(String content) {

		JsonObject release = new JsonParser().parse(content).getAsJsonObject();

		for (JsonElement attachment : (JsonArray)release.getAsJsonArray("assets")) {

			if (attachment instanceof JsonObject) {

				JsonObject file = (JsonObject)attachment;

				if (!file.get("name").getAsString().endsWith(".lzma")) continue;

				return file.get("browser_download_url").getAsString();
			}
		}

		return null;
	}

	public static String getExistingLauncherVersion(File workingDir) {

		try {
			
			File launcher_jar = new File(workingDir, "launcher.jar");

			if (!launcher_jar.exists()) return "0";

			File launcher_profile = new File(workingDir, "launcher_profiles.json");

			if (!launcher_profile.exists()) return "0";

			BufferedReader bufferedReader = new BufferedReader(new FileReader(launcher_profile));  

			StringBuilder content = new StringBuilder();
			String line;  

			while ((line = bufferedReader.readLine()) != null) {  

				content.append(line + "\n");  
			}

			bufferedReader.close();

			return new JsonParser().parse(content.toString()).getAsJsonObject().get("launcherVersion").getAsJsonObject().get("name").getAsString();

		} catch (IOException sad) {}

		return "0";
	}

	private static String findMD5(String content) {

		JsonObject release = new JsonParser().parse(content).getAsJsonObject();

		String description = release.get("body").getAsString();

		return description.contains("MD5") ? description.split("MD5: `")[1].split("`")[0] : "";
	}
}
