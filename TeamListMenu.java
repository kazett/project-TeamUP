package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TeamListMenu extends PagedMenu<Team> {
    private final boolean warnMode;

    public TeamListMenu(TeamUPPlugin plugin, Player viewer, Menu parent, boolean warnMode) {
        super(plugin, viewer, parent);
        this.warnMode = warnMode;
    }

    @Override
    protected GuiTheme theme() {
        return warnMode ? GuiTheme.WARN : GuiTheme.ADMIN;
    }

    @Override
    protected String title() {
        return warnMode ? "&8Warn Team - pilih team" : "&8List Team";
    }

    @Override
    protected List<Team> entries() {
        return plugin.teams().all();
    }

    @Override
    protected ItemStack render(Team t) {
        int on = plugin.teams().onlineMembers(t).size();
        List<String> lore = warnMode
                ? List.of("&7Ketua: &f" + t.nameOf(t.getLeader()), "&7Member: &f" + t.getMembers().size()
                        + " &7(online " + on + ")", "&7Strike: &c" + t.getStrikes(), "", "&eKlik untuk warn")
                : List.of("&7Ketua: &f" + t.nameOf(t.getLeader()), "&7Member: &f" + t.getMembers().size()
                        + " &7(online " + on + ")", "&7Strike: &c" + t.getStrikes(), "",
                        "&eKlik kiri: &7buka sebagai ketua", "&eKlik kanan: &7warn", "&cShift + kanan: &7hapus");
        return Items.make(Material.WHITE_BANNER, "&b" + t.getName(), lore);
    }

    @Override
    protected void click(Team t, InventoryClickEvent e) {
        if (plugin.teams().get(t.getName()) != t) {
            refresh();
            return;
        }
        if (warnMode) {
            new WarnLevelMenu(plugin, viewer, this, t).open();
            return;
        }
        ClickType ct = e.getClick();
        if (ct == ClickType.SHIFT_RIGHT) {
            new ConfirmMenu(plugin, viewer, this, "&4Hapus " + t.getName() + "?",
                    "Team akan dihapus permanen", () -> {
                plugin.teams().disband(t, true);
                Msg.send(viewer, "&7Team &e" + t.getName() + " &7dihapus.");
                new TeamListMenu(plugin, viewer, parent, false).open();
            }).open();
        } else if (ct == ClickType.RIGHT) {
            new WarnLevelMenu(plugin, viewer, this, t).open();
        } else if (ct == ClickType.LEFT) {
            new TeamMenu(plugin, viewer, this, t, true).open();
        }
    }
}
