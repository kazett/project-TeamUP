package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Rank;
import id.teamup.model.Team;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Ganti nama rank (hanya kalau admin mengizinkan ketua). */
public class RankNamesMenu extends Menu {
    private final Team team;
    private final boolean admin;

    public RankNamesMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, boolean admin) {
        super(plugin, viewer, parent);
        this.team = team;
        this.admin = admin;
    }

    @Override
    protected String title() {
        return "&8Nama Rank &0" + team.getName();
    }

    @Override
    protected int rows() {
        return 6;
    }

    private boolean allowed() {
        if (admin) return true;
        return team.getLeader().equals(viewer.getUniqueId()) && team.canRenameRanks();
    }

    @Override
    protected void build() {
        Rank[] ranks = Rank.values();
        for (int i = 0; i < ranks.length; i++) {
            Rank r = ranks[i];
            set(18 + 2 + i, Items.make(Material.NAME_TAG, "&e" + plugin.teams().rankName(team, r),
                    "&7Posisi: &f" + (i + 1), "", "&aKlik untuk ganti nama"), e -> {
                if (!allowed()) {
                    Msg.send(viewer, "&cKamu tidak boleh mengganti nama rank.");
                    return;
                }
                plugin.prompts().ask(viewer, "Ketik nama baru untuk rank &e" + plugin.teams().rankName(team, r)
                        + " &7(maks 16 karakter, boleh &&-kode warna):", text -> {
                    String t = text.trim();
                    if (t.isEmpty() || t.length() > 16) {
                        Msg.send(viewer, "&cNama harus 1-16 karakter.");
                    } else {
                        plugin.teams().renameRank(team, r, t);
                        Msg.send(viewer, "&aNama rank diganti menjadi &f" + t + "&a.");
                    }
                    open();
                });
            });
        }
        btn(48, Btn.BACK3, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
    }
}
