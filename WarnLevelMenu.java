package id.teamup.gui;

import id.teamup.Items;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class WarnLevelMenu extends Menu {
    private final Team team;

    public WarnLevelMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team) {
        super(plugin, viewer, parent);
        this.team = team;
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.WARN;
    }

    @Override
    protected String title() {
        return "&8Warn &0" + team.getName() + " &8- pilih level";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        List<Integer> days = plugin.getConfig().getIntegerList("warn-ban-days");
        List<String> lore = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int d = i < days.size() ? days.get(i) : i + 1;
            lore.add((i == 2 ? "&c" : "&e") + "Level " + (i + 1) + "&7: pelaku di-ban &f" + d + " hari &7+ 1 strike");
        }
        lore.add("&7Team tidak dibubarkan otomatis (admin yang putuskan)");
        lore.add("");
        lore.add("&7Pilih level, lalu pilih pelaku");
        set(4, Items.make(Material.PAPER, "&6Warn " + team.getName(), lore));

        Btn[] levels = {Btn.LV1, Btn.LV2, Btn.LV3};
        int[] slots = {18, 21, 24};
        for (int i = 0; i < 3; i++) {
            int level = i + 1;
            btn(slots[i], levels[i], e -> new WarnMemberMenu(plugin, viewer, this, team, level).open());
        }
        btn(48, Btn.BACK3, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
    }
}
