package me.TreeOfSelf.PandaColors.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.Stats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

// firstJoinMessage: a player with no play time yet gets it instead of "joined the game"
@Mixin(PlayerList.class)
public class FirstJoinMessageMixin {

    @ModifyArg(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"), index = 0)
    private Component pandaColors$firstJoin(Component message, @Local(argsOnly = true) ServerPlayer player) {
        String template = PandaColorsConfig.get().firstJoinMessage;
        if (template == null || template.isEmpty() || player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)) > 0) return message;
        MutableComponent out = Component.empty().withStyle(ChatFormatting.YELLOW);
        String[] parts = template.split("%player%", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) out.append(player.getDisplayName());
            if (!parts[i].isEmpty()) out.append(TextFormattingHelper.formatStyledInput(parts[i]));
        }
        return out;
    }
}
