package me.TreeOfSelf.PandaColors;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.parent.ColorNode;
import eu.pb4.placeholders.api.node.parent.GradientNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1;
import eu.pb4.placeholders.api.parsers.NodeParser;
import eu.pb4.placeholders.api.parsers.ParserBuilder;
import eu.pb4.placeholders.api.parsers.tag.TagRegistry;
import eu.pb4.placeholders.api.parsers.tag.TextTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TextFormattingHelper {

    private static final Map<String, Integer> COLOR_MAP = new LinkedHashMap<>();

    private static final NodeParser PARSER_BASE;
    private static final NodeParser PARSER_WITH_MARKDOWN;

    static {
        // --- Minecraft named color aliases (no underscores) ---
        COLOR_MAP.put("darkblue",    0x0000AA);
        COLOR_MAP.put("darkgreen",   0x00AA00);
        COLOR_MAP.put("darkaqua",    0x00AAAA);
        COLOR_MAP.put("darkred",     0xAA0000);
        COLOR_MAP.put("darkpurple",  0xAA00AA);
        COLOR_MAP.put("darkgray",    0x555555);
        COLOR_MAP.put("darkgrey",    0x555555);
        COLOR_MAP.put("lightpurple", 0xFF55FF);
        COLOR_MAP.put("grey",        0xAAAAAA);

        // --- CSS3 named colors (excludes the 16 Minecraft natives already in the parser) ---
        COLOR_MAP.put("aliceblue",           0xF0F8FF);
        COLOR_MAP.put("antiquewhite",        0xFAEBD7);
        COLOR_MAP.put("aquamarine",          0x7FFFD4);
        COLOR_MAP.put("azure",               0xF0FFFF);
        COLOR_MAP.put("beige",               0xF5F5DC);
        COLOR_MAP.put("bisque",              0xFFE4C4);
        COLOR_MAP.put("blanchedalmond",      0xFFEBCD);
        COLOR_MAP.put("blueviolet",          0x8A2BE2);
        COLOR_MAP.put("brown",               0xA52A2A);
        COLOR_MAP.put("burlywood",           0xDEB887);
        COLOR_MAP.put("cadetblue",           0x5F9EA0);
        COLOR_MAP.put("chartreuse",          0x7FFF00);
        COLOR_MAP.put("chocolate",           0xD2691E);
        COLOR_MAP.put("coral",               0xFF7F50);
        COLOR_MAP.put("cornflowerblue",      0x6495ED);
        COLOR_MAP.put("cornsilk",            0xFFF8DC);
        COLOR_MAP.put("crimson",             0xDC143C);
        COLOR_MAP.put("cyan",                0x00FFFF);
        COLOR_MAP.put("darkcyan",            0x008B8B);
        COLOR_MAP.put("darkgoldenrod",       0xB8860B);
        COLOR_MAP.put("darkkhaki",           0xBDB76B);
        COLOR_MAP.put("darkmagenta",         0x8B008B);
        COLOR_MAP.put("darkolivegreen",      0x556B2F);
        COLOR_MAP.put("darkorange",          0xFF8C00);
        COLOR_MAP.put("darkorchid",          0x9932CC);
        COLOR_MAP.put("darksalmon",          0xE9967A);
        COLOR_MAP.put("darkseagreen",        0x8FBC8F);
        COLOR_MAP.put("darkslateblue",       0x483D8B);
        COLOR_MAP.put("darkslategray",       0x2F4F4F);
        COLOR_MAP.put("darkslategrey",       0x2F4F4F);
        COLOR_MAP.put("darkturquoise",       0x00CED1);
        COLOR_MAP.put("darkviolet",          0x9400D3);
        COLOR_MAP.put("deeppink",            0xFF1493);
        COLOR_MAP.put("deepskyblue",         0x00BFFF);
        COLOR_MAP.put("dimgray",             0x696969);
        COLOR_MAP.put("dimgrey",             0x696969);
        COLOR_MAP.put("dodgerblue",          0x1E90FF);
        COLOR_MAP.put("firebrick",           0xB22222);
        COLOR_MAP.put("floralwhite",         0xFFFAF0);
        COLOR_MAP.put("forestgreen",         0x228B22);
        COLOR_MAP.put("fuchsia",             0xFF00FF);
        COLOR_MAP.put("gainsboro",           0xDCDCDC);
        COLOR_MAP.put("ghostwhite",          0xF8F8FF);
        COLOR_MAP.put("goldenrod",           0xDAA520);
        COLOR_MAP.put("greenyellow",         0xADFF2F);
        COLOR_MAP.put("honeydew",            0xF0FFF0);
        COLOR_MAP.put("hotpink",             0xFF69B4);
        COLOR_MAP.put("indianred",           0xCD5C5C);
        COLOR_MAP.put("indigo",              0x4B0082);
        COLOR_MAP.put("ivory",               0xFFFFF0);
        COLOR_MAP.put("khaki",               0xF0E68C);
        COLOR_MAP.put("lavender",            0xE6E6FA);
        COLOR_MAP.put("lavenderblush",       0xFFF0F5);
        COLOR_MAP.put("lawngreen",           0x7CFC00);
        COLOR_MAP.put("lemonchiffon",        0xFFFACD);
        COLOR_MAP.put("lightblue",           0xADD8E6);
        COLOR_MAP.put("lightcoral",          0xF08080);
        COLOR_MAP.put("lightcyan",           0xE0FFFF);
        COLOR_MAP.put("lightgoldenrodyellow",0xFAFAD2);
        COLOR_MAP.put("lightgray",           0xD3D3D3);
        COLOR_MAP.put("lightgreen",          0x90EE90);
        COLOR_MAP.put("lightgrey",           0xD3D3D3);
        COLOR_MAP.put("lightpink",           0xFFB6C1);
        COLOR_MAP.put("lightsalmon",         0xFFA07A);
        COLOR_MAP.put("lightseagreen",       0x20B2AA);
        COLOR_MAP.put("lightskyblue",        0x87CEFA);
        COLOR_MAP.put("lightslategray",      0x778899);
        COLOR_MAP.put("lightslategrey",      0x778899);
        COLOR_MAP.put("lightsteelblue",      0xB0C4DE);
        COLOR_MAP.put("lightyellow",         0xFFFFE0);
        COLOR_MAP.put("lime",                0x00FF00);
        COLOR_MAP.put("limegreen",           0x32CD32);
        COLOR_MAP.put("linen",               0xFAF0E6);
        COLOR_MAP.put("magenta",             0xFF00FF);
        COLOR_MAP.put("maroon",              0x800000);
        COLOR_MAP.put("mediumaquamarine",    0x66CDAA);
        COLOR_MAP.put("mediumblue",          0x0000CD);
        COLOR_MAP.put("mediumorchid",        0xBA55D3);
        COLOR_MAP.put("mediumpurple",        0x9370DB);
        COLOR_MAP.put("mediumseagreen",      0x3CB371);
        COLOR_MAP.put("mediumslateblue",     0x7B68EE);
        COLOR_MAP.put("mediumspringgreen",   0x00FA9A);
        COLOR_MAP.put("mediumturquoise",     0x48D1CC);
        COLOR_MAP.put("mediumvioletred",     0xC71585);
        COLOR_MAP.put("midnightblue",        0x191970);
        COLOR_MAP.put("mintcream",           0xF5FFFA);
        COLOR_MAP.put("mistyrose",           0xFFE4E1);
        COLOR_MAP.put("moccasin",            0xFFE4B5);
        COLOR_MAP.put("navajowhite",         0xFFDEAD);
        COLOR_MAP.put("navy",                0x000080);
        COLOR_MAP.put("oldlace",             0xFDF5E6);
        COLOR_MAP.put("olive",               0x808000);
        COLOR_MAP.put("olivedrab",           0x6B8E23);
        COLOR_MAP.put("orange",              0xFFA500);
        COLOR_MAP.put("orangered",           0xFF4500);
        COLOR_MAP.put("orchid",              0xDA70D6);
        COLOR_MAP.put("palegoldenrod",       0xEEE8AA);
        COLOR_MAP.put("palegreen",           0x98FB98);
        COLOR_MAP.put("paleturquoise",       0xAFEEEE);
        COLOR_MAP.put("palevioletred",       0xDB7093);
        COLOR_MAP.put("papayawhip",          0xFFEFD5);
        COLOR_MAP.put("peachpuff",           0xFFDAB9);
        COLOR_MAP.put("peru",                0xCD853F);
        COLOR_MAP.put("pink",                0xFFC0CB);
        COLOR_MAP.put("plum",                0xDDA0DD);
        COLOR_MAP.put("powderblue",          0xB0E0E6);
        COLOR_MAP.put("purple",              0x800080);
        COLOR_MAP.put("rebeccapurple",       0x663399);
        COLOR_MAP.put("rosybrown",           0xBC8F8F);
        COLOR_MAP.put("royalblue",           0x4169E1);
        COLOR_MAP.put("saddlebrown",         0x8B4513);
        COLOR_MAP.put("salmon",              0xFA8072);
        COLOR_MAP.put("sandybrown",          0xF4A460);
        COLOR_MAP.put("seagreen",            0x2E8B57);
        COLOR_MAP.put("seashell",            0xFFF5EE);
        COLOR_MAP.put("sienna",              0xA0522D);
        COLOR_MAP.put("silver",              0xC0C0C0);
        COLOR_MAP.put("skyblue",             0x87CEEB);
        COLOR_MAP.put("slateblue",           0x6A5ACD);
        COLOR_MAP.put("slategray",           0x708090);
        COLOR_MAP.put("slategrey",           0x708090);
        COLOR_MAP.put("snow",                0xFFFAFA);
        COLOR_MAP.put("springgreen",         0x00FF7F);
        COLOR_MAP.put("steelblue",           0x4682B4);
        COLOR_MAP.put("tan",                 0xD2B48C);
        COLOR_MAP.put("teal",                0x008080);
        COLOR_MAP.put("thistle",             0xD8BFD8);
        COLOR_MAP.put("tomato",              0xFF6347);
        COLOR_MAP.put("turquoise",           0x40E0D0);
        COLOR_MAP.put("violet",              0xEE82EE);
        COLOR_MAP.put("wheat",               0xF5DEB3);
        COLOR_MAP.put("whitesmoke",          0xF5F5F5);
        COLOR_MAP.put("yellowgreen",         0x9ACD32);

        // --- Extra descriptive colors ---
        COLOR_MAP.put("scarlet",     0xFF2400);
        COLOR_MAP.put("ruby",        0x9B111E);
        COLOR_MAP.put("wine",        0x722F37);
        COLOR_MAP.put("burgundy",    0x800020);
        COLOR_MAP.put("garnet",      0x733635);
        COLOR_MAP.put("rust",        0xB7410E);
        COLOR_MAP.put("brick",       0xCB4154);
        COLOR_MAP.put("ember",       0xCF4F00);
        COLOR_MAP.put("blood",       0x8B0000);
        COLOR_MAP.put("rose",        0xFF007F);
        COLOR_MAP.put("blush",       0xDE5D83);
        COLOR_MAP.put("candy",       0xFF4081);
        COLOR_MAP.put("flamingo",    0xFC8EAC);
        COLOR_MAP.put("mauve",       0xE0B0FF);
        COLOR_MAP.put("lilac",       0xC8A2C8);
        COLOR_MAP.put("periwinkle",  0xCCCCFF);
        COLOR_MAP.put("cobalt",      0x0047AB);
        COLOR_MAP.put("sapphire",    0x0F52BA);
        COLOR_MAP.put("cerulean",    0x007BA7);
        COLOR_MAP.put("ocean",       0x006994);
        COLOR_MAP.put("sky",         0x87CEEB);
        COLOR_MAP.put("ice",         0x99C5C4);
        COLOR_MAP.put("mint",        0x98FF98);
        COLOR_MAP.put("sage",        0xB2AC88);
        COLOR_MAP.put("jade",        0x00A86B);
        COLOR_MAP.put("emerald",     0x50C878);
        COLOR_MAP.put("forest",      0x228B22);
        COLOR_MAP.put("moss",        0x8A9A5B);
        COLOR_MAP.put("pine",        0x01796F);
        COLOR_MAP.put("amber",       0xFFBF00);
        COLOR_MAP.put("honey",       0xFFC30B);
        COLOR_MAP.put("mustard",     0xFFDB58);
        COLOR_MAP.put("cream",       0xFFFDD0);
        COLOR_MAP.put("latte",       0xA98668);
        COLOR_MAP.put("mocha",       0x967259);
        COLOR_MAP.put("espresso",    0x4B3832);
        COLOR_MAP.put("coffee",      0x6F4E37);
        COLOR_MAP.put("caramel",     0xC68642);
        COLOR_MAP.put("mahogany",    0xC04000);
        COLOR_MAP.put("walnut",      0x773F1A);
        COLOR_MAP.put("chestnut",    0x954535);
        COLOR_MAP.put("auburn",      0x922724);
        COLOR_MAP.put("copper",      0xB87333);
        COLOR_MAP.put("bronze",      0xCD7F32);
        COLOR_MAP.put("brass",       0xB5A642);
        COLOR_MAP.put("pearl",       0xF0EAD6);
        COLOR_MAP.put("champagne",   0xF7E7CE);
        COLOR_MAP.put("charcoal",    0x36454F);
        COLOR_MAP.put("obsidian",    0x1B1B1B);
        COLOR_MAP.put("onyx",        0x353935);
        COLOR_MAP.put("ash",         0xB2BEB5);
        COLOR_MAP.put("smoke",       0x738276);
        COLOR_MAP.put("graphite",    0x474A51);
        COLOR_MAP.put("platinum",    0xE5E4E2);
        COLOR_MAP.put("iron",        0xA19D94);

        // Build tag registry: copy defaults, replace gradient tags with our own
        TagRegistry.Builder builder = TagRegistry.builderCopyDefault()
                .remove("gradient")
                .remove("hard_gradient");

        // Register every color name as a proper enclosing tag
        for (Map.Entry<String, Integer> e : COLOR_MAP.entrySet()) {
            TextColor color = TextColor.fromRgb(e.getValue());
            builder.add(TextTag.enclosing(e.getKey(), "color", true,
                    (nodes, args, parser) -> new ColorNode(nodes, color)));
        }

        // Register gradient tag that resolves our color names in addition to Minecraft's
        builder.add(TextTag.enclosing("gradient", List.of("gr"), "gradient", true,
                (nodes, args, parser) -> {
                    List<TextColor> colors = resolveColorArgs(args.ordered());
                    if (colors.isEmpty()) return new ParentNode(nodes);
                    String type = args.get("type", "");
                    GradientNode.GradientProvider provider = switch (type) {
                        case "oklab" -> GradientNode.GradientProvider.colorsOkLab(colors);
                        case "hvs"   -> GradientNode.GradientProvider.colorsHvs(colors);
                        case "hard"  -> GradientNode.GradientProvider.colorsHard(colors);
                        default      -> GradientNode.GradientProvider.colors(colors);
                    };
                    return new GradientNode(nodes, provider);
                }));

        builder.add(TextTag.enclosing("hard_gradient", List.of("hgr"), "gradient", true,
                (nodes, args, parser) -> {
                    List<TextColor> colors = resolveColorArgs(args.ordered());
                    if (colors.isEmpty()) return new ParentNode(nodes);
                    return new GradientNode(nodes, GradientNode.GradientProvider.colorsHard(colors));
                }));

        TagRegistry registry = builder.build();

        PARSER_BASE = new ParserBuilder()
                .simplifiedTextFormat()
                .customTagRegistry(registry)
                .build();

        PARSER_WITH_MARKDOWN = new ParserBuilder()
                .simplifiedTextFormat()
                .customTagRegistry(registry)
                .markdown(
                        MarkdownLiteParserV1.MarkdownFormat.BOLD,
                        MarkdownLiteParserV1.MarkdownFormat.ITALIC,
                        MarkdownLiteParserV1.MarkdownFormat.UNDERLINE,
                        MarkdownLiteParserV1.MarkdownFormat.STRIKETHROUGH,
                        MarkdownLiteParserV1.MarkdownFormat.SPOILER
                )
                .build();
    }

    private static Optional<TextColor> resolveColor(String name) {
        Integer rgb = COLOR_MAP.get(name.toLowerCase());
        if (rgb != null) return Optional.of(TextColor.fromRgb(rgb));
        return TextColor.parseColor(name).result();
    }

    private static List<TextColor> resolveColorArgs(List<String> args) {
        List<TextColor> colors = new ArrayList<>();
        for (String arg : args) {
            resolveColor(arg).ifPresent(colors::add);
        }
        return colors;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public static String applyAmpersandColorCodes(String input) {
        if (input == null) return null;

        StringBuilder result = new StringBuilder();
        char[] chars = input.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if (c == '&' && i + 1 < chars.length) {
                char next = chars[i + 1];
                boolean isValidCode = (next >= '0' && next <= '9')
                        || (next >= 'a' && next <= 'f')
                        || (next >= 'k' && next <= 'r');
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
        if (text == null || text.isEmpty()) return Component.empty();

        String processed = text.replace("<ra>", "<gr:red:yellow:green>");
        NodeParser parser = PandaColorsConfig.get().markdown ? PARSER_WITH_MARKDOWN : PARSER_BASE;
        return parser.parseNode(TextNode.of(processed)).toComponent(ParserContext.of(), true);
    }

    public static Component formatStyledInput(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return formatTextWithCustomCodes(applyAmpersandColorCodes(text));
    }
}
