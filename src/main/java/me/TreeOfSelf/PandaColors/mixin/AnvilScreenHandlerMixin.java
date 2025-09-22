package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.ForgingSlotsManager;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilScreenHandler.class)
public abstract class AnvilScreenHandlerMixin extends ForgingScreenHandler {

	@Shadow
	private String newItemName;

	public AnvilScreenHandlerMixin(@Nullable ScreenHandlerType<?> type, int syncId, PlayerInventory playerInventory, ScreenHandlerContext context, ForgingSlotsManager forgingSlotsManager) {
		super(type, syncId, playerInventory, context, forgingSlotsManager);
	}

	@Inject(method = "setNewItemName", at = @At("HEAD"), cancellable = true)
	private void checkIfRestoringOriginal(String name, CallbackInfoReturnable<Boolean> cir) {
		ItemStack inputStack = this.input.getStack(0);
		if (!inputStack.isEmpty()) {
			NbtComponent customData = inputStack.get(DataComponentTypes.CUSTOM_DATA);
			if (customData != null) {
				NbtCompound nbt = customData.copyNbt();
				if (nbt.contains("panda_colors_original_name")) {
					String originalName = nbt.getString("panda_colors_original_name").get();
					if (name.equals(originalName)) {
						cir.setReturnValue(false);
					}
				}
			}
		}
	}

	@Inject(method = "setNewItemName", at = @At("TAIL"))
	private void storeOriginalNameInResult(String name, CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValue()) {
			ItemStack resultStack = this.output.getStack(0);
			if (!resultStack.isEmpty() && this.newItemName != null && !this.newItemName.isEmpty()) {
				NbtComponent existingData = resultStack.get(DataComponentTypes.CUSTOM_DATA);
				NbtCompound finalData;
				if (existingData != null) {
					finalData = existingData.copyNbt();
				} else {
					finalData = new NbtCompound();
				}
				finalData.putString("panda_colors_original_name", this.newItemName);

				resultStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(finalData));
			}
		}
	}


	@ModifyArg(method = "updateResult",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/item/ItemStack;set(Lnet/minecraft/component/ComponentType;Ljava/lang/Object;)Ljava/lang/Object;", ordinal = 0),
			index = 1)
	public <T> T modifySecondArgument(T value) {
		return (T) TextFormattingHelper.formatTextWithCustomCodes(this.newItemName);
	}

	@ModifyArg(method = "setNewItemName", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;set(Lnet/minecraft/component/ComponentType;Ljava/lang/Object;)Ljava/lang/Object;"))
	public <T> T modifySecondArgumentTwo(T value) {
		return (T) TextFormattingHelper.formatTextWithCustomCodes(this.newItemName);
	}
}