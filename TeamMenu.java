package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import id.teamup.vault.Vault;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/** Menu utama TeamUP. admin=true berarti dibuka admin sebagai ketua (semua izin). */
public class TeamMenu extends Menu {
    private final Team team;
    private final boolean admin;

    public TeamMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, boolean admin) {
        super(plugin, viewer, parent);
        this.team = team;
        this.admin = admin;
    }

    @Override
    protected GuiTheme theme() {
        return admin ? GuiTheme.ADMIN : GuiTheme.TEAM;
    }

    @Override
    protected String title() {
        return "&8TeamUP &0" + team.getName();
    }

    @Override
    protected int rows() {
        return 6;
    }

    private boolean can(TeamPerm p) {
        if (admin) return true;
        return plugin.teams().hasPerm(team, viewer.getUniqueId(), p);
    }

    @Override
    protected void build() {
        int online = plugin.teams().onlineMembers(team).size();
        set(4, Items.make(Material.BOOK, "&b" + team.getName(),
                List.of("&7Ketua: &f" + team.nameOf(team.getLeader()),
                        "&7Member: &f" + team.getMembers().size() + " &7(online " + online + ")",
                        "&7Strike: &c" + team.getStrikes())));

        btn(9, Btn.ANGGOTA, e -> new MembersMenu(plugin, viewer, this, team, admin).open());

        if (can(TeamPerm.VIEW_ONLINE)) {
            btn(14, Btn.ONLINE, e -> new OnlineMenu(plugin, viewer, this, team).open());
        }

        if (can(TeamPerm.MANAGE_MEMBERS)) {
            btn(18, Btn.TAMBAH, e -> new AddMemberMenu(plugin, viewer, this, team, admin).open());
        }

        boolean renameOk = admin || (team.getLeader().equals(viewer.getUniqueId()) && team.canRenameRanks());
        if (renameOk) {
            btn(23, Btn.NAMA_RANK, e -> new RankNamesMenu(plugin, viewer, this, team, admin).open());
        }

        if (admin || team.getLeader().equals(viewer.getUniqueId())) {
            btn(admin ? 41 : 27, Btn.COLOR, e -> new ColorMenu(plugin, viewer, this, team, admin).open());
        }

        // brankas fraksi: hanya yang punya izin (default Ketua + Wakil Ketua); admin melihat lewat menu admin
        if (plugin.vaults().enabled() && can(TeamPerm.VAULT)) {
            if (admin) {
                btn(50, Btn.BRANGKAS, e -> {
                    Vault v = plugin.vaults().ensure(team);
                    new VaultAdminMenu(plugin, viewer, this, v).open();
                });
            } else {
                btn(32, Btn.BRANGKAS, e -> plugin.vaults().open(viewer, team, 0));
            }
        }

        if (admin) {
            btn(27, Btn.WARN_TEAM, e -> new WarnLevelMenu(plugin, viewer, this, team).open());
            btn(32, Btn.HAPUS_TEAM, e -> new ConfirmMenu(plugin, viewer, this, "&cHapus " + team.getName() + "?",
                    "Team akan dihapus permanen", () -> {
                plugin.teams().disband(team, true);
                Msg.send(viewer, "&7Team &e" + team.getName() + " &7dihapus.");
                new AdminMenu(plugin, viewer).open();
            }).open());
            btn(36, team.canRenameRanks() ? Btn.IZIN_ON : Btn.IZIN_OFF, e -> {
                team.setCanRenameRanks(!team.canRenameRanks());
                plugin.teams().save();
                refresh();
            });
            if (parent != null) btn(45, Btn.BACK4, e -> parent.open());
        }
    }
}
