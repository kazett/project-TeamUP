package id.teamup.gui;

import id.teamup.TeamUPPlugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public abstract class Menu implements InventoryHolder {
    protected final TeamUPPlugin plugin;
    protected final Player viewer;
    protected final Menu parent;
    private Inventory inv;
    private Inventory shown;
    private final Map<Integer, Btn> btns = new HashMap<>();
    private final List<GuiFont.Label> labels = new ArrayList<>();
    private String sig = "";
    private final Map<Integer, Consumer<InventoryClickEvent>> actions = new HashMap<>();

    protected Menu(TeamUPPlugin plugin, Player viewer, Menu parent) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.parent = parent;
    }

    protected abstract String title();

    /** Teks judul yang ditampilkan (bisa ditambah info halaman). */
    protected String titleText() {
        return title();
    }

    protected abstract int rows();

    protected abstract void build();

    /** Tema warna menu (warna background pack). */
    protected GuiTheme theme() {
        return GuiTheme.TEAM;
    }

    private Inventory create() {
        Component t = GuiFont.styled(theme(), rows(), actions.keySet(), btns, labels, titleText());
        return Bukkit.createInventory(this, rows() * 9, t);
    }

    private void rebuild() {
        actions.clear();
        btns.clear();
        labels.clear();
        inv = Bukkit.createInventory(this, rows() * 9);
        build();
    }

    private String signature() {
        return new TreeSet<>(actions.keySet()) + "|" + new java.util.TreeMap<>(btns) + "|" + titleText() + "|" + labels;
    }

    public void open() {
        rebuild();
        sig = signature();
        Inventory real = create();
        real.setContents(inv.getContents());
        inv = real;
        shown = real;
        viewer.openInventory(real);
        viewer.playSound(viewer.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 0.3f, 1.7f);
    }

    public void refresh() {
        Inventory old = shown;
        rebuild();
        String now = signature();
        if (old != null && now.equals(sig)) {
            old.setContents(inv.getContents());
            inv = old;
        } else {
            // tombol/judul berubah -> judul (background) harus dibuat ulang
            sig = now;
            Inventory real = create();
            real.setContents(inv.getContents());
            inv = real;
            shown = real;
            viewer.openInventory(real);
        }
    }

    protected void set(int slot, ItemStack item, Consumer<InventoryClickEvent> action) {
        inv.setItem(slot, item);
        if (action != null) actions.put(slot, action);
    }

    /** Tombol berlabel (gambar + teks dari pack). Memakai b.width() slot ke kanan mulai dari slot. */
    protected void btn(int slot, Btn b, Consumer<InventoryClickEvent> action) {
        if (slot % 9 + b.width() > 9) return;
        btns.put(slot, b);
        for (int i = 0; i < b.width(); i++) actions.put(slot + i, action);
    }

    /** Teks yang digambar di dalam menu (tanpa item Minecraft). line 0 = tengah, 1 = baris atas, 2 = baris bawah. */
    protected void label(int slot, int line, String text) {
        labels.add(new GuiFont.Label(slot, line, text));
    }

    protected void set(int slot, ItemStack item) {
        set(slot, item, null);
    }

    /** Background digambar resource pack, jadi tidak ada yang perlu diisi. */
    protected void fill(ItemStack ignored) {}

    public void handle(InventoryClickEvent e) {
        Consumer<InventoryClickEvent> a = actions.get(e.getSlot());
        if (a != null) {
            viewer.playSound(viewer.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.3f);
            a.accept(e);
        }
    }

    @Override
    public Inventory getInventory() {
        return shown != null ? shown : inv;
    }
}
