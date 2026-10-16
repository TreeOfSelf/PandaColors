package me.TreeOfSelf.PandaColors.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import me.TreeOfSelf.PandaColors.Perms;
import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.FilteredText;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.entity.SignTextSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin {

    @Inject(method = "setAllowedPlayerEditor", at = @At("HEAD"))
    private void handleEditingStart(UUID editor, CallbackInfo ci) {
        if (!PandaColorsConfig.get().sign) {
            return;
        }
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide() && editor != null) {
            ServerLevel serverLevel = (ServerLevel) self.getLevel();
            ServerPlayer serverPlayer = (ServerPlayer) serverLevel.getPlayerByUUID(editor);
            if (serverPlayer == null) {
                return;
            }
            SignTextSlot editingSlot = self.getSlotPlayerIsFacing(serverPlayer);
            restoreForEditing(self, editingSlot);
            serverPlayer.connection.send(self.getUpdatePacket());
        }
    }

    @Unique
    private void restoreForEditing(SignBlockEntity self, SignTextSlot editingSlot) {
        if (!PandaColorsConfig.get().sign) {
            return;
        }
        DataComponentMap components = self.components();
        CustomData customData = components.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            SignTextSlot otherSlot = editingSlot == SignTextSlot.FRONT ? SignTextSlot.BACK : SignTextSlot.FRONT;
            String editingSideKey = originalKey(editingSlot);
            if (nbt.contains(editingSideKey)) {
                restoreTextSide(self, nbt, editingSideKey, editingSlot, true);
            }
            String otherSideKey = originalKey(otherSlot);
            if (nbt.contains(otherSideKey)) {
                restoreTextSide(self, nbt, otherSideKey, otherSlot, false);
            }
        }
    }

    @Unique
    private static String originalKey(SignTextSlot slot) {
        return slot == SignTextSlot.FRONT ? "panda_colors_original_front" : "panda_colors_original_back";
    }

    @Unique
    private void restoreTextSide(SignBlockEntity self, CompoundTag nbt, String key, SignTextSlot slot, boolean showOriginal) {

        String jsonString = nbt.getString(key).orElse("");
        JsonObject originalLines = JsonParser.parseString(jsonString).getAsJsonObject();

        SignText.Mutable signText = self.getText(slot).asMutable();

        if (showOriginal) {
            for (int i = 0; i < 4; i++) {
                String lineKey = "line_" + i;
                if (originalLines.has(lineKey)) {
                    String originalLine = originalLines.get(lineKey).getAsString();
                    signText.setLine(i, Component.literal(originalLine));
                }
            }
        } else {
            StringBuilder combinedText = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String lineKey = "line_" + i;
                if (originalLines.has(lineKey)) {
                    String originalLine = originalLines.get(lineKey).getAsString();
                    if (i > 0) combinedText.append("\n");
                    combinedText.append(originalLine);
                }
            }

            Component formattedCombined = TextFormattingHelper.formatStyledInput(combinedText.toString());
            Component[] formattedLines = splitFormattedTextProperly(formattedCombined);

            for (int i = 0; i < 4; i++) {
                signText.setLine(i, formattedLines[i]);
            }
        }

        self.setText(signText.asImmutable(), slot);

    }

    @Inject(method = "updateSignText", at = @At("TAIL"))
    private void formatAndStoreSignText(Player player, SignTextSlot slot, List<FilteredText> messages, CallbackInfo ci) {
        if (!PandaColorsConfig.get().sign) {
            return;
        }
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide()) {
            SignText signText = self.getText(slot);
            List<Component> currentLines = signText.getMessages(false);
            JsonObject previousLines = previousLines(self, slot);
            boolean allowed = Perms.can(player, Perms.SIGN);
            JsonObject originalLines = new JsonObject();
            StringBuilder combinedText = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                String originalString = currentLines.get(i).getString();
                if (!allowed) {
                    String previous = previousLines != null && previousLines.has("line_" + i)
                            ? previousLines.get("line_" + i).getAsString() : "";
                    originalString = keepPreviousFormatting(originalString, previous);
                }
                originalLines.addProperty("line_" + i, originalString);

                if (i > 0) combinedText.append("\n");
                combinedText.append(originalString);

            }
            Component formattedCombined = TextFormattingHelper.formatStyledInput(combinedText.toString());
            Component[] formattedLines = splitFormattedTextProperly(formattedCombined);
            SignText.Mutable formattedSignText = signText.asMutable();
            for (int i = 0; i < 4; i++) {
                formattedSignText.setLine(i, formattedLines[i]);
            }
            String key = originalKey(slot);

            DataComponentMap components = self.components();
            CustomData existingData = components.get(DataComponents.CUSTOM_DATA);
            CompoundTag finalData;
            if (existingData != null) {
                finalData = existingData.copyTag();
            } else {
                finalData = new CompoundTag();
            }
            finalData.putString(key, originalLines.toString());

            DataComponentMap newComponents = DataComponentMap.builder()
                    .addAll(components)
                    .set(DataComponents.CUSTOM_DATA, CustomData.of(finalData))
                    .build();
            self.setComponents(newComponents);

            self.setText(formattedSignText.asImmutable(), slot);
        }
    }

    @Unique
    private static final java.util.regex.Pattern FORMAT_TOKEN = java.util.regex.Pattern.compile("<[^<>]*>|&[0-9a-fk-orA-FK-OR]");

    @Unique
    private static JsonObject previousLines(SignBlockEntity self, SignTextSlot slot) {
        CustomData customData = self.components().get(DataComponents.CUSTOM_DATA);
        if (customData == null) return null;
        String json = customData.copyTag().getString(originalKey(slot)).orElse("");
        if (json.isEmpty()) return null;
        try {
            return JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // Without the sign permission: an unchanged line stays as it was, otherwise tags and &-codes that were already on
    // that line are kept and any other formatting is removed
    @Unique
    private static String keepPreviousFormatting(String line, String previous) {
        if (line.equals(previous)) return line;
        java.util.Set<String> previousTokens = new java.util.HashSet<>();
        java.util.regex.Matcher pm = FORMAT_TOKEN.matcher(previous);
        while (pm.find()) previousTokens.add(pm.group());

        StringBuilder out = new StringBuilder();
        java.util.regex.Matcher m = FORMAT_TOKEN.matcher(line);
        int last = 0;
        while (m.find()) {
            out.append(TextFormattingHelper.stripFormatting(line.substring(last, m.start())));
            String token = m.group();
            out.append(previousTokens.contains(token) ? token : TextFormattingHelper.stripFormatting(token));
            last = m.end();
        }
        out.append(TextFormattingHelper.stripFormatting(line.substring(last)));
        return out.toString();
    }

    @Unique
    private Component[] splitFormattedTextProperly(Component formattedText) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        final MutableComponent[] currentLine = {Component.empty()};

        formattedText.visit((style, string) -> {
            if (string.contains("\n")) {
                String[] parts = string.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        lines.add(currentLine[0]);
                        currentLine[0] = Component.empty();
                    }
                    if (!parts[i].isEmpty()) {
                        currentLine[0].append(Component.literal(parts[i]).withStyle(style));
                    }
                }
            } else {
                currentLine[0].append(Component.literal(string).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        lines.add(currentLine[0]);
        Component[] result = new Component[4];
        for (int i = 0; i < 4; i++) {
            result[i] = i < lines.size() ? lines.get(i) : Component.empty();
        }
        return result;
    }
}
