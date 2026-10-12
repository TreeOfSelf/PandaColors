package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.ChatFormatter;
import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.commands.MsgCommand;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;

// /msg, /tell, /w (and /r from PandaReply, which runs /msg)
@Mixin(MsgCommand.class)
public class MsgCommandMixin {

    @ModifyVariable(method = "sendMessage", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static PlayerChatMessage pandaColors$formatPrivateMessage(PlayerChatMessage message, CommandSourceStack source, Collection<ServerPlayer> players, PlayerChatMessage messageArg) {
        if (!PandaColorsConfig.get().commandBroadcast) {
            return message;
        }
        return ChatFormatter.format(source.getPlayer(), message);
    }
}
