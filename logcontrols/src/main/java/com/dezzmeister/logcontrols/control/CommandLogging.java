package com.dezzmeister.logcontrols.control;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.IWorldInfo;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Manages the command logging config as well as the set of commands that are logged in the server. {@link #save()}
 * is called on every world save to update the mappings file.
 * 
 * @author Joe Desmond
 */
public class CommandLogging {
	
	/**
	 * Default instance. There should only be one CommandLogging object
	 */
	public static CommandLogging DEFAULT_LOGGING = null;
	private static final Logger LOGGER = LogManager.getLogger();
	
	private final File logFile;
	private final Map<String, Boolean> commands;
	public final List<String> knownCommands;
	private final boolean defaultLogging;
	
	/**
	 * Instantiates {@link #DEFAULT_LOGGING}. If the file does not exist, generates a new file with command mappings all set
	 * to 'defaultLogging'.
	 * 
	 * @param server Minecraft server
	 * @param fileName file containing mappings
	 * @param defaultLogging true if unknown/unmapped commands should be logged
	 */
	public static void init(final MinecraftServer server, final String fileName, final boolean defaultLogging) {
		File dir = null;
		
		if (FMLEnvironment.dist.isDedicatedServer()) {
			dir = server.getDataDirectory();
		} else {
			for (final World world : server.getWorlds()) {
				if (world instanceof ServerWorld) {
					final ServerWorld serverWorld = (ServerWorld) world;
					final IWorldInfo worldInfo = serverWorld.getWorldInfo();
					
					if (worldInfo instanceof IServerWorldInfo) {
						final IServerWorldInfo serverWorldInfo = (IServerWorldInfo) worldInfo;						
						dir = new File(new File(server.getDataDirectory(), "saves"), serverWorldInfo.getWorldName());
						break;
					}
				}
			}
		}
		
		if (dir != null && dir.exists()) {
			DEFAULT_LOGGING = new CommandLogging(server, new File(dir, fileName), defaultLogging);
		} else {
			LOGGER.error("Unable to save " + new File(dir, fileName).getAbsolutePath() + ". Using temporary logging mappings");
			DEFAULT_LOGGING = new CommandLogging(server, null, defaultLogging);
		}
	}
	
	/**
	 * Private to ensure that there is only one instance.
	 * 
	 * @param _server Minecraft server
	 * @param _logFile file containing command logging entries
	 * @param _defaultLogging default logging value if a command does not have an entry
	 */
	private CommandLogging(final MinecraftServer _server, final File _logFile, final boolean _defaultLogging) {
		logFile = _logFile;
		defaultLogging = _defaultLogging;
		commands = getOrRebuild(_server, _logFile, _defaultLogging);
		knownCommands = getKnownCommands(commands);
	}
	
	private List<String> getKnownCommands(final Map<String, ?> commands) {
		final List<String> out = new ArrayList<String>();
		
		for (Entry<String, ?> entry : commands.entrySet()) {
			out.add(entry.getKey());
		}
		
		return out;
	}
	
	public boolean isLogged(final String command) {
		if (commands.containsKey(command)) {
			return commands.get(command);
		}
		
		return defaultLogging;
	}
	
	public void setLogged(final String command, final boolean isLogged) {
		commands.put(command, isLogged);
	}
	
	/**
	 * Resets every mapping to 'resetTo'.
	 * 
	 * @param resetTo true if every command should be logged, false if none should be logged
	 */
	public void reset(final boolean resetTo) {
		for (Entry<String, Boolean> entry : commands.entrySet()) {
			final String literal = entry.getKey();
			commands.put(literal, resetTo);
		}
	}
	
	private final Map<String, Boolean> getOrRebuild(final MinecraftServer server, final File entryFile, boolean defaultLogging) {
		if (entryFile.exists()) {
			try {
				return parse(entryFile);
			} catch (IOException e) {
				e.printStackTrace();
				return new HashMap<String, Boolean>();
			}
		} else {
			LOGGER.info(entryFile.getName() + " does not exist. Generating one...");
			return rebuild(server, entryFile, defaultLogging);
		}
	}
	
	private final Map<String, Boolean> rebuild(final MinecraftServer server, final File file, final boolean defaultLogging) {
		final Map<String, Boolean> out = new HashMap<String, Boolean>();
		final List<String> lines = new ArrayList<String>();
		final CommandDispatcher<CommandSource> dispatcher = server.getCommandManager().getDispatcher();
		final RootCommandNode<CommandSource> rootNode = dispatcher.getRoot();
		final Collection<CommandNode<CommandSource>> children = rootNode.getChildren();
		
		lines.add("#Automatically generated on " + (new SimpleDateFormat("MMM dd YYYY, hh:mm a").format(new Date())));
		lines.add("#Format: [command]=(true|false)");
		lines.add("#Setting a command to 'false' will disable logging for the command");
		
		for (final CommandNode<CommandSource> node : children) {
			if (node instanceof LiteralCommandNode) {
				final String literal = ((LiteralCommandNode<CommandSource>) node).getLiteral();
				lines.add(literal + "=" + defaultLogging);
				out.put(literal, defaultLogging);
			}
		}
		
		lines.add("cmdlog" + "=" + defaultLogging);
		out.put("cmdlog", defaultLogging);
		Collections.sort(lines);
		
		if (file == null) {
			return out;
		}
		
		try (final PrintWriter pw = new PrintWriter(file)) {
			for (final String line : lines) {
				pw.println(line);
			}
			
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return out;		
	}
	
	public final void save() {
		if (logFile == null) {
			return;
		}
		
		final List<String> lines = new ArrayList<String>();
		
		lines.add("#Last saved on " + (new SimpleDateFormat("MMM dd YYYY, hh:mm:ss a").format(new Date())));
		lines.add("#Format: [command]=(true|false)");
		lines.add("#Setting a command to 'false' will disable logging for the command");
		
		for (Entry<String, Boolean> entry : commands.entrySet()) {
			lines.add(entry.getKey() + "=" + entry.getValue());
		}
		
		Collections.sort(lines);
		
		try (final PrintWriter pw = new PrintWriter(logFile)) {
			for (final String line : lines) {
				pw.println(line);
			}
			
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private final Map<String, Boolean> parse(final File file) throws IOException {
		final Map<String, Boolean> out = new HashMap<String, Boolean>();
		final List<String> lines = Files.readAllLines(Paths.get(file.toURI()));
		final List<String> errors = new ArrayList<String>();
		
		for (int i = 0; i < lines.size(); i++) {
			final String line = lines.get(i).trim();
			
			if (line.startsWith("#") || line.isEmpty()) {
				continue;
			}
			
			if (!line.contains("=")) {
				errors.add("Unreadable line at line " + i);
				continue;
			}
			
			final String[] tokens = line.split("=");
			if (tokens.length != 2) {
				errors.add("Two or more instances of '=' at line " + i);
				continue;
			}
			
			final String commandName = tokens[0];
			final String potentialBoolean = tokens[1].toLowerCase();
			final boolean isLogged;
			
			if (potentialBoolean.equals("true")) {
				isLogged = true;
			} else if (potentialBoolean.equals("false")) {
				isLogged = false;
			} else {
				errors.add("Invalid boolean at line " + i + ", char " + (line.indexOf("=") + 1));
				continue;
			}
			
			out.put(commandName, isLogged);
		}
		
		if (!errors.isEmpty()) {
			LOGGER.error("Error parsing log controls file \"" + file.getName() + "\":");
			
			for (final String error : errors) {
				LOGGER.error("\t" + error);
			}
		}
		
		return out;
	}
}
