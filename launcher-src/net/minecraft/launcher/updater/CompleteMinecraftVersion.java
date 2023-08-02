package net.minecraft.launcher.updater;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.authlib.UserAuthentication;
import com.mojang.launcher.OperatingSystem;
import com.mojang.launcher.game.process.GameProcessBuilder;
import com.mojang.launcher.updater.VersionSyncInfo;
import com.mojang.launcher.updater.download.Downloadable;
import com.mojang.launcher.versions.Version;

import net.minecraft.launcher.CompatibilityRule;
import net.minecraft.launcher.CurrentLaunchFeatureMatcher;
import net.minecraft.launcher.Launcher;
import net.minecraft.launcher.game.MinecraftReleaseType;
import net.minecraft.launcher.profile.ProfileManager;

public class CompleteMinecraftVersion implements Version {

	private String inheritsFrom;

	private String id;
	private Date time;
	private Date releaseTime;
	private MinecraftReleaseType type;
	private String sha1;
	
	private String minecraftArguments;

	private List<Library> libraries;

	private String mainClass;

	private int minimumLauncherVersion;

	private String incompatibilityReason;

	private String assets;

	private List<CompatibilityRule> compatibilityRules;

	private String jar;

	private CompleteMinecraftVersion savableVersion;

	private transient boolean synced = false;

	private Map<DownloadType, DownloadInfo> downloads = Maps.newEnumMap(DownloadType.class);

	private AssetIndexInfo assetIndex;

	private Map<ArgumentType, List<Argument>> arguments;

	public CompleteMinecraftVersion() {}

	public CompleteMinecraftVersion(CompleteMinecraftVersion version) {

		this.inheritsFrom = version.inheritsFrom;
		this.id = version.id;
		this.time = version.time;
		this.releaseTime = version.releaseTime;
		this.type = version.type;
		this.minecraftArguments = version.minecraftArguments;
		this.mainClass = version.mainClass;
		this.minimumLauncherVersion = version.minimumLauncherVersion;
		this.incompatibilityReason = version.incompatibilityReason;
		this.assets = version.assets;
		this.jar = version.jar;
		this.downloads = version.downloads;

		if (version.libraries != null) {

			this.libraries = Lists.newArrayList();

			for (Library library : version.getLibraries()) {

				this.libraries.add(new Library(library)); 
			}
		} 
		if (version.arguments != null) {

			this.arguments = Maps.newEnumMap(ArgumentType.class);

			for (Map.Entry<ArgumentType, List<Argument>> entry : version.arguments.entrySet()) {

				this.arguments.put(entry.getKey(), new ArrayList<Argument>(entry.getValue()));
			}
		} 
		if (version.compatibilityRules != null) {

			this.compatibilityRules = Lists.newArrayList();

			for (CompatibilityRule compatibilityRule : version.compatibilityRules) {

				this.compatibilityRules.add(new CompatibilityRule(compatibilityRule)); 
			}
		} 
	}

	public String getId() {

		return this.id;
	}

	public MinecraftReleaseType getType() {

		return this.type;
	}

	public Date getUpdatedTime() {

		return this.time;
	}

	public Date getReleaseTime() {

		return this.releaseTime;
	}

	public List<Library> getLibraries() {

		return this.libraries;
	}

	public String getMainClass() {

		return this.mainClass;
	}

	public String getJar() {

		return (this.jar == null) ? this.id : this.jar;
	}

	public Collection<Library> getRelevantLibraries(CompatibilityRule.FeatureMatcher featureMatcher) {

		List<Library> result = new ArrayList<Library>();

		for (Library library : this.libraries) {

			if (library.appliesToCurrentEnvironment(featureMatcher)) {

				result.add(library); 
			}
		} 
		return result;
	}

	public Collection<File> getClassPath(OperatingSystem os, File base, CompatibilityRule.FeatureMatcher featureMatcher) {

		Collection<Library> libraries = getRelevantLibraries(featureMatcher);
		Collection<File> result = new ArrayList<File>();

		for (Library library : libraries) {

			if (library.getNatives() == null) result.add(new File(base, "libraries/" + library.getLibraryPath()));
		} 

		result.add(new File(base, "versions/" + getJar() + "/" + getJar() + ".jar"));

		return result;
	}

