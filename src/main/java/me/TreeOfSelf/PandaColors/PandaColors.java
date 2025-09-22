package me.TreeOfSelf.PandaColors;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PandaColors implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("panda-colors");

	@Override
	public void onInitialize() {
		LOGGER.info("PandaColors Started!");
	}
}