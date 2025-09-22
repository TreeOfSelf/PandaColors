package me.TreeOfSelf.PandaColors.mixin;

import com.google.gson.JsonObject;
import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.block.entity.SignText;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.server.network.ServerPlayerEntity;
import com.google.gson.JsonParser;
import java.util.UUID;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin {

    @Inject(method = "setEditor", at = @At("HEAD"))
    private void handleEditingStart(UUID editor, CallbackInfo ci) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getWorld() != null && !self.getWorld().isClient() && editor != null) {
            ServerWorld serverWorld = (ServerWorld) self.getWorld();
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) serverWorld.getPlayerByUuid(editor);
            boolean editingFront = self.isPlayerFacingFront(serverPlayer);
            restoreForEditing(self, editingFront);
            serverPlayer.networkHandler.sendPacket(self.toUpdatePacket());
        }
    }

    @Unique
    private void restoreForEditing(SignBlockEntity self, boolean editingFront) {
        var components = self.getComponents();
        NbtComponent customData = components.get(DataComponentTypes.CUSTOM_DATA);
        if (customData != null) {
            NbtCompound nbt = customData.copyNbt();
            String editingSideKey = editingFront ? "panda_colorss_original_front" : "panda_colors_original_back";
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
    private void restoreTextSide(SignBlockEntity self, NbtCompound nbt, String key, boolean front, boolean showOriginal) {

        String jsonString = nbt.getString(key).get();
        JsonObject originalLines = JsonParser.parseString(jsonString).getAsJsonObject();

        SignText signText = self.getText(front);

        if (showOriginal) {
            for (int i = 0; i < 4; i++) {
                String lineKey = "line_" + i;
                if (originalLines.has(lineKey)) {
                    String originalLine = originalLines.get(lineKey).getAsString();
                    signText = signText.withMessage(i, Text.literal(originalLine));
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
            
            Text formattedCombined = TextFormattingHelper.formatTextWithCustomCodes(combinedText.toString());
            Text[] formattedLines = splitFormattedTextProperly(formattedCombined);
            
            for (int i = 0; i < 4; i++) {
                signText = signText.withMessage(i, formattedLines[i]);
            }
        }

        self.setText(signText, front);

    }

    @Inject(method = "tryChangeText", at = @At("TAIL"))
    private void formatAndStoreSignText(PlayerEntity player, boolean front, List<net.minecraft.server.filter.FilteredMessage> messages, CallbackInfo ci) {
        SignBlockEntity self = (SignBlockEntity) (Object) this;
        if (self.getWorld() != null && !self.getWorld().isClient()) {
            SignText signText = self.getText(front);
            JsonObject originalLines = new JsonObject();
            StringBuilder combinedText = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                Text originalText = signText.getMessage(i, false);
                String originalString = originalText.getString();
                originalLines.addProperty("line_" + i, originalString);
                
                if (i > 0) combinedText.append("\n");
                combinedText.append(originalString);

            }
            Text formattedCombined = TextFormattingHelper.formatTextWithCustomCodes(combinedText.toString());
            Text[] formattedLines = splitFormattedTextProperly(formattedCombined);
            SignText formattedSignText = signText;
            for (int i = 0; i < 4; i++) {
                formattedSignText = formattedSignText.withMessage(i, formattedLines[i]);
            }
            String key = front ? "panda_colors_original_front" : "panda_colors_original_back";
            
            var components = self.getComponents();
            NbtComponent existingData = components.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound finalData;
            if (existingData != null) {
                finalData = existingData.copyNbt();
            } else {
                finalData = new NbtCompound();
            }
            finalData.putString(key, originalLines.toString());
            
            var newComponents = net.minecraft.component.ComponentMap.builder()
                    .addAll(components)
                    .add(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(finalData))
                    .build();
            self.setComponents(newComponents);
            
            self.setText(formattedSignText, front);
        }
    }

    @Unique
    private Text[] splitFormattedTextProperly(Text formattedText) {
        java.util.List<Text> lines = new java.util.ArrayList<>();
        final net.minecraft.text.MutableText[] currentLine = {Text.empty()};
        
        formattedText.visit((style, string) -> {
            if (string.contains("\n")) {
                String[] parts = string.split("\n", -1);
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) {
                        lines.add(currentLine[0]);
                        currentLine[0] = Text.empty();
                    }
                    if (!parts[i].isEmpty()) {
                        currentLine[0].append(Text.literal(parts[i]).setStyle(style));
                    }
                }
            } else {
                currentLine[0].append(Text.literal(string).setStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        lines.add(currentLine[0]);
        Text[] result = new Text[4];
        for (int i = 0; i < 4; i++) {
            result[i] = i < lines.size() ? lines.get(i) : Text.empty();
        }
        return result;
    }
}