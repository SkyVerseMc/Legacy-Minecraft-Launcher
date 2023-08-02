package com.mojang.launcher.versions;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.minecraft.launcher.game.MinecraftReleaseType;

public class ReleaseTypeAdapterFactory<T extends MinecraftReleaseType> extends TypeAdapter<T> {

	private final ReleaseTypeFactory<T> factory;

	public ReleaseTypeAdapterFactory(ReleaseTypeFactory<T> factory) {

		this.factory = factory;
	}

	public void write(JsonWriter out, T value) throws IOException {

		out.value(value.getName());
	}

	public T read(JsonReader in) throws IOException {

		return this.factory.getTypeByName(in.nextString());
	}
}
