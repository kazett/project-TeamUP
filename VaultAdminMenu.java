package id.teamup.gui;

import id.teamup.Items;
import id.teamup.TeamUPPlugin;
import id.teamup.vault.Vault;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Admin: HISTORY / VIEW / UPGRADE untuk satu brankas fraksi. */
public class VaultAdminMenu extends Menu {
    private final Vault vault;

    public VaultAdminMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Vault vault) {
        super(plugin, viewer, parent);
        this.vault = vault;
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return "&7Brankas &c" + vault.teamName();
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        set(4, Items.make(Material.CHEST, "&e" + vault.teamName(),
                "&7Level: &f" + vault.level() + "&7/" + vault.levelCount(),
                "&7Kapasitas: &f" + vault.capacity() + " slot",
                "&7Terisi: &f" + vault.usedSlots() + " slot",
                "&7Log: &f" + vault.logsNewestFirst().size() + " catatan"));
        btn(9, Btn.HISTORY, e -> new VaultLogMenu(plugin, viewer, this, vault, true).open());
        btn(14, Btn.VIEW, e -> new VaultViewMenu(plugin, viewer, this, vault).open());
        btn(18, Btn.UPGRADE, e -> new VaultUpgradeMenu(plugin, viewer, this, vault).open());
        btn(23, Btn.BACK4, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
    }
}
