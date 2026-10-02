package id.teamup.manager;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Rank;
import id.teamup.model.Team;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

/**
 * HUD pojok kanan atas: logo kotak di paling atas, plat tanggal/jam dan plat "NAMA TEAM - rank" di bawahnya.
 *
 * Semua digambar dalam SATU bossbar (warna WHITE yang dibuat transparan oleh pack), jadi bossbar lain
 * (misalnya ender dragon) tetap tampil normal di bawahnya. Posisi horizontal diatur dengan karakter spasi
 * (judul bossbar diatur total lebarnya = 2 x tengah layar, sehingga koordinat kursor = koordinat layar),
 * posisi vertikal diatur lewat font pack berbeda (teamup:hud_<offset>) yang menurunkan gambar sekian pixel.
 */
public class HudManager {
    private static final char GL_LEFT = '\uE900', GL_MID = '\uE901', GL_RIGHT = '\uE902';
    private static final char ICON_CALENDAR = '\uE920', ICON_SHIELD = '\uE921', ICON_SWORD = '\uE922';
    /** Pilihan ukuran logo (pixel GUI). Glyph logo = \uE910 + indeks. Harus sama dengan generator pack. */
    private static final int[] LOGO_SIZES = {16, 24, 32, 40, 48, 56, 64, 72, 80, 88, 96};
    private static final int LOGO_GAP = 3;     // jarak logo ke plat tanggal
    private static final int LINE_STEP = 19;   // jarak antar plat (tinggi 13 + 6)
    private static final Key BASE_FONT = Key.key("teamup", "hud");
    private static final TextColor WHITE = TextColor.color(0xFFFFFF);
    private static final int ICON_ADVANCE = 10;   // ikon 9 px + 1
    private static final int PAD_ICON = 18;       // jarak teks dari tepi kiri plat kalau ada ikon
    private static final int PAD_PLAIN = 6;       // kalau tanpa ikon
    private static final int PAD_RIGHT = 6;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final TeamUPPlugin plugin;
    private final Map<UUID, BossBar> bars = new HashMap<>();
    private final Map<UUID, String> last = new HashMap<>();
    private final NamespacedKey offKey;
    private BukkitTask task;

    public HudManager(TeamUPPlugin plugin) {
        this.plugin = plugin;
        this.offKey = new NamespacedKey(plugin, "hud_off");
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("hud.enabled", true);
    }

    private boolean logoOn() {
        return plugin.getConfig().getBoolean("hud.logo.enabled", true);
    }

    /** Ukuran logo kotak, dibulatkan ke pilihan terdekat. */
    private int logoSizeIndex() {
        int want = plugin.getConfig().getInt("hud.logo.size", 72);
        int best = 0;
        for (int i = 1; i < LOGO_SIZES.length; i++) {
            if (Math.abs(LOGO_SIZES[i] - want) < Math.abs(LOGO_SIZES[best] - want)) best = i;
        }
        return best;
    }

    private boolean hidden(Player p) {
        return p.getPersistentDataContainer().has(offKey, PersistentDataType.BYTE);
    }

    /** Nyalakan/matikan HUD untuk satu player (buat yang tidak pasang pack). Return true kalau jadi nyala. */
    public boolean toggle(Player p) {
        if (hidden(p)) {
            p.getPersistentDataContainer().remove(offKey);
            attach(p);
            return true;
        }
        p.getPersistentDataContainer().set(offKey, PersistentDataType.BYTE, (byte) 1);
        detach(p.getUniqueId());
        return false;
    }

