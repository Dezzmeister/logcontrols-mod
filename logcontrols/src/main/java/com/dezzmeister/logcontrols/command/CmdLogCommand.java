package com.dezzmeister.logcontrols.command;

import com.dezzmeister.logcontrols.control.CommandLogging;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;

public class CmdLogCommand {

	public static void register(final CommandDispatcher<CommandSource> dispatcher) {
		final LiteralArgumentBuilder<CommandSource> commandSource = Commands.literal("cmdlog")
				.then(getKnownCommandBranches(true)).then(getKnownCommandBranches(false))
				.then(Commands.literal("reset").then(Commands.argument("enableLogging", BoolArgumentType.bool())
						.executes(context -> resetLogging(context))));

		dispatcher.register(commandSource);
	}

	private static final LiteralArgumentBuilder<CommandSource> getKnownCommandBranches(final boolean get) {
		final LiteralArgumentBuilder<CommandSource> root = Commands.literal(get ? "get" : "set");

		for (final String cmd : CommandLogging.DEFAULT_LOGGING.knownCommands) {
			if (get) {
				root.then(Commands.literal(cmd).executes(context -> getLoggingFor(context, cmd)));
			} else {
				root.then(Commands.literal(cmd).then(Commands.argument("enableLogging", BoolArgumentType.bool())
						.executes(context -> setLoggingFor(context, cmd))));
			}
		}

		return root;
	}
	
	private static final int setLoggingFor(final CommandContext<CommandSource> context, final String literal) {
		final boolean shouldEnableLogging = BoolArgumentType.getBool(context, "enableLogging");
		
		CommandLogging.DEFAULT_LOGGING.setLogged(literal, shouldEnableLogging);
		
		return 1;
	}

	private static final int getLoggingFor(final CommandContext<CommandSource> context, final String commandLiteral) throws CommandSyntaxException {
		final boolean isLogged = CommandLogging.DEFAULT_LOGGING.isLogged(commandLiteral);
		final ServerPlayerEntity source = context.getSource().asPlayer();
		
		source.sendMessage(new StringTextComponent(isLogged + ""), Util.field_240973_b_);

		return 1;
	}

	private static final int resetLogging(final CommandContext<CommandSource> context) {
		final boolean shouldEnableLogging = BoolArgumentType.getBool(context, "enableLogging");

		CommandLogging.DEFAULT_LOGGING.reset(shouldEnableLogging);

		return 1;
	}
}
