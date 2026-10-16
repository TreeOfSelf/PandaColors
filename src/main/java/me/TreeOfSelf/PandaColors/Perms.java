package me.TreeOfSelf.PandaColors;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.world.entity.player.Player;

// pandacolors.<feature> permissions, allowed unless a permission provider (like PandaVotes) says otherwise
public final class Perms {
    public static final String CHAT = "pandacolors.chat";
    public static final String BASIC_FORMAT = "pandacolors.basicformat";
    public static final String CHAT_ITEMS = "pandacolors.chatitems";
    public static final String SIGN = "pandacolors.sign";
    public static final String ANVIL = "pandacolors.anvil";
    public static final String BOOK = "pandacolors.book";
    public static final String NAME_COLOR = "pandacolors.namecolor";

    public static boolean can(Player player, String permission) {
        return player == null || Permissions.check(player, permission, true);
    }
}
