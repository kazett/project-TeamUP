package id.teamup.gui;

import id.teamup.Msg;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
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

    /**
     * Teks yang digambar di dalam menu (pengganti item dekorasi). line: 0 = satu baris di tengah slot,
     * 1 = baris atas, 2 = baris bawah (dua baris dalam satu slot tinggi).
     */
    public record Label(int slot, int line, String text) {}

    /** Lebar teks font bawaan Minecraft (pixel GUI, sudah termasuk jarak antar huruf). */
    public static int textWidth(String plain) {
        int w = 0;
        for (char c : plain.toCharArray()) {
            switch (c) {
                case ' ', 'I', 't', '(', ')', '[', ']', '{', '}', '*', '"' -> w += 4;
                case 'l' -> w += 3;
                case 'i', '!', '.', ',', ':', ';', '|', '\'' -> w += 2;
                case 'f', 'k', '<', '>' -> w += 5;
                case '@', '~' -> w += 7;
                default -> w += 6;
            }
        }
        return w;
    }

    /** Potong teks (berkode warna &) supaya lebarnya tidak lebih dari maxPx. */
    public static String fit(String legacy, int maxPx) {
        String t = legacy;
        while (!t.isEmpty() && textWidth(plain(t)) > maxPx) {
            t = t.substring(0, t.length() - 1);
            if (t.endsWith("&")) t = t.substring(0, t.length() - 1);
        }
        return t;
    }

    /** Pecah teks polos jadi beberapa baris selebar maxPx. */
    public static List<String> wrap(String text, int maxPx) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String next = cur.length() == 0 ? word : cur + " " + word;
            if (cur.length() > 0 && textWidth(next) > maxPx) {
                lines.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(next);
            }
        }
        if (cur.length() > 0) lines.add(cur.toString());
        return lines;
    }

    private static int labelY(int slot, int line) {
        int r = slot / 9;
        return switch (line) {
            case 1 -> 19 + 18 * r;
            case 2 -> 27 + 18 * r;
            default -> 22 + 18 * r;
        };
    }

    public static Component styled(GuiTheme theme, int rows, Collection<Integer> actionSlots,
                                   Map<Integer, Btn> buttons, String text) {
        return styled(theme, rows, actionSlots, buttons, List.of(), text);
    }

    /** Judul berisi gambar: background + plat slot + tombol berlabel + teks judul + label teks. */
    public static Component styled(GuiTheme theme, int rows, Collection<Integer> actionSlots,
                                   Map<Integer, Btn> buttons, List<Label> labels, String text) {
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
        root = root.append(Component.text("").font(DEFAULT).color(NamedTextColor.WHITE).append(Msg.c(t)));

        // label teks diletakkan setelah judul; font txt_<y> menurunkan teks ke baris yang diinginkan
        int cur = TITLE_X + textWidth(plain(t));
        List<Label> sorted = new ArrayList<>(labels);
        sorted.sort(Comparator.comparingInt(Label::slot).thenComparingInt(Label::line));
        for (Label l : sorted) {
            int x = 7 + 18 * (l.slot() % 9) + 4;
            int y = labelY(l.slot(), l.line());
            String txt = fit(l.text(), 7 + 18 * 9 - 3 - x);
            Key f = Key.key("teamup", "txt_" + y);
            root = root.append(Component.text(shift(x - cur)).font(f).color(NamedTextColor.WHITE).append(Msg.c(txt)));
            cur = x + textWidth(plain(txt));
        }
        return root;
    }
}
