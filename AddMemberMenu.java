package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Pilih player online tanpa team; klik kiri/kanan = kirim undangan. */
public class AddMemberMenu extends PagedMenu<Player> {
    private final Team team;
    private final boolean admin;

    public AddMemberMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, boolean admin) {
        super(plugin, viewer, parent);
        this.team = team;
        this.admin = admin;
    }

    @Override
    protected String title() {
        return "&8Undang ke &0" + team.getName();
    }

    @Override
    protected List<Player> entries() {
        List<Player> l = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(viewer)) continue;
            if (plugin.teams().ofPlayer(p.getUniqueId()) != null) continue;
            l.add(p);
        }
        l.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return l;
    }

    @Override
    protected ItemStack render(Player p) {
        return Items.head(p, "&f" + p.getName(), List.of("&eKlik kiri / kanan: &7kirim undangan"));
    }

    @Override
    protected void click(Player target, InventoryClickEvent e) {
        if (!admin && !plugin.teams().hasPerm(team, viewer.getUniqueId(), TeamPerm.MANAGE_MEMBERS)) {
            Msg.send(viewer, "&cKamu tidak punya izin.");
            return;
        }
        if (!target.isOnline()) {
            refresh();
            return;
        }
        String err = plugin.invites().invite(team, viewer, target);
        if (err != null) Msg.send(viewer, "&c" + err);
        else Msg.send(viewer, "&7Undangan dikirim ke &e" + target.getName() + "&7.");
        refresh();
    }
}
