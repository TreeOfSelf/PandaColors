package me.TreeOfSelf.PandaColors.mixin;

import me.TreeOfSelf.PandaColors.PandaColorsConfig;
import me.TreeOfSelf.PandaColors.TextFormattingHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPlayNetworkHandlerMixin {

    @Shadow
    public ServerPlayer player;

    @Shadow
    protected abstract Filterable<String> filterableFromOutgoing(FilteredText text);

    @ModifyVariable(
            method = "broadcastChatMessage",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private PlayerChatMessage pandaColors$formatChatBeforeBroadcast(PlayerChatMessage message) {
        if (!PandaColorsConfig.get().chat) {
            return message;
        }

        return message.withUnsignedContent(TextFormattingHelper.formatStyledInput(message.signedContent()));
    }

    @Inject(method = "updateBookContents", at = @At("HEAD"))
    private void pandaColors$mapBookPages(List<FilteredText> contents, int slot, CallbackInfo ci) {
        if (!PandaColorsConfig.get().book) return;

        contents.replaceAll(page -> new FilteredText(
                TextFormattingHelper.applyAmpersandColorCodes(page.raw()),
                page.mask()
        ));
    }

    @Inject(method = "signBook", at = @At("HEAD"), cancellable = true)
    private void pandaColors$signBook(FilteredText title, List<FilteredText> contents, int slot, CallbackInfo ci) {
        if (!PandaColorsConfig.get().book) return;

        FilteredText newTitle = new FilteredText(
                TextFormattingHelper.applyAmpersandColorCodes(title.raw()),
                title.mask()
        );
        List<FilteredText> newContents = contents.stream()
                .map(page -> new FilteredText(
                        TextFormattingHelper.applyAmpersandColorCodes(page.raw()),
                        page.mask()
                ))
                .toList();

        ItemStack carried = this.player.getInventory().getItem(slot);
        if (carried.has(DataComponents.WRITABLE_BOOK_CONTENT)) {
            ItemStack writtenBook = carried.transmuteCopy(Items.WRITTEN_BOOK);
            writtenBook.remove(DataComponents.WRITABLE_BOOK_CONTENT);
            List<Filterable<Component>> pages = newContents.stream().map((page) -> (Filterable<Component>)(Filterable<?>)this.filterableFromOutgoing(page).map(Component::literal)).toList();            writtenBook.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(this.filterableFromOutgoing(newTitle), this.player.getPlainTextName(), 0, pages, true));
            this.player.getInventory().setItem(slot, writtenBook);
        }
        ci.cancel();
    }
}
