package me.TreeOfSelf.PandaColors;

import eu.pb4.placeholders.api.TextParserUtils;
import net.minecraft.text.Text;

public class TextFormattingHelper {
    
    public static Text formatTextWithCustomCodes(String text) {
        if (text == null || text.isEmpty()) {
            return Text.empty();
        }
        
        // Replace <ra> with <gr:red:yellow:green>
        String processedText = text.replace("<ra>", "<gr:red:yellow:green>");
        
        // Apply the standard formatting
        return TextParserUtils.formatTextSafe(processedText);
    }
}
