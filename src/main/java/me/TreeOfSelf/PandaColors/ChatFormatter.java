package me.TreeOfSelf.PandaColors;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;

// Formats a chat message (chat, /say, /me, /msg, /r) the way the sender's permissions allow:
// formatting with pandacolors.chat, stripped without it; [item]/[inv]/[echest] with pandacolors.chatitems
public final class ChatFormatter {

    public static PlayerChatMessage format(ServerPlayer sender, PlayerChatMessage message) {
        PandaColorsConfig cfg = PandaColorsConfig.get();
        boolean colors = cfg.chat && Perms.can(sender, Perms.CHAT);
        String text = message.signedContent();
        if (sender != null && cfg.chatItems && Perms.can(sender, Perms.CHAT_ITEMS) && ChatItems.hasTag(text)) {
            return message.withUnsignedContent(ChatItems.build(sender, text, part -> formatPart(part, cfg.chat, colors)));
        }
        if (!cfg.chat) {
            return message;
        }
        if (!colors) {
            String stripped = TextFormattingHelper.stripFormatting(text);
            return stripped.equals(text) ? message : message.withUnsignedContent(Component.literal(stripped));
        }
        return message.withUnsignedContent(TextFormattingHelper.formatStyledInput(text));
    }

    private static Component formatPart(String part, boolean enabled, boolean colors) {
        if (!enabled) return Component.literal(part);
        return colors ? TextFormattingHelper.formatStyledInput(part) : Component.literal(TextFormattingHelper.stripFormatting(part));
    }
}
