package id.teamup.command;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /tc pesan  ->  【TEAM】NAMA: pesan  (hanya ke anggota team) */
public class TeamChatCommand implements CommandExecutor {
    private final TeamUPPlugin plugin;

    public TeamChatCommand(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Hanya untuk player.");
            return true;
        }
        Team t = plugin.teams().ofPlayer(p.getUniqueId());
        if (t == null) {
            Msg.send(p, "&cKamu tidak punya team.");
            return true;
        }
        if (!plugin.teams().hasPerm(t, p.getUniqueId(), TeamPerm.CHAT)) {
            Msg.send(p, "&cRank kamu tidak bisa chat di team chat.");
            return true;
        }
        if (args.length == 0) {
            Msg.send(p, "&7Pakai: &f/tc <pesan>");
            return true;
        }
        String msg = String.join(" ", args);
        plugin.teams().broadcast(t, Msg.c("&8【&b" + t.getName() + "&8】&f" + p.getName() + "&7: &f")
                .append(net.kyori.adventure.text.Component.text(msg)));
        return true;
    }
}
