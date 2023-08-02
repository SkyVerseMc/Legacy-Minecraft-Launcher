package com.mojang.launcher.updater;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

	private final DateFormat enUsFormat = DateFormat.getDateTimeInstance(2, 2, Locale.US);

	private final DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

	public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {

		if (!(json instanceof JsonPrimitive)) throw new JsonParseException("The date should be a string value");

		Date date = deserializeToDate(json.getAsString());

		if (typeOfT == Date.class) return date;

		throw new IllegalArgumentException(getClass() + " cannot deserialize to " + typeOfT);
	}

	public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {

		synchronized (this.iso8601Format) {

			return (JsonElement)new JsonPrimitive(serializeToString(src));
		}
	}

	public Date deserializeToDate(String string) {

		synchronized (this.iso8601Format) {

			try {
				
				return this.iso8601Format.parse(string);
			
			} catch (ParseException e) {
			
				e.printStackTrace();
			}
			
			return null;
		}
	}

	public String serializeToString(Date date) {

		synchronized (this.iso8601Format) {

			String result = this.iso8601Format.format(date);

			return result;
		}
	}
}
