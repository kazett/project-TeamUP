package id.teamup.gui;

import id.teamup.TeamUPPlugin;
import org.bukkit.entity.Player;

public class AdminMenu extends Menu {
    public AdminMenu(TeamUPPlugin plugin, Player viewer) {
        super(plugin, viewer, null);
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return "&7TeamUP &cAdmin";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        label(0, 0, "&e" + plugin.teams().all().size() + " &7TEAM TERDAFTAR");
        btn(9, Btn.ADD_TEAM, e -> new CreateTeamPickMenu(plugin, viewer, this).open());
        btn(14, Btn.LIST_TEAM, e -> new TeamListMenu(plugin, viewer, this, false).open());
        btn(18, Btn.WARN_TEAM, e -> new TeamListMenu(plugin, viewer, this, true).open());
        if (plugin.vaults().enabled()) {
            btn(27, Btn.ADM_BRANGKAS, e -> new VaultListMenu(plugin, viewer, this).open());
        }
        btn(23, Btn.CLOSE4, e -> viewer.closeInventory());
    }
}
