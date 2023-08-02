package net.minecraft.launcher.updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.events.RefreshedVersionsListener;
import com.mojang.launcher.updater.ExceptionalThreadPoolExecutor;
import com.mojang.launcher.updater.VersionFilter;
import com.mojang.launcher.updater.VersionManager;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.DownloadJob;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.updater.download.EtagDownloadable;
import com.mojang.launcher.updater.download.assets.AssetDownloadable;
import com.mojang.launcher.updater.download.assets.AssetIndex;
import com.mojang.launcher.versions.Version;

import net.minecraft.launcher.LauncherConstants;
import net.minecraft.launcher.Main;
import net.minecraft.launcher.game.MinecraftReleaseType;

public class MinecraftVersionManager implements VersionManager {

	private static final Logger LOGGER = LogManager.getLogger();

	private final VersionList localVersionList;

	private final VersionList remoteVersionList;

	private final ThreadPoolExecutor executorService = (ThreadPoolExecutor)new ExceptionalThreadPoolExecutor(4, 8, 30L, TimeUnit.SECONDS);

	private final List<RefreshedVersionsListener> refreshedVersionsListeners = Collections.synchronizedList(new ArrayList<RefreshedVersionsListener>());

	private final Object refreshLock = new Object();

	private boolean isRefreshing;

	private final Gson gson = new Gson();

	public MinecraftVersionManager(VersionList localVersionList, VersionList remoteVersionList) {

		this.localVersionList = localVersionList;
		this.remoteVersionList = remoteVersionList;
	}

	public void refreshVersions() throws IOException {

		synchronized (this.refreshLock) {

			this.isRefreshing = true;
		}

		try {

			LOGGER.info("Refreshing local version list...");
			this.localVersionList.refreshVersions();
			LOGGER.info("Refreshing remote version list...");
			this.remoteVersionList.refreshVersions();

		} catch (IOException ex) {

			synchronized (this.refreshLock) {

				this.isRefreshing = false;
			} 
			throw ex;
		} 

		LOGGER.info("Refresh complete.");

		synchronized (this.refreshLock) {

			this.isRefreshing = false;
		}

		for (RefreshedVersionsListener listener : Lists.newArrayList(this.refreshedVersionsListeners)) {

			listener.onVersionsRefreshed(this); 
		}
	}

	public List<VersionSyncInfo> getVersions() {

		return getVersions(null);
	}

	public List<VersionSyncInfo> getVersions(VersionFilter<? extends MinecraftReleaseType> filter) {

		synchronized (this.refreshLock) {

			if (this.isRefreshing) return new ArrayList<VersionSyncInfo>(); 
		}

		List<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
		Map<String, VersionSyncInfo> lookup = new HashMap<String, VersionSyncInfo>();
		Map<MinecraftReleaseType, Integer> counts = Maps.newEnumMap(MinecraftReleaseType.class);

		for (MinecraftReleaseType type : MinecraftReleaseType.values()) {

			counts.put(type, Integer.valueOf(0)); 
		}
		for (Version version : Lists.newArrayList(this.localVersionList.getVersions())) {

			if (version.getType() == null || version.getUpdatedTime() == null) continue; 

			MinecraftReleaseType type = (MinecraftReleaseType)version.getType();
			if (filter != null && (!filter.getTypes().contains(type) || ((Integer)counts.get(type)).intValue() >= filter.getMaxCount())) continue; 

			VersionSyncInfo syncInfo = getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));

