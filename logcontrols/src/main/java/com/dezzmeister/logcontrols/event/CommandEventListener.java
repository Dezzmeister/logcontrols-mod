package com.dezzmeister.logcontrols.event;

import java.lang.reflect.Field;

import com.dezzmeister.logcontrols.control.CommandLogging;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

import net.minecraft.command.CommandSource;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class CommandEventListener {
	private static Field feedbackDisabledField;
	
	public static void init() {
		try {
			feedbackDisabledField = ObfuscationReflectionHelper.findField(CommandSource.class, "field_197048_j");
			feedbackDisabledField.setAccessible(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@SubscribeEvent
	public void receive(final CommandEvent event) {
		if (CommandLogging.DEFAULT_LOGGING == null) {
			return;
		}
		
		final ParseResults<CommandSource> parseResults = event.getParseResults();
		final CommandContextBuilder<CommandSource> context = parseResults.getContext();
		final CommandNode<CommandSource> rootNode = context.getNodes().get(0).getNode();
		final CommandSource source = context.getSource();
		
		if (rootNode instanceof LiteralCommandNode) {
			final LiteralCommandNode<CommandSource> rootLiteralNode = (LiteralCommandNode<CommandSource>) rootNode;
			final String literal = rootLiteralNode.getLiteral();
			
			try {
				feedbackDisabledField.set(source, !CommandLogging.DEFAULT_LOGGING.isLogged(literal));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
