package id.teamup.gui;

import id.teamup.Items;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Anggota team yang sedang online. */
public class OnlineMenu extends PagedMenu<Player> {
    private final Team team;

    public OnlineMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team) {
        super(plugin, viewer, parent);
        this.team = team;
    }

    @Override
    protected String title() {
        return "&8Online &0" + team.getName();
    }

    @Override
    protected List<Player> entries() {
        return plugin.teams().onlineMembers(team);
    }

    @Override
    protected ItemStack render(Player p) {
        return Items.head(p, "&a" + p.getName(),
                List.of("&7Rank: &e" + plugin.teams().rankName(team, team.rankOf(p.getUniqueId())), "&aOnline"));
    }

    @Override
    protected void click(Player p, InventoryClickEvent e) {}
}
