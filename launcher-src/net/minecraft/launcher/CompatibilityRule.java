package net.minecraft.launcher;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.launcher.OperatingSystem;

public class CompatibilityRule {

	public enum Action {

		ALLOW, DISALLOW;
	}

	public static interface FeatureMatcher {

		boolean hasFeature(String param1String, Object param1Object);
	}

	public class OSRestriction {

		private OperatingSystem name;

		private String version;

		private String arch;

		public OSRestriction() {}

		public OperatingSystem getName() {

			return this.name;
		}

		public String getVersion() {

			return this.version;
		}

		public String getArch() {

			return this.arch;
		}

		public OSRestriction(OSRestriction osRestriction) {

			this.name = osRestriction.name;
			this.version = osRestriction.version;
			this.arch = osRestriction.arch;
		}

		public boolean isCurrentOperatingSystem() {

			if (this.name != null && this.name != OperatingSystem.getCurrentPlatform()) return false; 

			if (this.version != null)

				try {

					Pattern pattern = Pattern.compile(this.version);
					Matcher matcher = pattern.matcher(System.getProperty("os.version"));

					if (!matcher.matches()) return false; 

				} catch (Throwable throwable) {} 

			if (this.arch != null) {

				try {

					Pattern pattern = Pattern.compile(this.arch);
					Matcher matcher = pattern.matcher(System.getProperty("os.arch"));

					if (!matcher.matches()) return false; 

				} catch (Throwable throwable) {} 
			}
			return true;
		}

		public String toString() {

			return "OSRestriction{name=" + this.name + ", version='" + this.version + '\'' + ", arch='" + this.arch + '\'' + '}';
		}
	}

	private Action action = Action.ALLOW;

	private OSRestriction os;

	private Map<String, Object> features;

	public CompatibilityRule() {}

	public CompatibilityRule(CompatibilityRule compatibilityRule) {

		this.action = compatibilityRule.action;

		if (compatibilityRule.os != null) this.os = new OSRestriction(compatibilityRule.os); 

		if (compatibilityRule.features != null) this.features = compatibilityRule.features; 
	}

	public Action getAppliedAction(FeatureMatcher featureMatcher) {

		if (this.os != null && !this.os.isCurrentOperatingSystem()) return null; 

		if (this.features != null) {

			if (featureMatcher == null) return null; 

			for (Map.Entry<String, Object> feature : this.features.entrySet()) {

				if (!featureMatcher.hasFeature(feature.getKey(), feature.getValue())) return null; 
			}
		}
		return this.action;
	}

	public Action getAction() {

		return this.action;
	}

	public OSRestriction getOs() {

		return this.os;
	}

	public Map<String, Object> getFeatures() {

		return this.features;
	}

	public String toString() {

		return "Rule{action=" + this.action + ", os=" + this.os + ", features=" + this.features + '}';
	}
}
