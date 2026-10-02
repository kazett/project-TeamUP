package id.teamup.manager;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

public class InviteManager {
    private record Invite(String teamKey, UUID inviter, long expires) {}

    private final TeamUPPlugin plugin;
    private final Map<UUID, Invite> invites = new HashMap<>();

    public InviteManager(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    /** return null kalau sukses, selain itu pesan error. */
    public String invite(Team team, Player inviter, Player target) {
        TeamManager tm = plugin.teams();
        if (tm.ofPlayer(target.getUniqueId()) != null) return "&e" + target.getName() + " &7sudah punya team.";
        String overlap = tm.checkOverlap(team, target.getUniqueId());
        if (overlap != null) return overlap;
        long ttl = plugin.getConfig().getLong("invite-expire-seconds", 60);
        invites.put(target.getUniqueId(), new Invite(team.getKey(), inviter.getUniqueId(), System.currentTimeMillis() + ttl * 1000));

        Component accept = Component.text("[TERIMA]", NamedTextColor.GREEN, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/teamup accept"))
                .hoverEvent(HoverEvent.showText(Msg.c("&aGabung ke team " + team.getName())));
        Component deny = Component.text("[CANCEL]", NamedTextColor.RED, TextDecoration.BOLD)
                .clickEvent(ClickEvent.runCommand("/teamup deny"))
                .hoverEvent(HoverEvent.showText(Msg.c("&cTolak undangan")));
        target.sendMessage(Msg.c("&8&m                                  "));
        target.sendMessage(Msg.c(Msg.PREFIX + "&e" + inviter.getName() + " &7mengundang kamu ke team &b" + team.getName()));
        target.sendMessage(accept.append(Component.text("   ")).append(deny));
        target.sendMessage(Msg.c("&7Undangan hangus dalam &e" + ttl + " &7detik."));
        target.sendMessage(Msg.c("&8&m                                  "));
        return null;
    }

    public void accept(Player p) {
        Invite inv = invites.remove(p.getUniqueId());
        if (inv == null || inv.expires() < System.currentTimeMillis()) {
            Msg.send(p, "&cTidak ada undangan aktif.");
            return;
        }
        Team team = plugin.teams().get(inv.teamKey());
        if (team == null) {
            Msg.send(p, "&cTeam sudah tidak ada.");
            return;
        }
        String err = plugin.teams().addMember(team, p.getUniqueId(), p.getName());
        if (err != null) {
            Msg.send(p, "&c" + err);
            return;
        }
        plugin.teams().broadcast(team, Msg.c(Msg.PREFIX + "&a" + p.getName() + " &7bergabung ke team!"));
    }

    public void deny(Player p) {
        Invite inv = invites.remove(p.getUniqueId());
        if (inv == null) {
            Msg.send(p, "&cTidak ada undangan aktif.");
            return;
        }
        Msg.send(p, "&7Undangan ditolak.");
        Player inviter = plugin.getServer().getPlayer(inv.inviter());
        if (inviter != null) Msg.send(inviter, "&e" + p.getName() + " &7menolak undanganmu.");
    }

    public void clear(UUID id) {
        invites.remove(id);
    }
}
