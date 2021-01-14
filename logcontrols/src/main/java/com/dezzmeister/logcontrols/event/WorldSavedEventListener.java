package com.dezzmeister.logcontrols.event;

import com.dezzmeister.logcontrols.control.CommandLogging;

import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class WorldSavedEventListener {
	
	@SubscribeEvent
	public void receive(final WorldEvent.Save event) {
		CommandLogging.DEFAULT_LOGGING.save();
	}
}
