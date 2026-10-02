package id.teamup.command;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.gui.TeamMenu;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class TeamUPCommand implements CommandExecutor, TabCompleter {
    private final TeamUPPlugin plugin;

    public TeamUPCommand(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("teamup.admin")) {
                Msg.send(sender, "&cKamu bukan admin.");
                return true;
            }
            plugin.reloadAll();
            Msg.send(sender, "&aConfig TeamUP dimuat ulang (tanpa restart).");
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Hanya untuk player.");
            return true;
        }
        if (args.length > 0) {
            String a = args[0].toLowerCase();
            if (a.equals("accept") || a.equals("terima")) {
                plugin.invites().accept(p);
                return true;
            }
            if (a.equals("hud")) {
                if (!plugin.hud().enabled()) {
                    Msg.send(p, "&cHUD sedang dimatikan oleh server (hud.enabled: false di config.yml).");
                    return true;
                }
                boolean on = plugin.hud().toggle(p);
                Msg.send(p, on ? "&aHUD dinyalakan." : "&7HUD dimatikan. Ketik &f/teamup hud &7lagi buat menyalakan.");
                return true;
            }
            if (a.equals("deny") || a.equals("cancel") || a.equals("tolak")) {
                plugin.invites().deny(p);
                return true;
            }
        }
        Team t = plugin.teams().ofPlayer(p.getUniqueId());
        if (t == null && plugin.teams().migrateByName(p.getUniqueId(), p.getName())) {
            t = plugin.teams().ofPlayer(p.getUniqueId());
        }
        if (t == null) {
            Msg.send(p, "&cKamu belum punya team. Team hanya bisa dibuat oleh admin.");
            return true;
        }
        if (!plugin.teams().hasPerm(t, p.getUniqueId(), TeamPerm.OPEN_MENU)) {
            Msg.send(p, "&cRank kamu tidak bisa membuka menu team.");
            return true;
        }
        try {
            new TeamMenu(plugin, p, null, t, false).open();
        } catch (Exception ex) {
            plugin.getLogger().severe("Gagal buka TeamMenu untuk " + p.getName() + ": " + ex);
            ex.printStackTrace();
            Msg.send(p, "&cMenu gagal dibuka (ada error). Kirim log console ke admin.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        return args.length == 1 ? List.of("accept", "deny", "hud", "reload") : List.of();
    }
}
