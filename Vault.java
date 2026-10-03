package id.teamup.vault;

import id.teamup.Items;
import id.teamup.gui.Btn;
import id.teamup.gui.GuiFont;
import id.teamup.gui.GuiTheme;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Brankas satu fraksi. Isi disimpan di array {@link #items}; tiap halaman GUI adalah satu Inventory yang
 * DIPAKAI BERSAMA semua pemain yang membuka brankas ini (jadi tidak mungkin dobel/duplikat antar pemain).
 * Perubahan dibandingkan dengan isi array ({@link #flush}) lalu dicatat ke log atas nama pemain yang melakukannya.
 */
public final class Vault {
    public static final int PER_PAGE = 45;
    public static final int HARD_MAX = 200;

    private final VaultManager mgr;
    private final String key;
    private final String id;
    private String teamName;
    private int level = 1;
    private final ItemStack[] items = new ItemStack[HARD_MAX];
    private int highWater = 0;
    private final ArrayDeque<LogEntry> log = new ArrayDeque<>();
    private final Map<Integer, Inventory> pages = new HashMap<>();
    private UUID pendingUuid;
    private String pendingName;
    private boolean flushQueued;
    private boolean saveQueued;
    private int diskLines;

    Vault(VaultManager mgr, String key, String teamName, String id) {
        this.mgr = mgr;
        this.key = key;
        this.teamName = teamName;
        this.id = id;
    }

    // ---------- info ----------

    public String key() {
        return key;
    }

    public String id() {
        return id;
    }

    public String teamName() {
        return teamName;
    }

    void setTeamName(String n) {
        this.teamName = n;
    }

    public int level() {
        return level;
    }

    void setLevel(int l) {
        this.level = l;
    }

    public int levelCount() {
        return mgr.levelCount();
    }

    public int capacity() {
        return Math.min(HARD_MAX, Math.max(mgr.slotsFor(level), highWater));
    }

    public int pageCount() {
        return Math.max(1, (capacity() + PER_PAGE - 1) / PER_PAGE);
    }

    ItemStack[] rawItems() {
        return items;
    }

    void computeHighWater() {
        highWater = 0;
        for (int i = 0; i < HARD_MAX; i++) if (items[i] != null) highWater = i + 1;
    }

    /** Jumlah slot yang terisi. */
    public int usedSlots() {
        flush();
        int n = 0;
        int cap = capacity();
        for (int i = 0; i < cap; i++) if (items[i] != null) n++;
        return n;
    }

    /** Salinan semua barang yang ada (urut sesuai slot). */
    public List<ItemStack> occupied() {
        flush();
        List<ItemStack> l = new ArrayList<>();
        int cap = capacity();
        for (int i = 0; i < cap; i++) if (items[i] != null) l.add(items[i].clone());
        return l;
    }

    // ---------- halaman GUI ----------

    private NamespacedKey lockKey() {
        return new NamespacedKey(mgr.plugin(), "vault_locked");
    }

    /** Penanda slot terkunci (diberi tanda tersembunyi supaya tidak pernah dianggap barang isi brankas). */
    private ItemStack lockedItem() {
        ItemStack it = Items.make(Material.BARRIER, "&cTerkunci", "&7Upgrade brankas oleh admin", "&7untuk membuka slot ini");
        ItemMeta m = it.getItemMeta();
        m.getPersistentDataContainer().set(lockKey(), PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    private boolean isLocked(ItemStack it) {
        return it != null && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(lockKey(), PersistentDataType.BYTE);
    }

    private Component buildTitle(int pg) {
        int total = pageCount();
        Map<Integer, Btn> btns = new HashMap<>();
        if (pg > 0) btns.put(45, Btn.PREV2);
        btns.put(47, Btn.LOG);
        if (pg < total - 1) btns.put(52, Btn.NEXT2);
        List<Integer> plates = new ArrayList<>();
        int cap = capacity();
        for (int s = 0; s < PER_PAGE; s++) if (pg * PER_PAGE + s < cap) plates.add(s);
        String text = GuiFont.clip("&f" + teamName, 14) + " &7(" + (pg + 1) + "/" + total + ")";
        return GuiFont.styled(GuiTheme.TEAM, 6, plates, btns, text);
    }

    /** Inventory bersama untuk halaman pg (dibuat kalau belum ada). */
    public Inventory page(int pg) {
        Inventory inv = pages.get(pg);
        if (inv != null) return inv;
        VaultHolder holder = new VaultHolder(this, pg);
        inv = Bukkit.createInventory(holder, 54, buildTitle(pg));
        holder.setInventory(inv);
        int cap = capacity();
        for (int s = 0; s < PER_PAGE; s++) {
            int idx = pg * PER_PAGE + s;
            if (idx < cap) inv.setItem(s, items[idx] == null ? null : items[idx].clone());
            else inv.setItem(s, lockedItem());
        }
        pages.put(pg, inv);
        return inv;
    }

    /** Tutup semua yang sedang melihat brankas ini dan buang cache halaman (setelah upgrade/hapus/reload). */
    void resetPages(String message) {
        flush();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof VaultHolder h && h.vault() == this) {
                p.closeInventory();
                if (message != null) p.sendMessage(id.teamup.Msg.c(id.teamup.Msg.PREFIX + message));
            }
        }
        pages.clear();
    }

    // ---------- perubahan isi + log ----------

    private static ItemStack norm(ItemStack it) {
        return it == null || it.getType().isAir() || it.getAmount() <= 0 ? null : it;
    }

    private static boolean same(ItemStack a, ItemStack b) {
        if (a == null || b == null) return a == b;
        return a.getAmount() == b.getAmount() && a.isSimilar(b);
    }

    private static ItemStack one(ItemStack it) {
        ItemStack c = it.clone();
        c.setAmount(1);
        return c;
    }

    /** Catat bahwa pemain p akan/ sedang mengubah brankas (dipanggil SEBELUM klik diproses). */
    public void note(Player p) {
        if (pendingUuid != null && !pendingUuid.equals(p.getUniqueId())) flush();
        pendingUuid = p.getUniqueId();
        pendingName = p.getName();
        if (!flushQueued) {
            flushQueued = true;
            Bukkit.getScheduler().runTask(mgr.plugin(), () -> {
                flushQueued = false;
                flush();
            });
        }
    }

    /** Bandingkan isi inventory dengan data, simpan perubahan, dan tulis log atas nama pelaku. */
    public void flush() {
        UUID who = pendingUuid;
        String whoName = pendingName;
        pendingUuid = null;
        pendingName = null;
        int cap = capacity();
        Map<ItemStack, Integer> net = new LinkedHashMap<>();
        boolean changed = false;
        for (Map.Entry<Integer, Inventory> en : pages.entrySet()) {
            int pg = en.getKey();
            Inventory inv = en.getValue();
            for (int s = 0; s < PER_PAGE; s++) {
                int idx = pg * PER_PAGE + s;
                if (idx >= cap) break;
                ItemStack rawNow = inv.getItem(s);
                if (isLocked(rawNow)) continue; // slot masih terkunci di tampilan lama
                ItemStack now = norm(rawNow);
                ItemStack old = items[idx];
                if (same(old, now)) continue;
                changed = true;
                if (old != null) net.merge(one(old), -old.getAmount(), Integer::sum);
                if (now != null) net.merge(one(now), now.getAmount(), Integer::sum);
                items[idx] = now == null ? null : now.clone();
                if (now != null && idx + 1 > highWater) highWater = idx + 1;
            }
        }
        if (!changed) return;
        UUID u = who != null ? who : LogEntry.SYSTEM;
        String n = whoName != null ? whoName : "SISTEM";
        for (Map.Entry<ItemStack, Integer> en : net.entrySet()) {
            int amount = en.getValue();
            if (amount == 0) continue; // cuma pindah slot, tidak perlu dicatat
            addLog(new LogEntry(System.currentTimeMillis(), n, u, amount > 0 ? 'P' : 'T',
                    en.getKey().getType().name(), Math.abs(amount), prettyName(en.getKey())));
        }
        markDirty();
    }

    /**
     * Masukkan item (dari shift-click inventory pemain) ke brankas, mulai dari halaman startPage.
     * Hanya ke slot yang terbuka. Mengembalikan sisa yang tidak muat (null kalau semua masuk).
     */
    public ItemStack insert(int startPage, ItemStack stack) {
        ItemStack left = stack.clone();
        int total = pageCount();
        int cap = capacity();
        for (int pass = 0; pass < 2 && left.getAmount() > 0; pass++) {
            for (int k = 0; k < total && left.getAmount() > 0; k++) {
                int pg = (startPage + k) % total;
                Inventory inv = page(pg);
                for (int s = 0; s < PER_PAGE && left.getAmount() > 0; s++) {
                    if (pg * PER_PAGE + s >= cap) break;
                    ItemStack rawCur = inv.getItem(s);
                    if (isLocked(rawCur)) continue;
                    ItemStack cur = norm(rawCur);
                    if (pass == 0) {
                        if (cur != null && cur.isSimilar(left) && cur.getAmount() < cur.getMaxStackSize()) {
                            int can = Math.min(left.getAmount(), cur.getMaxStackSize() - cur.getAmount());
                            ItemStack merged = cur.clone();
                            merged.setAmount(cur.getAmount() + can);
                            inv.setItem(s, merged);
                            left.setAmount(left.getAmount() - can);
                        }
                    } else if (cur == null) {
                        int put = Math.min(left.getAmount(), left.getMaxStackSize());
                        ItemStack n = left.clone();
                        n.setAmount(put);
                        inv.setItem(s, n);
                        left.setAmount(left.getAmount() - put);
                    }
                }
            }
        }
        return left.getAmount() > 0 ? left : null;
    }

    static String prettyName(ItemStack it) {
        if (it.hasItemMeta() && it.getItemMeta().hasDisplayName()) {
            String s = PlainTextComponentSerializer.plainText().serialize(it.getItemMeta().displayName());
            if (!s.isBlank()) return s;
        }
        StringBuilder sb = new StringBuilder();
        for (String w : it.getType().name().toLowerCase().split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    // ---------- log ----------

    void addLogLoaded(LogEntry e) {
        log.addLast(e);
    }

    void setDiskLines(int n) {
        diskLines = n;
    }

    public void addLog(LogEntry e) {
        log.addLast(e);
        diskLines++;
        mgr.appendLog(this, e);
        int max = mgr.logMax();
        boolean pruned = false;
        while (log.size() > max) {
            log.removeFirst();
            pruned = true;
        }
        if (pruned && diskLines > max * 2) {
            diskLines = log.size();
            mgr.rewriteLog(this, new ArrayList<>(log));
        }
    }

    public List<LogEntry> logsNewestFirst() {
        flush();
        List<LogEntry> l = new ArrayList<>(log);
        java.util.Collections.reverse(l);
        return l;
    }

    /** Hapus log (log lama diarsipkan kalau diaktifkan di config), lalu catat siapa yang menghapus. */
    public void clearLog(String byName) {
        flush();
        log.clear();
        diskLines = 0;
        mgr.archiveLog(this);
        addLog(new LogEntry(System.currentTimeMillis(), "SISTEM", LogEntry.SYSTEM, 'S', "BOOK", 0,
                "Log dibersihkan oleh " + byName));
    }

    // ---------- simpan ----------

    void markDirty() {
        if (saveQueued) return;
        saveQueued = true;
        Bukkit.getScheduler().runTaskLater(mgr.plugin(), () -> {
            saveQueued = false;
            mgr.save(this);
        }, 20L);
    }
}
