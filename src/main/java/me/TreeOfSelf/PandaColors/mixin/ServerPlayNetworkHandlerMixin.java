package me.TreeOfSelf.PandaColors.mixin;

import net.minecraft.server.filter.FilteredMessage;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.text.RawFilteredPair;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ServerPlayNetworkHandler.class)
public class ServerPlayNetworkHandlerMixin {

    @ModifyArg(method = "updateBookContent",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"),
            index = 0)
    private java.util.function.Function<FilteredMessage, RawFilteredPair<String>> formatBookPages(
            java.util.function.Function<FilteredMessage, RawFilteredPair<String>> originalMapper) {

        return (FilteredMessage page) -> {
            RawFilteredPair<String> original = originalMapper.apply(page);
            String formattedRaw = convertColorCodes(original.raw());
            return new RawFilteredPair<>(formattedRaw, original.filtered());
        };
    }

    @ModifyArg(method = "addBook",
            at = @At(value = "INVOKE",
                    target = "Ljava/util/stream/Stream;map(Ljava/util/function/Function;)Ljava/util/stream/Stream;"),
            index = 0)
    private java.util.function.Function<FilteredMessage, RawFilteredPair<Text>> formatAddBookPages(
            java.util.function.Function<FilteredMessage, RawFilteredPair<Text>> originalMapper) {

        return (FilteredMessage page) -> {
            RawFilteredPair<Text> original = originalMapper.apply(page);
            String formattedRaw = convertColorCodes(original.raw().getString());
            return new RawFilteredPair<>(Text.literal(formattedRaw), original.filtered());
        };
    }

    @ModifyArg(method = "addBook",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/network/ServerPlayNetworkHandler;toRawFilteredPair(Lnet/minecraft/server/filter/FilteredMessage;)Lnet/minecraft/text/RawFilteredPair;",
                    ordinal = 0),
            index = 0)
    private FilteredMessage formatAddBookTitle(FilteredMessage title) {
        String rawFormatted = convertColorCodes(title.raw());
        return new FilteredMessage(rawFormatted, title.mask());
    }

    @Unique
    private static String convertColorCodes(String input) {
        if (input == null) return null;

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '&' && i + 1 < chars.length) {
                char next = chars[i + 1];
                boolean isValidCode = (next >= '0' && next <= '9') || (next >= 'a' && next <= 'f') || (next >= 'k' && next <= 'r');

                if (isValidCode) {
                    if (i > 0 && chars[i - 1] == '\\') {
                        result.setLength(result.length() - 1);
                        result.append(c);
                    } else {
                        result.append('§');
                    }
                } else {
                    result.append(c);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }
}