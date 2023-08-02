package net.minecraft.launcher.ui.tabs.website;

import java.awt.Component;
import java.awt.Dimension;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.logging.log4j.LogManager;

public interface Browser {

	void loadUrl(String paramString);

	Component getComponent();

	void resize(Dimension paramDimension);

//	default String pingAndLoad(String url) {
//
//		try {
//
//			HttpURLConnection urlConnection = (HttpURLConnection) new URL(url).openConnection();
//
//			boolean ok = urlConnection.getResponseCode() == 200;
//
//			if (!ok) {
//
//				LogManager.getLogger().error("Unexpected exception loading " + url, urlConnection.getResponseMessage());
//			}
//
//			return ok ? url : "https://mcupdate.tumblr.com";
//
//		} catch(Exception e) {
//
//			e.printStackTrace();
//		}
//
//		return "https://mcupdate.tumblr.com";
//	}
}
