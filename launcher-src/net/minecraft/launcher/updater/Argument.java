package net.minecraft.launcher.updater;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.text.StrSubstitutor;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.launcher.game.process.GameProcessBuilder;

import net.minecraft.launcher.CompatibilityRule;

public class Argument {

	private final String[] value;

	private final List<CompatibilityRule> compatibilityRules;

	public Argument(String[] values, List<CompatibilityRule> compatibilityRules) {

		this.value = values;
		this.compatibilityRules = compatibilityRules;
	}

	public void apply(GameProcessBuilder output, CompatibilityRule.FeatureMatcher featureMatcher, StrSubstitutor substitutor) {

		if (appliesToCurrentEnvironment(featureMatcher)) {

			for (int i = 0; i < this.value.length; i++) {

				output.withArguments(new String[] { substitutor.replace(this.value[i]) });
			}
		}
	}

	public boolean appliesToCurrentEnvironment(CompatibilityRule.FeatureMatcher featureMatcher) {

		if (this.compatibilityRules == null) return true; 

		CompatibilityRule.Action lastAction = CompatibilityRule.Action.DISALLOW;

		for (CompatibilityRule compatibilityRule : this.compatibilityRules) {

			CompatibilityRule.Action action = compatibilityRule.getAppliedAction(featureMatcher);

			if (action != null) lastAction = action; 
		}
		return (lastAction == CompatibilityRule.Action.ALLOW);
	}

	public static class Serializer implements JsonDeserializer<Argument> {

		public Argument deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

			if (json.isJsonPrimitive()) return new Argument(new String[] { json.getAsString() }, null); 

			if (json.isJsonObject()) {

				String[] values;

				JsonObject obj = json.getAsJsonObject();
				JsonElement value = obj.get("value");

				if (value.isJsonPrimitive()) {

					values = new String[] { value.getAsString() };

				} else {

					JsonArray array = value.getAsJsonArray();
					values = new String[array.size()];

					for (int i = 0; i < array.size(); i++) values[i] = array.get(i).getAsString(); 
				}

				List<CompatibilityRule> rules = new ArrayList<CompatibilityRule>();

				if (obj.has("rules")) {

					JsonArray array = obj.getAsJsonArray("rules");

					for (JsonElement element : array) {

						rules.add((CompatibilityRule)context.deserialize(element, CompatibilityRule.class));
					}
				} 
				return new Argument(values, rules);
			} 
			throw new JsonParseException("Invalid argument, must be object or string");
		}
	}
}