    public void start() {
        if (!enabled()) return;
        for (Player p : Bukkit.getOnlinePlayers()) attach(p);
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!bars.containsKey(p.getUniqueId())) attach(p);
                update(p);
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (UUID id : new ArrayList<>(bars.keySet())) detach(id);
    }

    /** Dipanggil saat /tua reload: terapkan config baru (nyala/mati, logo, warna, posisi). */
    public void reload() {
        stop();
        start();
    }

    public void refreshAll() {
        last.clear();
        for (Player p : Bukkit.getOnlinePlayers()) update(p);
    }

    public void attach(Player p) {
        if (!enabled() || hidden(p) || bars.containsKey(p.getUniqueId())) return;
        BossBar b = BossBar.bossBar(Component.empty(), 0f, BossBar.Color.WHITE, BossBar.Overlay.PROGRESS);
        p.showBossBar(b);
        bars.put(p.getUniqueId(), b);
        update(p);
    }

    public void detach(UUID id) {
        BossBar b = bars.remove(id);
        last.remove(id);
        Player p = Bukkit.getPlayer(id);
        if (b != null && p != null) p.hideBossBar(b);
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(plugin.getConfig().getString("hud.timezone", "Asia/Jakarta"));
        } catch (Exception e) {
            return ZoneId.of("Asia/Jakarta");
        }
    }

    private int color(String path, int def) {
        String s = plugin.getConfig().getString(path, "");
        TextColor c = s == null ? null : TextColor.fromHexString(s.trim());
        return c == null ? def : c.value();
    }

    private static char icon(String name, char def) {
        if (name == null) return def;
        return switch (name.trim().toLowerCase()) {
            case "none", "off", "false" -> 0;
            case "calendar" -> ICON_CALENDAR;
            case "shield" -> ICON_SHIELD;
            case "sword" -> ICON_SWORD;
            default -> def;
        };
    }

    private static Key fontFor(int offset) {
        return Key.key("teamup", "hud_" + offset);
    }

    public void update(Player p) {
        BossBar b = bars.get(p.getUniqueId());
        if (b == null) return;
        var cfg = plugin.getConfig();
        int guiW = Math.max(200, cfg.getInt("hud.gui-width", 750));
        int margin = cfg.getInt("hud.margin", 12);
        int center = guiW / 2;
        int right = guiW - margin;

        boolean logo = logoOn();
        int li = logoSizeIndex();
        int logoSize = LOGO_SIZES[li];
        int logoOffX = cfg.getInt("hud.logo.offset-x", 0);
        // plat diturunkan sejauh tinggi logo + jarak; baris fraksi satu langkah lagi di bawahnya
        int off1 = logo ? logoSize + LOGO_GAP : 0;
        int off2 = off1 + LINE_STEP;

        Cursor cur = new Cursor();
        if (logo) {
            cur.shiftTo(right - logoSize + logoOffX, BASE_FONT);
            cur.add(Component.text(String.valueOf((char) (0xE910 + li))).font(BASE_FONT).color(WHITE),
                    logoSize + 1);
        }

        // baris tanggal + jam
        ZonedDateTime now = ZonedDateTime.now(zone());
        String label = cfg.getString("hud.timezone-label", "WIB");
        String date = now.format(DATE) + "  ";
        String time = now.format(TIME) + " ";
        List<Component> dp = new ArrayList<>();
        dp.add(Msg.c("&f" + date));
        dp.add(Msg.c("&f" + time));
        if (!label.isEmpty()) dp.add(Msg.c("&7" + label));
        int dw = width(date) + width(time) + width(label);
        char dateIcon = icon(cfg.getString("hud.date.icon", "calendar"), ICON_CALENDAR);
        plate(cur, fontFor(off1), right, color("hud.date.color", 0x3A86FF), dateIcon, dp, dw);

        // baris fraksi
        Team t = plugin.teams().ofPlayer(p.getUniqueId());
        char facIcon = icon(cfg.getString("hud.faction.icon", "shield"), ICON_SHIELD);
        String facColor = cfg.getString("hud.faction.color", "team");
        boolean teamBg = facColor != null && facColor.trim().equalsIgnoreCase("team");
        if (t == null) {
            plate(cur, fontFor(off2), right, color("hud.faction.no-team-color", 0x808080), facIcon,
                    List.of(Msg.c("&7no fraksi")), width("no fraksi"));
        } else {
            Rank r = t.rankOf(p.getUniqueId());
            String rank = r == null ? "-" : plugin.teams().rankName(t, r);
            String name = t.getName().toUpperCase();
            int tw = width(name) + width(" - ") + width(plain(rank));
            int bg = teamBg ? t.getColor().value() : color("hud.faction.color", 0x55FF55);
            List<Component> fp = new ArrayList<>();
            // kalau background mengikuti warna team, nama dibuat putih supaya tetap terbaca
            fp.add(Component.text(name, teamBg ? WHITE : t.getColor()));
            fp.add(Msg.c("&7 - "));
            fp.add(Msg.c("&f" + rank));
            plate(cur, fontFor(off2), right, bg, facIcon, fp, tw);
        }

        // total lebar judul = 2 x tengah layar -> bossbar mulai tepat di x = 0, koordinat kursor = koordinat layar
        cur.shiftTo(center * 2, BASE_FONT);

        Component title = cur.root;
        String key = String.valueOf(title);
        if (!key.equals(last.get(p.getUniqueId()))) {
            b.name(title);
            last.put(p.getUniqueId(), key);
        }
    }

    // ---------- susun judul bossbar ----------

    /** Kursor penyusun judul: melacak posisi horizontal (pixel GUI) dari awal judul. */
    private static final class Cursor {
        Component root = Component.empty();
        int pos = 0;

        void shiftTo(int target, Key font) {
            int d = target - pos;
            if (d != 0) root = root.append(Component.text(shift(d)).font(font));
            pos = target;
        }

        void add(Component c, int advance) {
            root = root.append(c);
            pos += advance;
        }
    }

    /**
     * Satu plat rata kanan di x = right - lebarPlat. Plat abu-abu diwarnai (tint) sesuai config;
     * ikon tetap berwarna asli (putih = tidak ditint).
     */
    private void plate(Cursor c, Key font, int right, int tint, char iconChar, List<Component> parts, int textW) {
        int padLeft = iconChar != 0 ? PAD_ICON : PAD_PLAIN;
        int n = Math.max(1, (int) Math.ceil((padLeft + textW + PAD_RIGHT - 8) / 8.0));
        int plate = 8 + 8 * n;
        int xp = right - plate;
        c.shiftTo(xp, font);

        StringBuilder body = new StringBuilder();
        body.append(GL_LEFT).append(shift(-1));
        for (int i = 0; i < n; i++) body.append(GL_MID).append(shift(-1));
        body.append(GL_RIGHT).append(shift(-1));
        c.add(Component.text(body.toString()).font(font).color(TextColor.color(tint)), plate);

        StringBuilder ic = new StringBuilder();
        ic.append(shift(-plate)).append(shift(3));
        if (iconChar != 0) {
            ic.append(iconChar).append(shift(padLeft - 3 - ICON_ADVANCE));
        } else {
            ic.append(shift(padLeft - 3));
        }
        c.add(Component.text(ic.toString()).font(font).color(WHITE), padLeft - plate);

        Component text = Component.text("").font(font);
        for (Component part : parts) text = text.append(part);
        c.add(text, textW);
    }

    /** Geser kursor n pixel (negatif = mundur); karakter spasi ada di font pack. */
    private static String shift(int n) {
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

    /** Lebar teks font default Minecraft (pixel GUI, sudah termasuk jarak antar huruf). */
    static int width(String s) {
        int w = 0;
        for (char c : s.toCharArray()) {
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
}
