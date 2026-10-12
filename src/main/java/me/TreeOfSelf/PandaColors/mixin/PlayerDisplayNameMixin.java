package me.TreeOfSelf.PandaColors.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.TreeOfSelf.PandaColors.NameColors;
import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.URI;

// /namecolor: the colored name replaces the plain one inside the display name (chat, tab list via %player:displayname%)
@Mixin(Player.class)
public class PlayerDisplayNameMixin {

    // Profile link on the name instead of vanilla's suggested /tell
    @ModifyReturnValue(method = "getDisplayName", at = @At("RETURN"))
    private Component pandaColors$profileLink(Component name) {
        String url = PandaColorsConfig.get().profileUrl;
        Player player = (Player) (Object) this;
        if (url == null || url.isEmpty() || !(player instanceof ServerPlayer)) return name;
        try {
            URI uri = URI.create(url.replace("%name%", player.getGameProfile().name()));
            return name.copy().withStyle(s -> s.withClickEvent(new ClickEvent.OpenUrl(uri)));
        } catch (IllegalArgumentException e) {
            return name;
        }
    }

    @WrapOperation(method = "getDisplayName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getName()Lnet/minecraft/network/chat/Component;"))
    private Component pandaColors$coloredName(Player player, Operation<Component> original) {
        Component name = original.call(player);
        if (!PandaColorsConfig.get().nameColor || !(player instanceof ServerPlayer)) return name;
        Component colored = NameColors.get(player.getUUID(), name.getString());
        return colored != null ? colored : name;
    }
}
