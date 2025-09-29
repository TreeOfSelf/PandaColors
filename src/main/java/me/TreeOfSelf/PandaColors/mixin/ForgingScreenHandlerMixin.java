package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.mixin.accessor.ForgingScreenHandlerAccessor;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.WrittenBookContentComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ForgingScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ForgingScreenHandler.class)
public class ForgingScreenHandlerMixin {

    @Shadow @Final protected PlayerEntity player;

    @Inject(method = "onContentChanged", at = @At("HEAD"))
    private void restoreOriginalNameFromInput(Inventory inventory, CallbackInfo ci) {
        ForgingScreenHandler self = (ForgingScreenHandler) (Object) this;

        if (self instanceof AnvilScreenHandler) {
            ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) self;

            if (inventory == accessor.pandaColors$getInput()) {
                ItemStack inputStack = accessor.pandaColors$getInput().getStack(0);
                if (!inputStack.isEmpty()) {
                    ItemStack displayStack = null;

                    NbtComponent customData = inputStack.get(DataComponentTypes.CUSTOM_DATA);
                    if (customData != null) {
                        NbtCompound nbt = customData.copyNbt();
                        if (nbt.contains("panda_colors_original_name")) {
                            String originalName = nbt.getString("panda_colors_original_name").get();
                            displayStack = inputStack.copy();
                            displayStack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(originalName));
                        }
                    }

                    if (displayStack == null) {
                        WrittenBookContentComponent bookContent = inputStack.get(DataComponentTypes.WRITTEN_BOOK_CONTENT);
                        if (bookContent != null) {
                            String titleRaw = bookContent.title().raw();
                            if (titleRaw.contains("§")) {
                                String strippedTitle = titleRaw.replaceAll("§.", "");
                                RawFilteredPair<String> newTitle = new RawFilteredPair<>(strippedTitle, bookContent.title().filtered());

                                displayStack = inputStack.copy();
                                WrittenBookContentComponent newContent = new WrittenBookContentComponent(
                                        newTitle,
                                        bookContent.author(),
                                        bookContent.generation(),
                                        bookContent.pages(),
                                        bookContent.resolved()
                                );
                                displayStack.set(DataComponentTypes.WRITTEN_BOOK_CONTENT, newContent);
                            }
                        }
                    }

                    if (displayStack != null && player instanceof ServerPlayerEntity serverPlayer) {
                        ScreenHandlerSlotUpdateS2CPacket fakePacket = new ScreenHandlerSlotUpdateS2CPacket(
                                self.syncId, self.nextRevision(), 0, displayStack
                        );
                        serverPlayer.networkHandler.sendPacket(fakePacket);
                    }
                }
            }
        }
    }
}