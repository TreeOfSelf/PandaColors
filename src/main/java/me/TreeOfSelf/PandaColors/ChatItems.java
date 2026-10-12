package me.TreeOfSelf.PandaColors;

import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

// [item], [inv] and [echest] in chat. Inventories are snapshotted when sent and open read-only from the chat link.
public final class ChatItems {

    private enum Kind { ITEM, INVENTORY, ENDER_CHEST }

    private record Snapshot(Component title, MenuType<?> menu, List<ItemStack> items) {}

    private static final int MAX_SNAPSHOTS = 500;
    private static final Map<String, Snapshot> SNAPSHOTS = new LinkedHashMap<>(16, 0.75f, false) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Snapshot> eldest) {
            return size() > MAX_SNAPSHOTS;
        }
    };

    // Inventory slots in GUI order: main inventory, hotbar, then helmet, chestplate, leggings, boots, (gap), offhand
    private static final int[] INVENTORY_LAYOUT;

    static {
        List<Integer> layout = new ArrayList<>();
        for (int i = 9; i < 36; i++) layout.add(i);
        for (int i = 0; i < 9; i++) layout.add(i);
        layout.addAll(List.of(39, 38, 37, 36, -1, -1, -1, -1, 40));
        INVENTORY_LAYOUT = layout.stream().mapToInt(Integer::intValue).toArray();
    }

    public static boolean hasTag(String text) {
        return findTag(text.toLowerCase(Locale.ROOT), 0) != null;
    }

    // Splits the message at the tags. Text parts go through `text`, tags become item/inventory links.
    public static MutableComponent build(ServerPlayer player, String message, java.util.function.Function<String, Component> text) {
        MutableComponent out = Component.empty();
        String lower = message.toLowerCase(Locale.ROOT);
        int pos = 0;
        while (true) {
            Match match = findTag(lower, pos);
            if (match == null) break;
            if (match.start > pos) out.append(text.apply(message.substring(pos, match.start)));
            Component link = link(player, match.kind);
            out.append(link != null ? link : Component.literal(message.substring(match.start, match.end)));
            pos = match.end;
        }
        if (pos < message.length()) out.append(text.apply(message.substring(pos)));
        return out;
    }

    private record Match(int start, int end, Kind kind) {}

    private static Match findTag(String lower, int from) {
        PandaColorsConfig cfg = PandaColorsConfig.get();
        Match best = null;
        best = earliest(best, lower, from, cfg.itemTags, Kind.ITEM);
        best = earliest(best, lower, from, cfg.inventoryTags, Kind.INVENTORY);
        best = earliest(best, lower, from, cfg.enderChestTags, Kind.ENDER_CHEST);
        return best;
    }

    private static Match earliest(Match best, String lower, int from, List<String> tags, Kind kind) {
        for (String tag : tags) {
            if (tag == null || tag.isEmpty()) continue;
            int i = lower.indexOf(tag.toLowerCase(Locale.ROOT), from);
            if (i < 0) continue;
            // Earliest wins; on a tie the longer tag wins ([inventory] over [inv] if one were a prefix)
            if (best == null || i < best.start || (i == best.start && i + tag.length() > best.end)) {
                best = new Match(i, i + tag.length(), kind);
            }
        }
        return best;
    }

    private static Component link(ServerPlayer player, Kind kind) {
        String name = player.getPlainTextName();
        return switch (kind) {
            case ITEM -> {
                ItemStack stack = player.getMainHandItem();
                if (stack.isEmpty()) stack = player.getOffhandItem();
                if (stack.isEmpty()) yield null;
                MutableComponent c = Component.empty().append(stack.getDisplayName());
                if (stack.getCount() > 1) c.append(Component.literal(" x" + stack.getCount()).withStyle(ChatFormatting.GRAY));
                yield c;
            }
            case INVENTORY -> {
                List<ItemStack> items = new ArrayList<>();
                for (int slot : INVENTORY_LAYOUT) items.add(slot < 0 ? ItemStack.EMPTY : player.getInventory().getItem(slot).copy());
                yield snapshotLink(Component.literal(name + "'s Inventory"), MenuType.GENERIC_9x5, items);
            }
            case ENDER_CHEST -> {
                Container ender = player.getEnderChestInventory();
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < ender.getContainerSize(); i++) items.add(ender.getItem(i).copy());
                yield snapshotLink(Component.literal(name + "'s Ender Chest"), MenuType.GENERIC_9x3, items);
            }
        };
    }

    private static Component snapshotLink(Component title, MenuType<?> menu, List<ItemStack> items) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        synchronized (SNAPSHOTS) {
            SNAPSHOTS.put(id, new Snapshot(title, menu, items));
        }
        return Component.literal("[").append(title).append("]").withStyle(s -> s
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.RunCommand("/pandacolors view " + id))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to view"))));
    }

    public static boolean open(ServerPlayer viewer, String id) {
        Snapshot snapshot;
        synchronized (SNAPSHOTS) {
            snapshot = SNAPSHOTS.get(id);
        }
        if (snapshot == null) return false;
        SimpleGui gui = new SimpleGui(snapshot.menu, viewer, false);
        gui.setTitle(snapshot.title);
        for (int i = 0; i < snapshot.items.size() && i < gui.getSize(); i++) {
            ItemStack stack = snapshot.items.get(i);
            if (!stack.isEmpty()) gui.setSlot(i, GuiElementBuilder.from(stack.copy()));
        }
        gui.open();
        return true;
    }
}
