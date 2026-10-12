package me.TreeOfSelf.PandaColors;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class PandaColors implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("panda-colors");

	@Override
	public void onInitialize() {
		PandaColorsConfig.load();
		ServerLifecycleEvents.SERVER_STARTED.register(NameColors::load);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("pandacolors")
				.then(literal("view").then(argument("id", StringArgumentType.word()).executes(ctx -> {
					if (!ChatItems.open(ctx.getSource().getPlayerOrException(), StringArgumentType.getString(ctx, "id"))) {
						ctx.getSource().sendFailure(Component.literal("That link has expired."));
						return 0;
					}
					return 1;
				}))));

			dispatcher.register(literal("namecolor")
				.requires(source -> PandaColorsConfig.get().nameColor)
				.executes(ctx -> {
					ctx.getSource().sendSystemMessage(Component.literal("Usage: /namecolor <format>, e.g. /namecolor <gr:red:gold> or /namecolor <aqua><bold>. /namecolor clear removes it.").withStyle(ChatFormatting.GRAY));
					return 1;
				})
				.then(literal("clear").executes(ctx -> {
					NameColors.set(ctx.getSource().getPlayerOrException().getUUID(), null);
					ctx.getSource().sendSystemMessage(Component.literal("Name color removed.").withStyle(ChatFormatting.GREEN));
					return 1;
				}))
				.then(argument("format", StringArgumentType.greedyString()).executes(ctx -> {
					ServerPlayer player = ctx.getSource().getPlayerOrException();
					if (!Perms.can(player, Perms.NAME_COLOR)) {
						ctx.getSource().sendFailure(Component.literal("You don't have the Colored Name perk. Try /vote"));
						return 0;
					}
					String format = StringArgumentType.getString(ctx, "format").trim();
					Component name = NameColors.build(format, player.getPlainTextName());
					if (name == null) {
						ctx.getSource().sendFailure(Component.literal("That format changes the text of your name. Use only colors and styles, e.g. <gr:red:gold> or &c&l"));
						return 0;
					}
					NameColors.set(player.getUUID(), format);
					ctx.getSource().sendSystemMessage(Component.literal("Your name now looks like: ").withStyle(ChatFormatting.GREEN).append(name));
					return 1;
				})));
		});
		LOGGER.info("PandaColors Started!");
	}
}
