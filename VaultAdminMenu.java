package id.teamup.gui;

import id.teamup.TeamUPPlugin;
import id.teamup.vault.Vault;
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
        label(0, 1, "&eLEVEL " + vault.level() + "&7/" + vault.levelCount() + "  &7LOG &f" + vault.logsNewestFirst().size());
        label(0, 2, "&f" + vault.usedSlots() + "&7/" + vault.capacity() + " SLOT TERISI");
        btn(9, Btn.HISTORY, e -> new VaultLogMenu(plugin, viewer, this, vault, true).open());
        btn(14, Btn.VIEW, e -> new VaultViewMenu(plugin, viewer, this, vault).open());
        btn(18, Btn.UPGRADE, e -> new VaultUpgradeMenu(plugin, viewer, this, vault).open());
        btn(23, Btn.BACK4, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
    }
}
