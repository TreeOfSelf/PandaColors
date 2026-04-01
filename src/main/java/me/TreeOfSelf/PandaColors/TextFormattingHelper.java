package me.TreeOfSelf.PandaColors;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import net.minecraft.network.chat.Component;

public class TextFormattingHelper {

    private static final NodeParser PANDA_COLORS_PARSER = new ParserBuilder()
            .simplifiedTextFormat()
            .build();

    public static String applyAmpersandColorCodes(String input) {
        if (input == null) {
            return null;
        }

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

    public static Component formatTextWithCustomCodes(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        String processedText = text.replace("<ra>", "<gr:red:yellow:green>");
        TextNode node = PANDA_COLORS_PARSER.parseNode(TextNode.of(processedText));
        return node.toComponent(ParserContext.of(), true);
    }

    public static Component formatStyledInput(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return formatTextWithCustomCodes(applyAmpersandColorCodes(text));
    }
}
