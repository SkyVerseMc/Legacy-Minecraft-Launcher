package com.mojang.launcher.events;

import com.mojang.launcher.game.process.GameProcess;

public interface GameOutputLogProcessor {

	void onGameOutput(GameProcess paramGameProcess, String paramString);
}
