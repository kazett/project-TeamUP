package id.teamup.gui;

import id.teamup.Msg;
import java.util.Collection;
import java.util.Map;
import java.util.TreeSet;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/**
 * Bikin judul inventory yang isinya gambar (background panel + plat tombol) lewat font resource pack.
 * Judul dimulai di x=8 pixel. Glyph bitmap maju (lebar + 1) pixel, "spasi" negatif dipakai buat mundur.
 */
public final class GuiFont {
    private static final Key FONT = Key.key("teamup", "gui");
    private static final Key DEFAULT = Key.key("minecraft", "default");
    private static final int TITLE_X = 8;
    private static final int BG_ADVANCE = 176 + 1;
    private static final int PLATE_ADVANCE = 18 + 1;

    private GuiFont() {}

    /** Geser kursor n pixel (negatif = mundur) memakai karakter spasi dari pack. */
    static String shift(int n) {
        StringBuilder sb = new StringBuilder();
        int v = Math.abs(n);
        int base = n < 0 ? 0xF000 : 0xF200;
        for (int bit = 256; bit >= 1; bit >>= 1) {
            while (v >= bit) {
                sb.append((char) (base | bit));
                v -= bit;
            }
        }
        return sb.toString();
    }

    private static String plain(String legacy) {
        return PlainTextComponentSerializer.plainText().serialize(Msg.c(legacy));
    }

    /** Potong judul biar muat di header (maks ~26 karakter). */
    public static String clip(String legacy, int max) {
        String p = plain(legacy);
        return p.length() > max ? "&f" + p.substring(0, max - 2) + ".." : legacy;
    }

    /** Judul berisi gambar: background + plat slot + tombol berlabel + teks judul. */
    public static Component styled(GuiTheme theme, int rows, Collection<Integer> actionSlots,
                                   Map<Integer, Btn> buttons, String text) {
        java.util.Set<Integer> covered = new java.util.HashSet<>();
        for (Map.Entry<Integer, Btn> en : buttons.entrySet())
            for (int i = 0; i < en.getValue().width(); i++) covered.add(en.getKey() + i);
        TreeSet<Integer> draw = new TreeSet<>(buttons.keySet());
        for (int s : actionSlots) if (!covered.contains(s) && s >= 0 && s < rows * 9) draw.add(s);

        Component root = Component.empty();
        StringBuilder bg = new StringBuilder();
        bg.append(shift(-TITLE_X));
        bg.append((char) (0xE000 + (rows - 1) * 3 + theme.ordinal()));
        root = root.append(Component.text(bg.toString()).font(FONT).color(NamedTextColor.WHITE));
        int cursor = BG_ADVANCE;

        for (int slot : draw) {
            int row = slot / 9;
            int x = 7 + 18 * (slot % 9);
            Btn b = buttons.get(slot);
            String piece = shift(x - cursor);
            Component c;
            if (b != null) {
                piece += (char) (0xE200 + b.ordinal() * 6 + row);
                cursor = x + 18 * b.width() + 1;
                c = Component.text(piece).font(FONT).color(NamedTextColor.WHITE);
            } else {
                piece += (char) (0xE100 + row);
                cursor = x + PLATE_ADVANCE;
                c = Component.text(piece).font(FONT).color(TextColor.color(theme.rgb()));
            }
            root = root.append(c);
        }
        root = root.append(Component.text(shift(TITLE_X - cursor)).font(FONT));

        String t = text.replace("&4", "&c").replace("&0", "&f").replace("&8", "&7");
        t = clip(t, 26);
        return root.append(Component.text("").font(DEFAULT).color(NamedTextColor.WHITE).append(Msg.c(t)));
    }
}
