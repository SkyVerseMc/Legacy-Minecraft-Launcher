package com.mojang.launcher.game.process;

import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.List;

public interface GameProcess {

	List<String> getStartupArguments();

	Collection<String> getSysOutLines();

	Predicate<String> getSysOutFilter();

	boolean isRunning();

	void setExitRunnable(GameProcessRunnable paramGameProcessRunnable);

	GameProcessRunnable getExitRunnable();

	int getExitCode();

	void stop();
}
