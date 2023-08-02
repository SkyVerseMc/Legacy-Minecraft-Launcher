package com.mojang.launcher.versions;

import net.minecraft.launcher.game.MinecraftReleaseType;

public interface ReleaseTypeFactory<T extends MinecraftReleaseType> extends Iterable<T> {

	T getTypeByName(String paramString);

	T[] getAllTypes();

	Class<T> getTypeClass();
}
