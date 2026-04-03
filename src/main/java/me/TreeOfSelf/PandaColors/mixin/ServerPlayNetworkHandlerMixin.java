package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerPlayNetworkHandlerMixin {

    @ModifyVariable(method = "updateBookContents", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private List<FilteredText> pandaColors$mapBookPages(List<FilteredText> contents) {
        if (!PandaColorsConfig.get().book) {
            return contents;
        }

        return contents.stream()
                .map(page -> new FilteredText(
                        TextFormattingHelper.applyAmpersandColorCodes(page.raw()),
                        page.mask()
                ))
                .toList();
    }

    @ModifyVariable(method = "signBook", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private FilteredText pandaColors$mapBookTitle(FilteredText title) {
        if (!PandaColorsConfig.get().book) {
            return title;
        }

        return new FilteredText(
                TextFormattingHelper.applyAmpersandColorCodes(title.raw()),
                title.mask()
        );
    }

    @ModifyVariable(method = "signBook", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private List<FilteredText> pandaColors$mapSignBookContents(List<FilteredText> contents) {
        if (!PandaColorsConfig.get().book) {
            return contents;
        }

        return contents.stream()
                .map(page -> new FilteredText(
                        TextFormattingHelper.applyAmpersandColorCodes(page.raw()),
                        page.mask()
                ))
                .toList();
    }
}