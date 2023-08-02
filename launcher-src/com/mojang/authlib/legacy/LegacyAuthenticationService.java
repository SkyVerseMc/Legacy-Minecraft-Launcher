package com.mojang.authlib.legacy;

import java.net.Proxy;

import org.apache.commons.lang3.Validate;

import com.mojang.authlib.Agent;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.HttpAuthenticationService;

public class LegacyAuthenticationService extends HttpAuthenticationService {

	protected LegacyAuthenticationService(Proxy proxy) {

		super(proxy);
	}

	public LegacyUserAuthentication createUserAuthentication(Agent agent) {

		Validate.notNull(agent);

		if (agent != Agent.MINECRAFT) throw new IllegalArgumentException("Legacy authentication cannot handle anything but Minecraft");

		return new LegacyUserAuthentication(this);
	}

	public LegacyMinecraftSessionService createMinecraftSessionService() {

		return new LegacyMinecraftSessionService(this);
	}

	public GameProfileRepository createProfileRepository() {

		throw new UnsupportedOperationException("Legacy authentication service has no profile repository");
	}
}
