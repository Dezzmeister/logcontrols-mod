package com.dezzmeister.logcontrols;

import com.dezzmeister.logcontrols.command.CmdLogCommand;
import com.dezzmeister.logcontrols.control.CommandLogging;
import com.dezzmeister.logcontrols.event.CommandEventListener;
import com.dezzmeister.logcontrols.event.WorldSavedEventListener;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;


@Mod("logcontrols")
public class LogControlsMod {

    public LogControlsMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new CommandEventListener());
        MinecraftForge.EVENT_BUS.register(new WorldSavedEventListener());
    }

    
    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        CommandEventListener.init();
    }
    
    @SubscribeEvent
	public void onServerStarted(FMLServerStartedEvent event) {
		CommandLogging.init(event.getServer(), "command-logging.properties", true);
		CmdLogCommand.register(event.getServer().getCommandManager().getDispatcher());
	}
}
