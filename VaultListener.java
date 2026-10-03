package id.teamup.vault;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.gui.VaultBackMenu;
import id.teamup.gui.VaultLogMenu;
import id.teamup.model.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** Menjaga brankas tetap aman: hanya slot isi yang bisa dipakai, semua perubahan dicatat ke log. */
public class VaultListener implements Listener {
    private final TeamUPPlugin plugin;

    public VaultListener(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean stillAllowed(Player p, Vault v) {
        return plugin.vaults().enabled() && plugin.vaults().canUse(p, v);
    }

    private void kick(Player p, Vault v) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            v.flush();
            p.closeInventory();
            Msg.send(p, "&cKamu tidak punya akses ke brankas ini lagi.");
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof VaultHolder h)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Vault v = h.vault();

        // izin dicek ulang di setiap klik (misalnya pangkat diturunkan / dikeluarkan saat brankas terbuka)
        if (!stillAllowed(p, v)) {
            e.setCancelled(true);
            kick(p, v);
            return;
        }

        InventoryAction act = e.getAction();
        if (e.getClick() == ClickType.CREATIVE || act == InventoryAction.COLLECT_TO_CURSOR
                || act == InventoryAction.CLONE_STACK || act == InventoryAction.UNKNOWN) {
            e.setCancelled(true);
            return;
        }

        int raw = e.getRawSlot();
        if (raw >= 0 && raw < top.getSize()) {
            // klik di dalam brankas
            if (h.nav(raw) != VaultHolder.Nav.NONE) {
                e.setCancelled(true);
                nav(p, v, h, h.nav(raw));
                return;
            }
            if (!h.allowed(raw)) {
                e.setCancelled(true); // slot terkunci / area tombol
                return;
            }
            v.note(p);
            return;
        }

        if (raw >= top.getSize() && act == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            // shift-click dari inventory pemain: dimasukkan manual HANYA ke slot isi (tidak ke area tombol)
            e.setCancelled(true);
            ItemStack src = e.getCurrentItem();
            if (src == null || src.getType().isAir()) return;
            v.note(p);
            ItemStack left = v.insert(h.page(), src);
            e.setCurrentItem(left);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof VaultHolder h)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Vault v = h.vault();
        if (!stillAllowed(p, v)) {
            e.setCancelled(true);
            kick(p, v);
            return;
        }
        boolean touchesTop = false;
        for (int raw : e.getRawSlots()) {
            if (raw < top.getSize()) {
                touchesTop = true;
                if (!h.allowed(raw)) {
                    e.setCancelled(true);
                    return;
                }
            }
        }
        if (touchesTop) v.note(p);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof VaultHolder h) h.vault().flush();
    }

    private void nav(Player p, Vault v, VaultHolder h, VaultHolder.Nav n) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            Team t = plugin.teams().ofPlayer(p.getUniqueId());
            if (t == null) return;
            switch (n) {
                case PREV -> plugin.vaults().open(p, t, h.page() - 1);
                case NEXT -> plugin.vaults().open(p, t, h.page() + 1);
                case LOG -> new VaultLogMenu(plugin, p, new VaultBackMenu(plugin, p, t, h.page()), v, false).open();
                default -> { }
            }
        });
    }
}
