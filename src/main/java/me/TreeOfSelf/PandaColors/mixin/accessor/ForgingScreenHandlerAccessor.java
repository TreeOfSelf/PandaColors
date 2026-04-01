package me.TreeOfSelf.PandaColors.mixin.accessor;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.inventory.ResultContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ForgingScreenHandlerAccessor {
    @Accessor("inputSlots")
    Container pandaColors$getInput();

    @Accessor("resultSlots")
    ResultContainer pandaColors$getResult();
}
