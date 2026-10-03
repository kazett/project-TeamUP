package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.vault.LogEntry;
import id.teamup.vault.Vault;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Riwayat brankas (siapa memasukkan / mengambil barang apa). Clear log hanya untuk yang berhak. */
public class VaultLogMenu extends Menu {
    private final Vault vault;
    private final boolean admin;
    private int page = 0;

    public VaultLogMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Vault vault, boolean admin) {
        super(plugin, viewer, parent);
        this.vault = vault;
        this.admin = admin;
    }

    @Override
    protected GuiTheme theme() {
        return admin ? GuiTheme.ADMIN : GuiTheme.TEAM;
    }

    @Override
    protected String title() {
        return "&7Log &f" + vault.teamName();
    }

    @Override
    protected String titleText() {
        int pages = Math.max(1, (vault.logsNewestFirst().size() + 44) / 45);
        return GuiFont.clip(title(), 20) + " &7(" + (Math.min(page, pages - 1) + 1) + "/" + pages + ")";
    }

    @Override
    protected int rows() {
        return 6;
    }

    private static String clean(String s) {
        return s == null ? "" : s.replace("&", "").replace("\u00a7", "");
    }

    private ItemStack render(LogEntry e) {
        Material m = Material.matchMaterial(e.material());
        if (m == null || m.isAir() || !m.isItem()) m = Material.PAPER;
        String when = plugin.vaults().formatTime(e.ts());
        String who = clean(e.name());
        String item = clean(e.item());
        return switch (e.action()) {
            case 'P' -> Items.make(m, "&a" + who + " &7masukin barang &e[" + e.amount() + "x " + item + "]",
                    "&7Aksi: &aMASUK brankas", "&7Waktu: &f" + when);
            case 'T' -> Items.make(m, "&c" + who + " &7ambil barang &e[" + e.amount() + "x " + item + "]",
                    "&7Aksi: &cKELUAR brankas", "&7Waktu: &f" + when);
            default -> Items.make(m, "&e" + item, "&7Catatan sistem", "&7Waktu: &f" + when);
        };
    }

    private boolean canClear() {
        return admin || plugin.vaults().canUse(viewer, vault);
    }

    @Override
    protected void build() {
        List<LogEntry> list = vault.logsNewestFirst();
        int pages = Math.max(1, (list.size() + 44) / 45);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < 45; i++) {
            int idx = page * 45 + i;
            if (idx >= list.size()) break;
            set(i, render(list.get(idx)));
        }
        if (list.isEmpty()) set(22, Items.make(Material.PAPER, "&7Belum ada riwayat"));
        if (page > 0) btn(45, Btn.PREV2, e -> { page--; refresh(); });
        btn(47, Btn.BACK3, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
        if (canClear()) {
            btn(50, Btn.CLEAR2, e -> {
                if (!canClear()) return;
                new ConfirmMenu(plugin, viewer, this, "&cClear log " + vault.teamName() + "?",
                        "Log lama diarsipkan, dicatat siapa yang menghapus", () -> {
                    if (!canClear()) return;
                    vault.clearLog(viewer.getName());
                    Msg.send(viewer, "&7Log brankas dibersihkan.");
                    page = 0;
                    open();
                }).open();
            });
        }
        if (page < pages - 1) btn(52, Btn.NEXT2, e -> { page++; refresh(); });
    }
}
