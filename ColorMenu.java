package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ColorMenu extends Menu {
    private record Opt(NamedTextColor color, String code, String label, Material mat) {}

    private static final Opt[] OPTS = {
        new Opt(NamedTextColor.BLACK, "&0", "Hitam", Material.BLACK_CONCRETE),
        new Opt(NamedTextColor.DARK_BLUE, "&1", "Biru Tua", Material.BLUE_CONCRETE),
        new Opt(NamedTextColor.DARK_GREEN, "&2", "Hijau Tua", Material.GREEN_CONCRETE),
        new Opt(NamedTextColor.DARK_AQUA, "&3", "Cyan Tua", Material.CYAN_CONCRETE),
        new Opt(NamedTextColor.DARK_RED, "&4", "Merah Tua", Material.RED_CONCRETE),
        new Opt(NamedTextColor.DARK_PURPLE, "&5", "Ungu", Material.PURPLE_CONCRETE),
        new Opt(NamedTextColor.GOLD, "&6", "Emas", Material.ORANGE_CONCRETE),
        new Opt(NamedTextColor.GRAY, "&7", "Abu-abu", Material.LIGHT_GRAY_CONCRETE),
        new Opt(NamedTextColor.DARK_GRAY, "&8", "Abu Tua", Material.GRAY_CONCRETE),
        new Opt(NamedTextColor.BLUE, "&9", "Biru", Material.LIGHT_BLUE_CONCRETE),
        new Opt(NamedTextColor.GREEN, "&a", "Hijau", Material.LIME_CONCRETE),
        new Opt(NamedTextColor.AQUA, "&b", "Cyan", Material.LIGHT_BLUE_CONCRETE_POWDER),
        new Opt(NamedTextColor.RED, "&c", "Merah", Material.RED_CONCRETE_POWDER),
        new Opt(NamedTextColor.LIGHT_PURPLE, "&d", "Pink", Material.MAGENTA_CONCRETE),
        new Opt(NamedTextColor.YELLOW, "&e", "Kuning", Material.YELLOW_CONCRETE),
        new Opt(NamedTextColor.WHITE, "&f", "Putih", Material.WHITE_CONCRETE)
    };

    private final Team team;
    private final boolean admin;

    public ColorMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, boolean admin) {
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
        return "&8Warna &0" + team.getName();
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        for (int i = 0; i < OPTS.length; i++) {
            Opt o = OPTS[i];
            boolean now = team.getColor() == o.color();
            var it = Items.make(o.mat(), o.code() + o.label(),
                    now ? "&aWarna team sekarang" : "&eKlik: &7pakai warna ini");
            set(9 + (i / 8) * 9 + i % 8, now ? Items.glow(it) : it, e -> {
                if (!admin && !team.getLeader().equals(viewer.getUniqueId())) return;
                team.setColor(o.color());
                plugin.teams().save();
                plugin.tags().refresh(team);
                Msg.send(viewer, "&7Warna team jadi " + o.code() + o.label() + "&7.");
                refresh();
            });
        }
        btn(48, Btn.BACK3, e -> parent.open());
    }
}
