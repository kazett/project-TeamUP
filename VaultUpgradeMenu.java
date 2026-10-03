package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.vault.Vault;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Admin: pilih level brankas. Level yang sudah terbuka bertanda centang dan tidak bisa diturunkan. */
public class VaultUpgradeMenu extends Menu {
    private static final Btn[] DONE = {Btn.VL1_DONE, Btn.VL2_DONE, Btn.VL3_DONE, Btn.VL4_DONE, Btn.VL5_DONE};
    private static final Btn[] OPEN = {Btn.VL1_OPEN, Btn.VL2_OPEN, Btn.VL3_OPEN, Btn.VL4_OPEN, Btn.VL5_OPEN};
    private final Vault vault;

    public VaultUpgradeMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Vault vault) {
        super(plugin, viewer, parent);
        this.vault = vault;
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return "&7Upgrade &c" + vault.teamName();
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        btn(0, Btn.BACK3, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
        set(4, Items.make(Material.CHEST, "&e" + vault.teamName(),
                "&7Level sekarang: &f" + vault.level() + "&7/" + vault.levelCount(),
                "&7Kapasitas: &f" + vault.capacity() + " slot",
                "&8Upgrade tidak bisa di-downgrade"));
        int count = Math.min(5, vault.levelCount());
        for (int lv = 1; lv <= count; lv++) {
            final int level = lv;
            int base = lv * 9 + 1;
            int slots = plugin.vaults().slotsFor(lv);
            boolean done = lv <= vault.level();
            if (done) {
                btn(base, DONE[lv - 1], e -> Msg.send(viewer, "&7Level " + level + " sudah terbuka."));
                set(base + 5, Items.glow(Items.make(Material.CHEST, "&a" + slots + " slot",
                        "&aTerbuka" + (lv == vault.level() ? " &7(level aktif)" : ""))));
            } else {
                btn(base, OPEN[lv - 1], e -> new ConfirmMenu(plugin, viewer, this,
                        "&eUpgrade ke Level " + level + "?",
                        slots + " slot, tidak bisa di-downgrade", () -> {
                    String err = plugin.vaults().upgrade(vault, level, viewer.getName());
                    if (err != null) Msg.send(viewer, "&c" + err);
                    else Msg.send(viewer, "&aBrankas " + vault.teamName() + " di-upgrade ke Level " + level + ".");
                    new VaultUpgradeMenu(plugin, viewer, parent, vault).open();
                }).open());
                set(base + 5, Items.make(Material.ENDER_CHEST, "&e" + slots + " slot", "&7Klik tombol untuk upgrade"));
            }
        }
    }
}
