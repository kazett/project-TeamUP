package id.teamup.gui;

import id.teamup.Items;
import id.teamup.TeamUPPlugin;
import id.teamup.vault.Vault;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Admin: daftar brankas semua fraksi. */
public class VaultListMenu extends PagedMenu<Vault> {
    public VaultListMenu(TeamUPPlugin plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return "&7Brankas &cFraksi";
    }

    @Override
    protected List<Vault> entries() {
        return plugin.vaults().all();
    }

    @Override
    protected ItemStack render(Vault v) {
        return Items.make(Material.CHEST, "&e" + v.teamName() + " &7brangkas",
                "&7Level: &f" + v.level() + "&7/" + v.levelCount() + " &8(" + v.capacity() + " slot)",
                "&7Terisi: &f" + v.usedSlots() + "&7/" + v.capacity() + " slot",
                "&7Klik untuk kelola");
    }

    @Override
    protected void click(Vault v, InventoryClickEvent e) {
        new VaultAdminMenu(plugin, viewer, this, v).open();
    }
}