			lookup.put(version.getId(), syncInfo);
			result.add(syncInfo);
		}

		for (Version version : this.remoteVersionList.getVersions()) {

			if (version.getType() == null || version.getUpdatedTime() == null)continue; 

			MinecraftReleaseType type = (MinecraftReleaseType)version.getType();

			if (lookup.containsKey(version.getId()) || (filter != null && (!filter.getTypes().contains(type) || ((Integer)counts.get(type)).intValue() >= filter.getMaxCount()))) continue; 

			VersionSyncInfo syncInfo = getVersionSyncInfo(this.localVersionList.getVersion(version.getId()), version);

			lookup.put(version.getId(), syncInfo);
			result.add(syncInfo);

			if (filter != null) counts.put(type, Integer.valueOf(((Integer)counts.get(type)).intValue() + 1)); 
		} 
		if (result.isEmpty()) {

			for (Version version : this.localVersionList.getVersions()) {

				if (version.getType() == null || version.getUpdatedTime() == null) continue; 

				VersionSyncInfo syncInfo = getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));

				lookup.put(version.getId(), syncInfo);
				result.add(syncInfo);
			}
		}
		
		Collections.sort(result, new Comparator<VersionSyncInfo>() {

			public int compare(VersionSyncInfo a, VersionSyncInfo b) {

				Version aVer = a.getLatestVersion();
				Version bVer = b.getLatestVersion();

				if (aVer.getReleaseTime() != null && bVer.getReleaseTime() != null) return bVer.getReleaseTime().compareTo(aVer.getReleaseTime()); 

				return bVer.getUpdatedTime().compareTo(aVer.getUpdatedTime());
			}
		});

		return result;
	}

	public VersionSyncInfo getVersionSyncInfo(Version version) {

		return getVersionSyncInfo(version.getId());
	}

	public VersionSyncInfo getVersionSyncInfo(String name) {

		return getVersionSyncInfo(this.localVersionList.getVersion(name), this.remoteVersionList.getVersion(name));
	}

	public VersionSyncInfo getVersionSyncInfo(Version localVersion, Version remoteVersion) {

		boolean installed = (localVersion != null);
		boolean upToDate = installed;

		String here = Main.getWorkingDirectory().getAbsolutePath() + "/versions/";
		if (installed && this.isModded(here + localVersion.getId() + "/" + localVersion.getId() + ".jar")) localVersion.setType(MinecraftReleaseType.MODDED);


		CompleteMinecraftVersion resolved = null;

		if (installed && remoteVersion != null) upToDate = !remoteVersion.getUpdatedTime().after(localVersion.getUpdatedTime()); 

		if (localVersion instanceof CompleteMinecraftVersion) {

			try {

				resolved = ((CompleteMinecraftVersion)localVersion).resolve(this);

			} catch (IOException ex) {

				LOGGER.error("Couldn't resolve version " + localVersion.getId(), ex);
				resolved = (CompleteMinecraftVersion)localVersion;
			}
			upToDate &= this.localVersionList.hasAllFiles(resolved, OperatingSystem.getCurrentPlatform());
		}
		return new VersionSyncInfo((Version)resolved, remoteVersion, installed, upToDate);
	}

	private boolean isModded(String jarPath) {

		boolean isModded = false;

		try {

			JarFile jarFile = new JarFile(jarPath);

			boolean isSigned = false;
			Enumeration<JarEntry> entries = jarFile.entries();

			while (entries.hasMoreElements()) {

				JarEntry entry = entries.nextElement();
				String entryName = entry.getName();

				if (entryName.startsWith("META-INF/") && (entryName.endsWith(".RSA") || entryName.endsWith(".SF"))) {

					isSigned = true;
					break;
				}
			}
			if (isSigned) {

				ZipEntry t = jarFile.getEntry("META-INF/MOJANGCS.SF");
				if (t == null) {
					
					jarFile.close();
					return true;
				}
				
				InputStream is = jarFile.getInputStream(t);
				
				BufferedReader br = new BufferedReader(new InputStreamReader(is));
				String line;

				while ((line = br.readLine()) != null) {

					if (line.startsWith("Created-By:")) {

						jarFile.close();

						return false;
					}
				}
			} else {

				jarFile.close();

				return true;
			}
			
			jarFile.close();

		} catch (IOException e) {

			if (!(e instanceof NoSuchFileException)) e.printStackTrace();
		}
		return isModded;  
	}

	public List<VersionSyncInfo> getInstalledVersions() {

		List<VersionSyncInfo> result = new ArrayList<VersionSyncInfo>();
		Collection<Version> versions = Lists.newArrayList(this.localVersionList.getVersions());

		for (Version version : versions) {

			if (version.getType() == null || version.getUpdatedTime() == null) continue;

			VersionSyncInfo syncInfo = getVersionSyncInfo(version, this.remoteVersionList.getVersion(version.getId()));
			result.add(syncInfo);
		} 
		return result;
	}

	public VersionList getRemoteVersionList() {

		return this.remoteVersionList;
	}

	public VersionList getLocalVersionList() {

		return this.localVersionList;
	}

	public Version getLatestVersion(VersionSyncInfo syncInfo) throws IOException {

		if (syncInfo.getLatestSource() == VersionSyncInfo.VersionSource.REMOTE) {

			CompleteMinecraftVersion result = null;
			IOException exception = null;
			try {

				result = this.remoteVersionList.getCompleteVersion(syncInfo.getLatestVersion());
			} catch (IOException e) {

				exception = e;

				try {

					result = this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());

				} catch (IOException iOException) {}
			} 
			if (result != null) return result;

			throw exception;
		} 
		return this.localVersionList.getCompleteVersion(syncInfo.getLatestVersion());
	}

	public DownloadJob downloadVersion(VersionSyncInfo syncInfo, DownloadJob job) throws IOException {

		if (!(this.localVersionList instanceof LocalVersionList)) throw new IllegalArgumentException("Cannot download if local repo isn't a LocalVersionList"); 

		if (!(this.remoteVersionList instanceof RemoteVersionList)) throw new IllegalArgumentException("Cannot download if local repo isn't a RemoteVersionList"); 

		CompleteMinecraftVersion version = (CompleteMinecraftVersion) getLatestVersion(syncInfo);

		File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();
		Proxy proxy = ((RemoteVersionList)this.remoteVersionList).getProxy();

		job.addDownloadables(version.getRequiredDownloadables(OperatingSystem.getCurrentPlatform(), proxy, baseDirectory, false));

		String jarFile = "versions/" + version.getJar() + "/" + version.getJar() + ".jar";

		AbstractDownloadInfo clientInfo = version.getDownloadURL(DownloadType.CLIENT);

		if (clientInfo == null) {

			job.addDownloadables(new Downloadable[] { (Downloadable)new EtagDownloadable(proxy, new URL("https://s3.amazonaws.com/Minecraft.Download/" + jarFile), new File(baseDirectory, jarFile), false) });

		} else {

			job.addDownloadables(new Downloadable[] { new PreHashedDownloadable(proxy, clientInfo.getUrl(), new File(baseDirectory, jarFile), false, clientInfo.getSha1()) });
		}

		return job;
	}

	public DownloadJob downloadResources(DownloadJob job, Version version) throws IOException {

		File baseDirectory = ((LocalVersionList)this.localVersionList).getBaseDirectory();

		job.addDownloadables(getResourceFiles(((RemoteVersionList)this.remoteVersionList).getProxy(), baseDirectory, (CompleteMinecraftVersion)version));

		return job;
	}

	private Set<Downloadable> getResourceFiles(Proxy proxy, File baseDirectory, CompleteMinecraftVersion version) {

		Set<Downloadable> result = new HashSet<Downloadable>();

		InputStream inputStream = null;

		File assets = new File(baseDirectory, "assets");
		File objectsFolder = new File(assets, "objects");
		File indexesFolder = new File(assets, "indexes");

		long start = System.nanoTime();

		AssetIndexInfo indexInfo = version.getAssetIndex();

		File indexFile = new File(indexesFolder, indexInfo.getId() + ".json");

		try {

			URL indexUrl = indexInfo.getUrl();

			inputStream = indexUrl.openConnection(proxy).getInputStream();

			String json = IOUtils.toString(inputStream);

			FileUtils.writeStringToFile(indexFile, json);

			AssetIndex index = (AssetIndex)this.gson.fromJson(json, AssetIndex.class);

			for (Map.Entry<AssetIndex.AssetObject, String> entry : (Iterable<Map.Entry<AssetIndex.AssetObject, String>>)index.getUniqueObjects().entrySet()) {

				AssetIndex.AssetObject object = entry.getKey();

				String filename = object.getHash().substring(0, 2) + "/" + object.getHash();

				File file = new File(objectsFolder, filename);

				if (!file.isFile() || FileUtils.sizeOf(file) != object.getSize()) {

					AssetDownloadable assetDownloadable = new AssetDownloadable(proxy, entry.getValue(), object, LauncherConstants.URL_RESOURCE_BASE, objectsFolder);
					assetDownloadable.setExpectedSize(object.getSize());

					result.add(assetDownloadable);
				} 
			} 
			long end = System.nanoTime();
			long delta = end - start;

			LOGGER.debug("Delta time to compare resources: " + (delta / 1000000L) + " ms ");

		} catch (Exception ex) {

			LOGGER.error("Couldn't download resources", ex);

		} finally {

			IOUtils.closeQuietly(inputStream);
		} 
		return result;
	}

	public ThreadPoolExecutor getExecutorService() {

		return this.executorService;
	}

	public void addRefreshedVersionsListener(RefreshedVersionsListener listener) {

		this.refreshedVersionsListeners.add(listener);
	}

	public void removeRefreshedVersionsListener(RefreshedVersionsListener listener) {

		this.refreshedVersionsListeners.remove(listener);
	}

	public VersionSyncInfo syncVersion(VersionSyncInfo syncInfo) throws IOException {

		Version remoteVersion = getRemoteVersionList().getCompleteVersion(syncInfo.getRemoteVersion());

		getLocalVersionList().removeVersion(syncInfo.getLocalVersion());
		getLocalVersionList().addVersion(remoteVersion);

		((LocalVersionList)getLocalVersionList()).saveVersion(((CompleteMinecraftVersion)remoteVersion).getSavableVersion());

		return getVersionSyncInfo((Version)remoteVersion);
	}

	public void installVersion(Version version) throws IOException {

		if (version instanceof CompleteMinecraftVersion) version = ((CompleteMinecraftVersion)version).getSavableVersion(); 

		VersionList localVersionList = getLocalVersionList();

		if (localVersionList.getVersion(version.getId()) != null) localVersionList.removeVersion(version.getId()); 

		localVersionList.addVersion(version);

		if (localVersionList instanceof LocalVersionList) ((LocalVersionList)localVersionList).saveVersion(version); 

		LOGGER.info("Installed " + version);
	}

	public void uninstallVersion(Version version) throws IOException {

		VersionList localVersionList = getLocalVersionList();

		if (localVersionList instanceof LocalVersionList) {

			localVersionList.uninstallVersion((Version)version);
			LOGGER.info("Uninstalled " + version);
		} 
	}
}
