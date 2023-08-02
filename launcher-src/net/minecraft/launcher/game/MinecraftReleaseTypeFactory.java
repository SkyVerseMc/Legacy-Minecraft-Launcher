package net.minecraft.launcher.game;

import java.util.Iterator;

import com.google.common.collect.Iterators;
import com.mojang.launcher.versions.ReleaseTypeFactory;

public class MinecraftReleaseTypeFactory implements ReleaseTypeFactory<MinecraftReleaseType> {
	
	private static final MinecraftReleaseTypeFactory FACTORY = new MinecraftReleaseTypeFactory();

	public MinecraftReleaseType getTypeByName(String name) {
	
		return MinecraftReleaseType.getByName(name);
	}

	public MinecraftReleaseType[] getAllTypes() {
		
		return MinecraftReleaseType.values();
	}

	public Class<MinecraftReleaseType> getTypeClass() {
		
		return MinecraftReleaseType.class;
	}

	@SuppressWarnings("unchecked")
	public Iterator<MinecraftReleaseType> iterator() {

		return (Iterator<MinecraftReleaseType>) (Iterator<?>)Iterators.forArray((Object[])MinecraftReleaseType.values());
	}

	public static MinecraftReleaseTypeFactory instance() {
		
		return FACTORY;
	}
}
