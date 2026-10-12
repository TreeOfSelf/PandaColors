package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.ChatFormatter;
import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(PlayerList.class)
public class PlayerListMixin {

    @ModifyVariable(
            method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Lnet/minecraft/commands/CommandSourceStack;Lnet/minecraft/network/chat/ChatType$Bound;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0)
    private PlayerChatMessage pandaColors$formatCommandBroadcast(PlayerChatMessage message, PlayerChatMessage messageArg, CommandSourceStack source, ChatType.Bound bound) {
        if (!PandaColorsConfig.get().commandBroadcast) {
            return message;
        }
        return ChatFormatter.format(source.getPlayer(), message);
    }
}
