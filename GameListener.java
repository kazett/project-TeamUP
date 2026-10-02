package id.teamup.listener;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.function.Consumer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

public class GameListener implements Listener {
    private final TeamUPPlugin plugin;

    public GameListener(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    /** Input chat untuk GUI (nama team, alasan warn, nama rank). */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        if (!plugin.prompts().has(p.getUniqueId())) return;
        e.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());
        Consumer<String> cb = plugin.prompts().take(p.getUniqueId());
        if (cb == null) return;
        if (text.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> Msg.send(p, "&7Dibatalkan."));
            return;
        }
        Bukkit.getScheduler().runTask(plugin, () -> cb.accept(text));
    }

    /** Tidak ada friendly fire antar anggota team yang sama. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (plugin.getConfig().getBoolean("friendly-fire", false)) return;
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = null;
        Entity d = e.getDamager();
        if (d instanceof Player p) attacker = p;
        else if (d instanceof Projectile proj && proj.getShooter() instanceof Player p) attacker = p;
        if (attacker == null || attacker.equals(victim)) return;
        Team a = plugin.teams().ofPlayer(attacker.getUniqueId());
        if (a != null && a == plugin.teams().ofPlayer(victim.getUniqueId())) {
            e.setCancelled(true);
            Msg.send(attacker, "&c" + victim.getName() + " adalah teman satu team kamu!");
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.hud().attach(p);
        plugin.sendWelcome(p);
        plugin.teams().migrateByName(p.getUniqueId(), p.getName());
        Team t = plugin.teams().ofPlayer(p.getUniqueId());
        if (t == null) return;
        if (!p.getName().equals(t.getLastNames().get(p.getUniqueId()))) {
            t.getLastNames().put(p.getUniqueId(), p.getName());
            plugin.teams().save();
        }
        plugin.tags().refresh(t);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.hud().detach(e.getPlayer().getUniqueId());
        plugin.prompts().clear(e.getPlayer().getUniqueId());
        plugin.invites().clear(e.getPlayer().getUniqueId());
    }

    /**
     * Klik nama TeamUP di /plugins menjalankan "/version TeamUP" -> tampilkan link GitHub TeamUP
     * (bukan info bawaan): cukup link GitHub kazett. Berlaku juga kalau diketik manual: /version TeamUP, /ver TeamUP, /about TeamUP.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onVersionCmd(PlayerCommandPreprocessEvent e) {
        if (!isAboutTeamUP(e.getMessage())) return;
        e.setCancelled(true);
        plugin.sendLink(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onConsoleVersionCmd(ServerCommandEvent e) {
        if (!isAboutTeamUP(e.getCommand())) return;
        e.setCancelled(true);
        plugin.sendLink(e.getSender());
    }

    private static boolean isAboutTeamUP(String raw) {
        String[] t = raw.trim().replaceFirst("^/", "").split("\\s+");
        if (t.length < 2) return false;
        String label = t[0].toLowerCase();
        int colon = label.indexOf(':');
        if (colon >= 0) label = label.substring(colon + 1);
        if (!(label.equals("version") || label.equals("ver") || label.equals("about"))) return false;
        return t[1].equalsIgnoreCase("teamup");
    }
}
