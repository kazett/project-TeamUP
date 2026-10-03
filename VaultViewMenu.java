package id.teamup.gui;

import id.teamup.TeamUPPlugin;
import id.teamup.vault.Vault;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Admin: lihat isi brankas (hanya lihat, semua klik dibatalkan; yang ditampilkan salinan). */
public class VaultViewMenu extends PagedMenu<ItemStack> {
    private final Vault vault;

    public VaultViewMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Vault vault) {
        super(plugin, viewer, parent);
        this.vault = vault;
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return "&7Isi &c" + vault.teamName();
    }

    @Override
    protected List<ItemStack> entries() {
        return vault.occupied();
    }

    @Override
    protected ItemStack render(ItemStack entry) {
        return entry.clone();
    }

    @Override
    protected void click(ItemStack entry, InventoryClickEvent e) {
        // hanya lihat
    }
}
