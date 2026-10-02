package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Rank;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Daftar anggota: klik anggota untuk membuka menu kelola. */
public class MembersMenu extends PagedMenu<UUID> {
    private final Team team;
    private final boolean admin;

    public MembersMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, boolean admin) {
        super(plugin, viewer, parent);
        this.team = team;
        this.admin = admin;
    }

    @Override
    protected String title() {
        return "&8Anggota &0" + team.getName();
    }

    private boolean canRank() {
        return admin || plugin.teams().hasPerm(team, viewer.getUniqueId(), TeamPerm.RANK);
    }

    private boolean canKick() {
        return admin || plugin.teams().hasPerm(team, viewer.getUniqueId(), TeamPerm.MANAGE_MEMBERS);
    }

    private boolean outranks(Rank target) {
        if (admin) return true;
        Rank mine = team.rankOf(viewer.getUniqueId());
        return mine != null && target.ordinal() > mine.ordinal();
    }

    @Override
    protected List<UUID> entries() {
        List<UUID> l = new ArrayList<>(team.getMembers().keySet());
        l.sort(Comparator.<UUID>comparingInt(u -> team.rankOf(u).ordinal())
                .thenComparing(u -> team.nameOf(u), String.CASE_INSENSITIVE_ORDER));
        return l;
    }

    @Override
    protected ItemStack render(UUID id) {
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        Rank r = team.rankOf(id);
        List<String> lore = new ArrayList<>();
        lore.add("&7Rank: &e" + plugin.teams().rankName(team, r));
        lore.add(op.isOnline() ? "&aOnline" : "&cOffline");
        if (r != Rank.LEADER && (canRank() || (canKick() && outranks(r)))) {
            lore.add("");
            lore.add("&eKlik: &7kelola anggota");
        }
        return Items.head(op, "&f" + team.nameOf(id), lore);
    }

    @Override
    protected void click(UUID id, InventoryClickEvent e) {
        Rank tr = team.rankOf(id);
        if (tr == null || tr == Rank.LEADER) return;
        if (!canRank() && !(canKick() && outranks(tr))) return;
        new MemberActionMenu(plugin, viewer, this, team, id, admin).open();
    }
}
