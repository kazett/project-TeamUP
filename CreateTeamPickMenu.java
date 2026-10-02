package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Admin pilih player online tanpa team, lalu ketik nama team di chat. */
public class CreateTeamPickMenu extends PagedMenu<Player> {
    public CreateTeamPickMenu(TeamUPPlugin plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return "&8Add Team - pilih ketua";
    }

    @Override
    protected List<Player> entries() {
        List<Player> l = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (plugin.teams().ofPlayer(p.getUniqueId()) == null) l.add(p);
        }
        l.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return l;
    }

    @Override
    protected ItemStack render(Player p) {
        return Items.head(p, "&f" + p.getName(), List.of("&eKlik: &7jadikan ketua team baru"));
    }

    @Override
    protected void click(Player leader, InventoryClickEvent e) {
        if (!leader.isOnline()) {
            refresh();
            return;
        }
        plugin.prompts().ask(viewer, "Ketik nama team untuk ketua &e" + leader.getName()
                + " &7(2-" + plugin.teams().maxNameLength() + " karakter, huruf/angka/_/spasi):", text -> {
            String name = text.trim();
            String err = plugin.teams().create(name, leader.getUniqueId(), leader.getName());
            if (err != null) {
                Msg.send(viewer, "&c" + err);
                open();
                return;
            }
            Msg.send(viewer, "&aTeam &e" + name + " &adibuat dengan ketua &e" + leader.getName() + "&a.");
            if (leader.isOnline()) Msg.send(leader, "&aTeam &e" + name + " &asudah dibuat. Kamu adalah &eKETUA&a! Buka menu: &f/teamup");
            new AdminMenu(plugin, viewer).open();
        });
    }
}
