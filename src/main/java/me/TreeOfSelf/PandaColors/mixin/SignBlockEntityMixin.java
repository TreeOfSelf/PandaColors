package me.TreeOfSelf.PandaColors.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.TreeOfSelf.PandaColors.PandaColorsConfig;
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
            JsonObject originalLines = new JsonObject();
            StringBuilder combinedText = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                Component originalText = currentLines.get(i);
                String originalString = originalText.getString();
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
