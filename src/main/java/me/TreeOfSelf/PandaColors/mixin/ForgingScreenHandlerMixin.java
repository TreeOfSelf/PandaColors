package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import me.TreeOfSelf.PandaColors.mixin.accessor.ForgingScreenHandlerAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemCombinerMenu.class)
public class ForgingScreenHandlerMixin {

    @Shadow
    @Final
    protected Player player;

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void restoreOriginalNameFromInput(Container inventory, CallbackInfo ci) {
        if (!PandaColorsConfig.get().anvil) {
            return;
        }
        ItemCombinerMenu self = (ItemCombinerMenu) (Object) this;

        if (self instanceof AnvilMenu) {
            ForgingScreenHandlerAccessor accessor = (ForgingScreenHandlerAccessor) self;

            if (inventory == accessor.pandaColors$getInput()) {
                ItemStack inputStack = accessor.pandaColors$getInput().getItem(0);
                if (!inputStack.isEmpty()) {
                    ItemStack displayStack = null;

                    CustomData customData = inputStack.get(DataComponents.CUSTOM_DATA);
                    if (customData != null) {
                        CompoundTag nbt = customData.copyTag();
                        if (nbt.contains("panda_colors_original_name")) {
                            String originalName = nbt.getString("panda_colors_original_name").orElse("");
                            displayStack = inputStack.copy();
                            displayStack.set(DataComponents.CUSTOM_NAME, Component.literal(originalName));
                        }
                    }

                    if (displayStack == null) {
                        WrittenBookContent bookContent = inputStack.get(DataComponents.WRITTEN_BOOK_CONTENT);
                        if (bookContent != null) {
                            String titleRaw = bookContent.title().raw();
                            if (titleRaw.contains("§")) {
                                String strippedTitle = titleRaw.replaceAll("§.", "");
                                Filterable<String> newTitle = Filterable.passThrough(strippedTitle);

                                displayStack = inputStack.copy();
                                WrittenBookContent newContent = new WrittenBookContent(
                                        newTitle,
                                        bookContent.author(),
                                        bookContent.generation(),
                                        bookContent.pages(),
                                        bookContent.resolved()
                                );
                                displayStack.set(DataComponents.WRITTEN_BOOK_CONTENT, newContent);
                            }
                        }
                    }

                    if (displayStack != null && this.player instanceof ServerPlayer serverPlayer) {
                        ClientboundContainerSetSlotPacket fakePacket = new ClientboundContainerSetSlotPacket(
                                self.containerId, self.incrementStateId(), 0, displayStack
                        );
                        serverPlayer.connection.send(fakePacket);
                    }
                }
            }
        }
    }
}
