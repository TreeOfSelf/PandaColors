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
            return message.withUnsignedContent(ChatItems.build(sender, text, part -> formatPart(sender, part, cfg.chat, colors)));
        }
        if (!cfg.chat) {
            return message;
        }
        if (!colors) {
            return message.withUnsignedContent(plain(sender, text));
        }
        return message.withUnsignedContent(TextFormattingHelper.formatStyledInput(text));
    }

    // Styles and markdown (pandacolors.basicformat, everyone by default); colors need pandacolors.chat
    private static Component plain(ServerPlayer sender, String text) {
        if (sender == null || Perms.can(sender, Perms.BASIC_FORMAT)) {
            return TextFormattingHelper.formatBasicMarkdown(text);
        }
        return Component.literal(TextFormattingHelper.stripFormatting(text));
    }

    private static Component formatPart(ServerPlayer sender, String part, boolean enabled, boolean colors) {
        if (!enabled) return Component.literal(part);
        return colors ? TextFormattingHelper.formatStyledInput(part) : plain(sender, part);
    }
}