	public Set<String> getRequiredFiles(OperatingSystem os) {

		Set<String> neededFiles = new HashSet<String>();

		for (Library library : getRelevantLibraries(createFeatureMatcher())) {

			if (library.getNatives() != null) {

				String natives = library.getNatives().get(os);

				if (natives != null) {

					neededFiles.add("libraries/" + library.getArtifactPath(natives)); 
				}
				continue;
			} 
			neededFiles.add("libraries/" + library.getArtifactPath());
		} 
		return neededFiles;
	}

	public Set<Downloadable> getRequiredDownloadables(OperatingSystem os, Proxy proxy, File targetDirectory, boolean ignoreLocalFiles) throws MalformedURLException {

		Set<Downloadable> neededFiles = new HashSet<Downloadable>();

		for (Library library : getRelevantLibraries(createFeatureMatcher())) {

			String file = null;
			String classifier = null;

			if (library.getNatives() != null) {

				classifier = library.getNatives().get(os);

				if (classifier != null) file = library.getArtifactPath(classifier); 

			} else {

				file = library.getArtifactPath();
			} 
			if (file != null) {

				File local = new File(targetDirectory, "libraries/" + file);
				Downloadable download = library.createDownload(proxy, file, local, ignoreLocalFiles, classifier);

				if (download != null) neededFiles.add(download); 
			} 
		} 
		return neededFiles;
	}

	public String toString() {

		return "CompleteVersion{id='" + this.id + '\'' + ", updatedTime=" + this.time + ", releasedTime=" + this.time + ", type=" + this.type + ", libraries=" + this.libraries + ", mainClass='" + this.mainClass + '\'' + ", jar='" + this.jar + '\'' + ", minimumLauncherVersion=" + this.minimumLauncherVersion + '}';
	}

	public String getMinecraftArguments() {

		return this.minecraftArguments;
	}

	public int getMinimumLauncherVersion() {

		return this.minimumLauncherVersion;
	}

