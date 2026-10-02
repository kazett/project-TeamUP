package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Rank;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class MemberActionMenu extends Menu {
    private final Team team;
    private final UUID target;
    private final boolean admin;

    public MemberActionMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, UUID target, boolean admin) {
        super(plugin, viewer, parent);
        this.team = team;
        this.target = target;
        this.admin = admin;
    }

    @Override
    protected String title() {
        return "&8Anggota &0" + team.nameOf(target);
    }

    @Override
    protected int rows() {
        return 6;
    }

    private boolean canRank() {
        return admin || plugin.teams().hasPerm(team, viewer.getUniqueId(), TeamPerm.RANK);
    }

    private boolean canKick(Rank tr) {
        if (!admin && !plugin.teams().hasPerm(team, viewer.getUniqueId(), TeamPerm.MANAGE_MEMBERS)) return false;
        if (admin) return true;
        Rank mine = team.rankOf(viewer.getUniqueId());
        return mine != null && tr.ordinal() > mine.ordinal();
    }

    @Override
    protected void build() {
        Rank tr = team.rankOf(target);
        if (tr == null) {
            btn(48, Btn.BACK3, e -> parent.open());
            return;
        }
        OfflinePlayer op = Bukkit.getOfflinePlayer(target);
        List<String> lore = new ArrayList<>();
        lore.add("&7Rank: &e" + plugin.teams().rankName(team, tr));
        lore.add(op.isOnline() ? "&aOnline" : "&cOffline");
        set(4, Items.head(op, "&f" + team.nameOf(target), lore));

        if (canRank()) {
            btn(9, Btn.NAIK, e -> changeRank(true));
            btn(14, Btn.TURUN, e -> changeRank(false));
        }
        if (canKick(tr)) {
            btn(18, Btn.KICK, e -> new ConfirmMenu(plugin, viewer, this, "&cKick " + team.nameOf(target) + "?",
                    "Anggota dikeluarkan dari team", this::kick).open());
        }
        btn(48, Btn.BACK3, e -> parent.open());
    }

    private void changeRank(boolean up) {
        Rank tr = team.rankOf(target);
        if (tr == null || tr == Rank.LEADER) return;
        Rank nr = up ? tr.prev() : tr.next();
        if (nr == null || nr == Rank.LEADER) {
            Msg.send(viewer, "&cRank sudah di batas.");
            return;
        }
        String err = plugin.teams().setRank(team, target, nr);
        if (err != null) {
            Msg.send(viewer, "&c" + err);
            return;
        }
        String name = team.nameOf(target);
        Msg.send(viewer, "&e" + name + " &7sekarang &a" + plugin.teams().rankName(team, nr) + "&7.");
        Player p = Bukkit.getPlayer(target);
        if (p != null) Msg.send(p, "&7Rank kamu di team menjadi &a" + plugin.teams().rankName(team, nr) + "&7.");
        refresh();
    }

    private void kick() {
        Rank tr = team.rankOf(target);
        if (tr == null || !canKick(tr)) {
            Msg.send(viewer, "&cKamu tidak bisa kick anggota ini.");
            parent.open();
            return;
        }
        String name = team.nameOf(target);
        plugin.teams().removeMember(team, target);
        Player kicked = Bukkit.getPlayer(target);
        if (kicked != null) Msg.send(kicked, "&cKamu dikeluarkan dari team &e" + team.getName() + "&c.");
        plugin.teams().broadcast(team, Msg.c(Msg.PREFIX + "&e" + name + " &7dikeluarkan dari team."));
        parent.open();
    }
}
