package com.mojang.launcher.game.runner;

import com.mojang.launcher.game.GameInstanceStatus;

public interface GameRunnerListener {

	void onGameInstanceChangedState(GameRunner paramGameRunner, GameInstanceStatus paramGameInstanceStatus);
}
