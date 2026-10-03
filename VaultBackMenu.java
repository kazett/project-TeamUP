package id.teamup.gui;

import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import org.bukkit.entity.Player;

/** Penghubung tombol "kembali" dari menu log ke brankas (brankas sendiri bukan Menu biasa). */
public class VaultBackMenu extends Menu {
    private final Team team;
    private final int page;

    public VaultBackMenu(TeamUPPlugin plugin, Player viewer, Team team, int page) {
        super(plugin, viewer, null);
        this.team = team;
        this.page = page;
    }

    @Override
    public void open() {
        plugin.vaults().open(viewer, team, page);
    }

    @Override
    protected String title() {
        return "";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {}
}