	public boolean appliesToCurrentEnvironment() {

		if (this.compatibilityRules == null) return true;

		CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;

		for (CompatibilityRule compatibilityRule : this.compatibilityRules) {

			ProfileManager profileManager = Launcher.getCurrentInstance().getProfileManager();

			UserAuthentication auth = profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());

			CompatibilityRule.Action action = compatibilityRule.getAppliedAction((CompatibilityRule.FeatureMatcher)new CurrentLaunchFeatureMatcher(profileManager.getSelectedProfile(), this, auth));

			if (action != null) lastAction = action; 
		} 
		return (lastAction == CompatibilityRule.Action.ALLOW);
	}

	public String getIncompatibilityReason() {

		return this.incompatibilityReason;
	}

	public boolean isSynced() {

		return this.synced;
	}

	public void setSynced(boolean synced) {

		this.synced = synced;
	}

	public String getInheritsFrom() {

		return this.inheritsFrom;
	}

	public CompleteMinecraftVersion resolve(MinecraftVersionManager versionManager) throws IOException {

		return resolve(versionManager, Sets.newHashSet());
	}

	protected CompleteMinecraftVersion resolve(MinecraftVersionManager versionManager, Set<String> resolvedSoFar) throws IOException {

		if (this.inheritsFrom == null) return this; 

		if (!resolvedSoFar.add(this.id)) throw new IllegalStateException("Circular dependency detected"); 

		VersionSyncInfo parentSync = versionManager.getVersionSyncInfo(this.inheritsFrom);
		CompleteMinecraftVersion parent = ((CompleteMinecraftVersion) versionManager.getLatestVersion(parentSync)).resolve(versionManager, resolvedSoFar);
		CompleteMinecraftVersion result = new CompleteMinecraftVersion(parent);

		if (!parentSync.isInstalled() || !parentSync.isUpToDate() || parentSync.getLatestSource() != VersionSyncInfo.VersionSource.LOCAL) {

			versionManager.installVersion(parent); 
		}
		result.savableVersion = this;
		result.inheritsFrom = null;
		result.id = this.id;
		result.time = this.time;
		result.releaseTime = this.releaseTime;
		result.type = this.type;

		if (this.minecraftArguments != null) result.minecraftArguments = this.minecraftArguments; 

		if (this.mainClass != null) result.mainClass = this.mainClass; 

		if (this.incompatibilityReason != null) result.incompatibilityReason = this.incompatibilityReason; 

		if (this.assets != null) result.assets = this.assets; 

		if (this.jar != null) result.jar = this.jar; 

		if (this.libraries != null) {

			List<Library> newLibraries = Lists.newArrayList();

			for (Library library : this.libraries) {

				newLibraries.add(new Library(library)); 
			}
			for (Library library : result.libraries) {

				newLibraries.add(library); 
			}
			result.libraries = newLibraries;
		} 
		if (this.arguments != null) {

			if (result.arguments == null) result.arguments = new EnumMap<ArgumentType, List<Argument>>(ArgumentType.class); 

			for (Map.Entry<ArgumentType, List<Argument>> entry : this.arguments.entrySet()) {

				List<Argument> arguments = result.arguments.get(entry.getKey());

				if (arguments == null) {

					arguments = new ArrayList<Argument>();
					result.arguments.put(entry.getKey(), arguments);
				}
				arguments.addAll(entry.getValue());
			} 
		} 

		if (this.compatibilityRules != null) {

			for (CompatibilityRule compatibilityRule : this.compatibilityRules) {

				result.compatibilityRules.add(new CompatibilityRule(compatibilityRule));  
			}
		}
		return result;
	}

	public CompleteMinecraftVersion getSavableVersion() {

		return (CompleteMinecraftVersion)Objects.firstNonNull(this.savableVersion, this);
	}

	public AbstractDownloadInfo getDownloadURL(DownloadType type) {

		return this.downloads.get(type);
	}

	public AssetIndexInfo getAssetIndex() {

		if (this.assetIndex == null) this.assetIndex = new AssetIndexInfo((String)Objects.firstNonNull(this.assets, "legacy")); 

		return this.assetIndex;
	}

	public CompatibilityRule.FeatureMatcher createFeatureMatcher() {

		ProfileManager profileManager = Launcher.getCurrentInstance().getProfileManager();
		UserAuthentication auth = profileManager.getAuthDatabase().getByUUID(profileManager.getSelectedUser());

		return (CompatibilityRule.FeatureMatcher) new CurrentLaunchFeatureMatcher(profileManager.getSelectedProfile(), this, auth);
	}

	public void addArguments(ArgumentType type, CompatibilityRule.FeatureMatcher featureMatcher, GameProcessBuilder builder, StrSubstitutor substitutor) {

		if (this.arguments != null) {

			List<Argument> args = this.arguments.get(type);

			if (args != null) {

				for (Argument argument : args) {

					argument.apply(builder, featureMatcher, substitutor);  
				}
			}

		} else if (this.minecraftArguments != null) {

			if (type == ArgumentType.GAME) {

				for (String arg : this.minecraftArguments.split(" ")) {

					builder.withArguments(new String[] { substitutor.replace(arg) });
				} 
				if (featureMatcher.hasFeature("is_demo_user", Boolean.valueOf(true))) builder.withArguments(new String[] { "--demo" }); 

				if (featureMatcher.hasFeature("has_custom_resolution", Boolean.valueOf(true))) builder.withArguments(new String[] { "--width", substitutor.replace("${resolution_width}"), "--height", substitutor.replace("${resolution_height}") }); 

			} else if (type == ArgumentType.JVM) {

				if (OperatingSystem.getCurrentPlatform() == OperatingSystem.WINDOWS) {

					builder.withArguments(new String[] { "-XX:HeapDumpPath=MojangTricksIntelDriversForPerformance_javaw.exe_minecraft.exe.heapdump" });

					if (Launcher.getCurrentInstance().usesWinTenHack()) builder.withArguments(new String[] { "-Dos.name=Windows 10", "-Dos.version=10.0" }); 

				} else if (OperatingSystem.getCurrentPlatform() == OperatingSystem.OSX) {

					builder.withArguments(new String[] { substitutor.replace("-Xdock:icon=${asset=icons/minecraft.icns}"), "-Xdock:name=Minecraft" });
				} 
				builder.withArguments(new String[] { substitutor.replace("-Djava.library.path=${natives_directory}") });
				builder.withArguments(new String[] { substitutor.replace("-Dminecraft.launcher.brand=${launcher_name}") });
				builder.withArguments(new String[] { substitutor.replace("-Dminecraft.launcher.version=${launcher_version}") });
				builder.withArguments(new String[] { substitutor.replace("-Dminecraft.client.jar=${primary_jar}") });
				builder.withArguments(new String[] { "-cp", substitutor.replace("${classpath}") });
			} 
		} 
	}

	public String getURL() {

		return "https://piston-meta.mojang.com/v1/packages/" + this.sha1 + "/" + this.id;
	}

	public String getSha1() {
		
		return this.sha1;
	}

	public int getComplianceLevel() {
		
		return 0;
	}

	@Override
	public void setType(MinecraftReleaseType type) {
		
		this.type = type;
	}
}
