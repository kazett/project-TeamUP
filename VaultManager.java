package id.teamup.vault;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Mengelola semua brankas fraksi: buat otomatis, simpan/muat, upgrade, arsip saat team dibubarkan. */
public class VaultManager {
    private final TeamUPPlugin plugin;
    private final Map<String, Vault> vaults = new HashMap<>();
    private final File dir;
    private final File archiveDir;
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> new Thread(r, "TeamUP-Vault-IO"));

    public VaultManager(TeamUPPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "vaults");
        this.archiveDir = new File(dir, "archive");
        dir.mkdirs();
        archiveDir.mkdirs();
    }

    TeamUPPlugin plugin() {
        return plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("vault.enabled", true);
    }

    // ---------- config ----------

    private List<Integer> levels() {
        List<Integer> src = plugin.getConfig().getIntegerList("vault.slots-per-level");
        List<Integer> out = new ArrayList<>();
        int prev = 0;
        for (int v : src) {
            if (v <= prev || v > Vault.HARD_MAX) continue; // harus naik terus
            out.add(v);
            prev = v;
            if (out.size() == 5) break;
        }
        if (out.isEmpty()) return List.of(20, 40, 60, 80, 100);
        return out;
    }

    public int levelCount() {
        return levels().size();
    }

    /** Jumlah slot untuk level (1-based). */
    public int slotsFor(int level) {
        List<Integer> l = levels();
        return l.get(Math.max(0, Math.min(level, l.size()) - 1));
    }

    int logMax() {
        return Math.max(50, Math.min(5000, plugin.getConfig().getInt("vault.log.max-entries", 500)));
    }

    public String formatTime(long ts) {
        ZoneId z;
        try {
            z = ZoneId.of(plugin.getConfig().getString("hud.timezone", "Asia/Jakarta"));
        } catch (Exception e) {
            z = ZoneId.of("Asia/Jakarta");
        }
        return DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(z).format(Instant.ofEpochMilli(ts));
    }

    // ---------- akses ----------

    private static String fileId(String key) {
        String safe = key.toLowerCase().replaceAll("[^a-z0-9_]", "_");
        if (safe.length() > 24) safe = safe.substring(0, 24);
        return safe + "-" + Integer.toHexString(key.hashCode());
    }

    public Vault get(Team t) {
        return vaults.get(t.getKey());
    }

    /** Semua brankas, urut nama team. */
    public List<Vault> all() {
        List<Vault> l = new ArrayList<>(vaults.values());
        l.sort(Comparator.comparing(Vault::teamName, String.CASE_INSENSITIVE_ORDER));
        return l;
    }

    /** Brankas milik team; dibuat otomatis kalau belum ada. */
    public Vault ensure(Team t) {
        Vault v = vaults.get(t.getKey());
        if (v != null) return v;
        v = new Vault(this, t.getKey(), t.getName(), fileId(t.getKey()));
        vaults.put(t.getKey(), v);
        v.addLog(new LogEntry(System.currentTimeMillis(), "SISTEM", LogEntry.SYSTEM, 'S', "CHEST", 0,
                "Brankas dibuat (Level 1, " + slotsFor(1) + " slot)"));
        save(v);
        return v;
    }

    public void ensureAll() {
        if (!enabled()) return;
        for (Team t : plugin.teams().all()) ensure(t);
    }

    public boolean canUse(Player p, Vault v) {
        Team t = plugin.teams().ofPlayer(p.getUniqueId());
        return t != null && t.getKey().equals(v.key()) && plugin.teams().hasPerm(t, p.getUniqueId(), TeamPerm.VAULT);
    }

    /** Buka brankas untuk anggota (hanya yang punya izin). */
    public void open(Player p, Team t, int page) {
        if (!enabled()) {
            Msg.send(p, "&cFitur brankas sedang dimatikan.");
            return;
        }
        Vault v = ensure(t);
        if (!canUse(p, v)) {
            Msg.send(p, "&cHanya Ketua / Wakil Ketua yang bisa membuka brankas fraksi.");
            return;
        }
        int pg = Math.max(0, Math.min(page, v.pageCount() - 1));
        p.openInventory(v.page(pg));
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.5f, 1.2f);
    }

    /** Upgrade ke level tertentu (tidak bisa turun). Return pesan error atau null kalau sukses. */
    public String upgrade(Vault v, int target, String by) {
        v.flush();
        if (target <= v.level()) return "Brankas sudah level " + v.level() + " (tidak bisa downgrade).";
        if (target > levelCount()) return "Level tertinggi adalah " + levelCount() + ".";
        v.resetPages("&eBrankas di-upgrade, silakan buka lagi."); // simpan perubahan terakhir + tutup tampilan lama DULU
        v.setLevel(target);
        v.addLog(new LogEntry(System.currentTimeMillis(), "SISTEM", LogEntry.SYSTEM, 'S', "NETHER_STAR", 0,
                "Brankas di-upgrade ke Level " + target + " (" + slotsFor(target) + " slot) oleh " + by));
        save(v);
        Team t = plugin.teams().get(v.teamName());
        if (t != null) {
            plugin.teams().broadcast(t, Msg.c(Msg.PREFIX + "&aBrankas fraksi di-upgrade ke &eLevel " + target
                    + " &7(" + slotsFor(target) + " slot)"));
        }
        return null;
    }

    /** Dipanggil saat config dimuat ulang: kapasitas bisa berubah, cache halaman dibuang. */
    public void onReload() {
        for (Vault v : vaults.values()) v.resetPages("&eBrankas dimuat ulang, silakan buka lagi.");
    }

    // ---------- simpan / muat ----------

    private Path ymlPath(Vault v) {
        return new File(dir, v.id() + ".yml").toPath();
    }

    private Path logPath(Vault v) {
        return new File(dir, v.id() + ".log").toPath();
    }

    private String serialize(Vault v) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("key", v.key());
        y.set("team", v.teamName());
        y.set("level", v.level());
        ItemStack[] raw = v.rawItems();
        for (int i = 0; i < raw.length; i++) if (raw[i] != null) y.set("items." + i, raw[i]);
        return y.saveToString();
    }

    private void atomicWrite(Path target, String text) {
        try {
            Path tmp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(tmp, text, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan brankas " + target.getFileName() + ": " + e);
        }
    }

    /** Simpan isi brankas (diserialisasi di thread utama, ditulis di thread IO). */
    void save(Vault v) {
        v.flush();
        String text = serialize(v);
        Path p = ymlPath(v);
        io.execute(() -> atomicWrite(p, text));
    }

    void appendLog(Vault v, LogEntry e) {
        Path p = logPath(v);
        String line = e.toLine() + "\n";
        io.execute(() -> {
            try {
                Files.writeString(p, line, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException ex) {
                plugin.getLogger().warning("Gagal menulis log brankas: " + ex);
            }
        });
    }

    void rewriteLog(Vault v, List<LogEntry> entries) {
        Path p = logPath(v);
        StringBuilder sb = new StringBuilder();
        for (LogEntry e : entries) sb.append(e.toLine()).append('\n');
        String text = sb.toString();
        io.execute(() -> atomicWrite(p, text));
    }

    /** Pindahkan file log lama ke folder arsip (sebelum di-clear). */
    void archiveLog(Vault v) {
        Path p = logPath(v);
        boolean keep = plugin.getConfig().getBoolean("vault.log.archive-on-clear", true);
        File ad = archiveDir;
        String prefix = v.id() + "-";
        io.execute(() -> {
            try {
                if (Files.exists(p)) {
                    if (keep) {
                        Path to = new File(ad, prefix + System.currentTimeMillis() + ".log").toPath();
                        Files.move(p, to, StandardCopyOption.REPLACE_EXISTING);
                        File[] old = ad.listFiles((d, n) -> n.startsWith(prefix) && n.endsWith(".log"));
                        if (old != null && old.length > 10) {
                            java.util.Arrays.sort(old, Comparator.comparing(File::getName));
                            for (int i = 0; i < old.length - 10; i++) old[i].delete();
                        }
                    } else {
                        Files.deleteIfExists(p);
                    }
                }
            } catch (IOException ex) {
                plugin.getLogger().warning("Gagal mengarsipkan log brankas: " + ex);
            }
        });
    }

    public void loadAll() {
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                String key = y.getString("key");
                if (key == null) continue;
                Team t = plugin.teams().get(key);
                if (t == null) {
                    plugin.getLogger().warning("Brankas " + f.getName() + " tidak punya team (file dibiarkan, tidak dihapus).");
                    continue;
                }
                String id = f.getName().substring(0, f.getName().length() - 4);
                Vault v = new Vault(this, t.getKey(), t.getName(), id);
                v.setLevel(Math.max(1, Math.min(y.getInt("level", 1), levelCount())));
                ConfigurationSection sec = y.getConfigurationSection("items");
                if (sec != null) {
                    for (String k : sec.getKeys(false)) {
                        try {
                            int idx = Integer.parseInt(k);
                            ItemStack it = sec.getItemStack(k);
                            if (idx >= 0 && idx < Vault.HARD_MAX && it != null) v.rawItems()[idx] = it;
                        } catch (NumberFormatException ignored) {
                            // kunci aneh dilewati
                        }
                    }
                }
                v.computeHighWater();
                loadLog(v);
                vaults.put(t.getKey(), v);
            } catch (Exception e) {
                plugin.getLogger().severe("Gagal memuat brankas " + f.getName() + ": " + e);
            }
        }
    }

    private void loadLog(Vault v) {
        Path p = logPath(v);
        if (!Files.exists(p)) return;
        try {
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            int n = 0;
            for (String line : lines) {
                LogEntry e = LogEntry.parse(line);
                if (e != null) {
                    v.addLogLoaded(e);
                    n++;
                }
            }
            v.setDiskLines(lines.size());
            while (n > logMax()) {
                // hanya batas memori; file dirapikan otomatis saat ada log baru
                n--;
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Gagal membaca log brankas " + p.getFileName() + ": " + e);
        }
    }

    /** Team dibubarkan: isi dan log brankas DIARSIPKAN (tidak dihapus) supaya barang bisa dilacak admin. */
    public void archive(Team t) {
        Vault v = vaults.remove(t.getKey());
        if (v == null) return;
        v.flush();
        v.resetPages(null);
        int used = 0;
        for (ItemStack it : v.rawItems()) if (it != null) used++;
        String text = serialize(v);
        Path y = ymlPath(v);
        Path lg = logPath(v);
        long now = System.currentTimeMillis();
        File ad = archiveDir;
        String base = v.id() + "-" + now;
        io.execute(() -> {
            try {
                atomicWrite(new File(ad, base + ".yml").toPath(), text);
                Files.deleteIfExists(y);
                if (Files.exists(lg)) Files.move(lg, new File(ad, base + ".log").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                plugin.getLogger().severe("Gagal mengarsipkan brankas " + base + ": " + e);
            }
        });
        if (used > 0) {
            plugin.getLogger().warning("Brankas fraksi " + t.getName() + " diarsipkan berisi " + used
                    + " slot barang -> plugins/TeamUP/vaults/archive/" + base + ".yml");
        }
    }

    public void shutdown() {
        for (Vault v : new ArrayList<>(vaults.values())) {
            v.flush();
            save(v);
        }
        io.shutdown();
        try {
            if (!io.awaitTermination(20, TimeUnit.SECONDS)) {
                plugin.getLogger().severe("Penyimpanan brankas belum selesai (timeout)!");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public UUID system() {
        return LogEntry.SYSTEM;
    }
}
