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
            boolean editingFront = self.isFacingFrontText(serverPlayer);
            restoreForEditing(self, editingFront);
            serverPlayer.connection.send(self.getUpdatePacket());
        }
    }

    @Unique
    private void restoreForEditing(SignBlockEntity self, boolean editingFront) {
        if (!PandaColorsConfig.get().sign) {
            return;
        }
        DataComponentMap components = self.components();
        CustomData customData = components.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            String editingSideKey = editingFront ? "panda_colors_original_front" : "panda_colors_original_back";
            if (nbt.contains(editingSideKey)) {
                restoreTextSide(self, nbt, editingSideKey, editingFront, true);
            }
            String otherSideKey = editingFront ? "panda_colors_original_back" : "panda_colors_original_front";
            if (nbt.contains(otherSideKey)) {
                restoreTextSide(self, nbt, otherSideKey, !editingFront, false);
            }
        }
    }

    @Unique
    private void restoreTextSide(SignBlockEntity self, CompoundTag nbt, String key, boolean front, boolean showOriginal) {

        String jsonString = nbt.getString(key).orElse("");
        JsonObject originalLines = JsonParser.parseString(jsonString).getAsJsonObject();

        SignText signText = self.getText(front);

        if (showOriginal) {
            for (int i = 0; i < 4; i++) {
                String lineKey = "line_" + i;
                if (originalLines.has(lineKey)) {
                    String originalLine = originalLines.get(lineKey).getAsString();
                    signText = signText.setMessage(i, Component.literal(originalLine));
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
                signText = signText.setMessage(i, formattedLines[i]);
            }
        }

        self.setText(signText, front);

    }

    @Inject(method = "updateSignText", at = @At("TAIL"))
    private void formatAndStoreSignText(Player player, boolean front, List<FilteredText> messages, CallbackInfo ci) {
        if (!PandaColorsConfig.get().sign) {
            return;
        }
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getLevel() != null && !self.getLevel().isClientSide()) {
            SignText signText = self.getText(front);
            JsonObject originalLines = new JsonObject();
            StringBuilder combinedText = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                Component originalText = signText.getMessage(i, false);
                String originalString = originalText.getString();
                originalLines.addProperty("line_" + i, originalString);

                if (i > 0) combinedText.append("\n");
                combinedText.append(originalString);

            }
            Component formattedCombined = TextFormattingHelper.formatStyledInput(combinedText.toString());
            Component[] formattedLines = splitFormattedTextProperly(formattedCombined);
            SignText formattedSignText = signText;
            for (int i = 0; i < 4; i++) {
                formattedSignText = formattedSignText.setMessage(i, formattedLines[i]);
            }
            String key = front ? "panda_colors_original_front" : "panda_colors_original_back";

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

            self.setText(formattedSignText, front);
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
