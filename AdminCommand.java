package id.teamup.command;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.gui.AdminMenu;
import id.teamup.model.Team;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class AdminCommand implements CommandExecutor, TabCompleter {
    private final TeamUPPlugin plugin;

    public AdminCommand(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("teamup.admin")) {
            Msg.send(sender, "&cKamu bukan admin.");
            return true;
        }
        if (args.length == 0) {
            if (sender instanceof Player p) new AdminMenu(plugin, p).open();
            else Msg.send(sender, "&7Pakai: /tua create <team> <player> | delete <team> | list | hud <lebar> [margin] | reload");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "create" -> {
                if (args.length < 3) {
                    Msg.send(sender, "&7Pakai: /tua create <team> <player>");
                    return true;
                }
                // nama team boleh pakai spasi: semua kata sebelum argumen terakhir, argumen terakhir = ketua
                String teamName = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length - 1));
                String leaderArg = args[args.length - 1];
                Player online = Bukkit.getPlayerExact(leaderArg); // cocok walau beda huruf besar/kecil
                java.util.UUID uid;
                String realName;
                if (online != null) {
                    uid = online.getUniqueId();
                    realName = online.getName();
                } else {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(leaderArg);
                    uid = op.getUniqueId();
                    realName = leaderArg;
                }
                String err = plugin.teams().create(teamName, uid, realName);
                if (err != null) Msg.send(sender, "&c" + err);
                else Msg.send(sender, "&aTeam &e" + teamName + " &adibuat, ketua &e" + realName + "&a.");
            }
            case "delete" -> {
                Team t = args.length > 1 ? plugin.teams().get(String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length))) : null;
                if (t == null) {
                    Msg.send(sender, "&cTeam tidak ditemukan.");
                    return true;
                }
                plugin.teams().disband(t, true);
                Msg.send(sender, "&7Team &e" + t.getName() + " &7dihapus.");
            }
            case "reload" -> {
                plugin.reloadAll();
                Msg.send(sender, "&aConfig TeamUP dimuat ulang (tanpa restart).");
            }
            case "hud" -> {
                if (args.length < 2) {
                    Msg.send(sender, "&7Pakai: /tua hud <lebar GUI> [margin kanan]  (contoh: /tua hud 750 12)");
                    return true;
                }
                try {
                    plugin.getConfig().set("hud.gui-width", Integer.parseInt(args[1]));
                    if (args.length > 2) plugin.getConfig().set("hud.margin", Integer.parseInt(args[2]));
                    plugin.saveConfig();
                    plugin.hud().refreshAll();
                    Msg.send(sender, "&aPosisi HUD diperbarui.");
                } catch (NumberFormatException ex) {
                    Msg.send(sender, "&cAngka tidak valid.");
                }
            }
            case "list" -> {
                Msg.send(sender, "&7Team (" + plugin.teams().all().size() + "):");
                for (Team t : plugin.teams().all())
                    sender.sendMessage(Msg.c("&8- &b" + t.getName() + " &7ketua &f" + t.nameOf(t.getLeader())
                            + " &7member &f" + t.getMembers().size() + " &7strike &c" + t.getStrikes()));
            }
            default -> Msg.send(sender, "&7Pakai: /tua create <team> <player> | delete <team> | list | hud <lebar> [margin] | reload");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("teamup.admin")) return List.of();
        if (args.length == 1) return List.of("create", "delete", "list", "hud", "reload");
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            List<String> l = new ArrayList<>();
            for (Team t : plugin.teams().all()) l.add(t.getName());
            return l;
        }
        return List.of();
    }
}
