package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import me.TreeOfSelf.PandaColors.Perms;
import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import me.TreeOfSelf.PandaColors.mixin.accessor.ForgingScreenHandlerAccessor;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.StringUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    private String itemName;

    // Without the permission the name keeps its plain text but loses the formatting
    @Unique
    private <T> T pandaColors$stripped(T value) {
        if (!PandaColorsConfig.get().anvil || this.itemName == null) return value;
        return (T) Component.literal(TextFormattingHelper.stripFormatting(this.itemName));
    }

    @Unique
    private boolean pandaColors$enabled() {
        return PandaColorsConfig.get().anvil && Perms.can(((ForgingScreenHandlerAccessor) this).pandaColors$getPlayer(), Perms.ANVIL);
    }

    @Inject(method = "setItemName", at = @At("HEAD"), cancellable = true)
    private void checkIfRestoringOriginal(String name, CallbackInfoReturnable<Boolean> cir) {
        if (!pandaColors$enabled()) {
            return;
        }
        ForgingScreenHandlerAccessor access = (ForgingScreenHandlerAccessor) this;
        ItemStack inputStack = access.pandaColors$getInput().getItem(0);
        if (!inputStack.isEmpty()) {
            CustomData customData = inputStack.get(DataComponents.CUSTOM_DATA);
            if (customData != null) {
                CompoundTag nbt = customData.copyTag();
                if (nbt.contains("panda_colors_original_name")) {
                    String originalName = nbt.getString("panda_colors_original_name").orElse("");
                    if (name.equals(originalName)) {
                        cir.setReturnValue(false);
                    }
                }
            }
        }
    }

    @Inject(method = "setItemName", at = @At("TAIL"))
    private void handleNameChanges(String name, CallbackInfoReturnable<Boolean> cir) {
        if (!pandaColors$enabled()) {
            return;
        }
        if (cir.getReturnValue()) {
            ForgingScreenHandlerAccessor access = (ForgingScreenHandlerAccessor) this;
            ItemStack resultStack = access.pandaColors$getResult().getItem(0);
            if (!resultStack.isEmpty()) {
                String sanitizedName = StringUtil.filterText(name);
                if (StringUtil.isBlank(sanitizedName)) {
                    CustomData existingData = resultStack.get(DataComponents.CUSTOM_DATA);
                    if (existingData != null) {
                        CompoundTag nbtData = existingData.copyTag();
                        nbtData.remove("panda_colors_original_name");
                        if (nbtData.isEmpty()) {
                            resultStack.remove(DataComponents.CUSTOM_DATA);
                        } else {
                            resultStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbtData));
                        }
                    }
                } else if (this.itemName != null && !this.itemName.isEmpty()) {
                    CustomData existingData = resultStack.get(DataComponents.CUSTOM_DATA);
                    CompoundTag finalData;
                    if (existingData != null) {
                        finalData = existingData.copyTag();
                    } else {
                        finalData = new CompoundTag();
                    }
                    finalData.putString("panda_colors_original_name", this.itemName);

                    resultStack.set(DataComponents.CUSTOM_DATA, CustomData.of(finalData));
                }
            }
        }
    }

    @ModifyArg(method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0),
            index = 1)
    public <T> T pandaColors$formatResultName(T value) {
        if (!pandaColors$enabled()) {
            return pandaColors$stripped(value);
        }
        return (T) TextFormattingHelper.formatStyledInput(this.itemName);
    }

    @ModifyArg(method = "setItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;set(Lnet/minecraft/core/component/DataComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
    public <T> T pandaColors$formatSetName(T value) {
        if (!pandaColors$enabled()) {
            return pandaColors$stripped(value);
        }
        return (T) TextFormattingHelper.formatStyledInput(this.itemName);
    }
}
