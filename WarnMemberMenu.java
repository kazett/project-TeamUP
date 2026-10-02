package id.teamup.gui;

import id.teamup.Items;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** Pilih pelaku (bisa banyak, online/offline). Klik kiri = pilih/batal, lalu konfirmasi. */
public class WarnMemberMenu extends PagedMenu<UUID> {
    private final Team team;
    private final int level;
    private final Set<UUID> selected = new LinkedHashSet<>();

    public WarnMemberMenu(TeamUPPlugin plugin, Player viewer, Menu parent, Team team, int level) {
        super(plugin, viewer, parent);
        this.team = team;
        this.level = level;
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.WARN;
    }

    @Override
    protected String title() {
        return "&8Warn " + level + " &0" + team.getName() + " &8- pilih pelaku";
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
        boolean sel = selected.contains(id);
        List<String> lore = new ArrayList<>();
        lore.add("&7Rank: &e" + plugin.teams().rankName(team, team.rankOf(id)));
        lore.add(op.isOnline() ? "&aOnline" : "&cOffline");
        lore.add("");
        lore.add(sel ? "&c&lDIPILIH sebagai pelaku" : "&eKlik kiri: &7pilih sebagai pelaku");
        ItemStack it = Items.head(op, (sel ? "&c" : "&f") + team.nameOf(id), lore);
        return sel ? Items.glow(it) : it;
    }

    @Override
    protected void click(UUID id, InventoryClickEvent e) {
        if (!selected.remove(id)) selected.add(id);
        refresh();
    }

    @Override
    protected int backSlot() {
        return 47;
    }

    @Override
    protected void extra() {
        btn(50, selected.isEmpty() ? Btn.OK2_OFF : Btn.OK2, e -> {
            if (selected.isEmpty()) {
                Msg.send(viewer, "&cPilih minimal 1 pelaku.");
                return;
            }
            plugin.prompts().ask(viewer, "Ketik alasan warn di chat (ketik &f- &7untuk alasan default):", text -> {
                if (plugin.teams().get(team.getName()) != team) {
                    Msg.send(viewer, "&cTeam sudah tidak ada.");
                    return;
                }
                String reason = text.equals("-") || text.isBlank() ? "Pelanggaran" : text.trim();
                List<UUID> culprits = new ArrayList<>();
                for (UUID u : selected) if (team.getMembers().containsKey(u)) culprits.add(u);
                if (culprits.isEmpty()) {
                    Msg.send(viewer, "&cPelaku tidak valid.");
                    return;
                }
                plugin.teams().warn(team, level, culprits, reason, viewer.getName());
                Msg.send(viewer, "&aWarn " + level + " dikirim ke team &e" + team.getName() + "&a.");
                new AdminMenu(plugin, viewer).open();
            });
        });
    }
}
