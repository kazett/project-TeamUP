package id.teamup;

import id.teamup.command.AdminCommand;
import id.teamup.command.TeamChatCommand;
import id.teamup.command.TeamUPCommand;
import id.teamup.gui.MenuListener;
import id.teamup.listener.GameListener;
import id.teamup.manager.ChatPrompt;
import id.teamup.manager.HudManager;
import id.teamup.manager.InviteManager;
import id.teamup.manager.NametagManager;
import id.teamup.manager.TeamManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TeamUPPlugin extends JavaPlugin {
    private TeamManager teams;
    private InviteManager invites;
    private ChatPrompt prompts;
    private NametagManager tags;
    private HudManager hud;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tags = new NametagManager(this);
        teams = new TeamManager(this);
        invites = new InviteManager(this);
        prompts = new ChatPrompt(this);
        teams.load();
        hud = new HudManager(this);
        tags.refreshAll();

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        getServer().getPluginManager().registerEvents(new GameListener(this), this);

        TeamUPCommand main = new TeamUPCommand(this);
        getCommand("teamup").setExecutor(main);
        getCommand("teamup").setTabCompleter(main);
        AdminCommand admin = new AdminCommand(this);
        getCommand("teamupadmin").setExecutor(admin);
        getCommand("teamupadmin").setTabCompleter(admin);
        getCommand("tc").setExecutor(new TeamChatCommand(this));
        hud.start();
        banner();
        getLogger().info("TeamUP aktif - " + teams.all().size() + " team dimuat.");
    }

    private static final String GITHUB = "https://github.com/kazett";
    private static final String[] BANNER = {
            "&8&m                                        ",
            "&b&lTeamUP",
            "&7build by &bkazett",
            "&aenjoy create your team",
            "&esupport and update",
            "&9&n" + GITHUB,
            "&7lapor bug/saran bisa ke github",
            "&aenjoyerr yourr fraksi",
            "",
            "&c&lKAZETT DEVELOPMENT RIGHT",
            "&8&m                                        "
    };

    /** Banner di console saat plugin nyala. */
    private void banner() {
        String[] lines = BANNER.clone();
        lines[1] = "&b&lTeamUP &7v" + getDescription().getVersion();
        for (String l : lines) getServer().getConsoleSender().sendMessage(Msg.c(l));
    }

    /** Kirim banner ke player/console (dipakai saat join dan saat klik TeamUP di /plugins -> /version TeamUP). */
    public void sendBanner(CommandSender to) {
        for (String l : BANNER) {
            Component c = Msg.c(l);
            if (l.contains("github.com")) {
                c = c.clickEvent(ClickEvent.openUrl(GITHUB))
                        .hoverEvent(HoverEvent.showText(Msg.c("&7Klik untuk buka GitHub kazett")));
            }
            to.sendMessage(c);
        }
    }

    /** Satu baris link GitHub (dipakai saat klik TeamUP di /plugins -> /version TeamUP). */
    public void sendLink(CommandSender to) {
        to.sendMessage(Msg.c("&bTeamUP &7by &bkazett &8- &9&n" + GITHUB)
                .clickEvent(ClickEvent.openUrl(GITHUB))
                .hoverEvent(HoverEvent.showText(Msg.c("&7Klik untuk buka GitHub kazett"))));
    }

    /** Muat ulang config.yml tanpa restart server (/tua reload). */
    public void reloadAll() {
        reloadConfig();
        if (hud != null) hud.reload();
        if (tags != null) tags.refreshAll();
    }

    /** Teks sambutan di chat saat player join (bisa dimatikan: welcome.enabled di config.yml). */
    public void sendWelcome(Player p) {
        if (!getConfig().getBoolean("welcome.enabled", true)) return;
        getServer().getScheduler().runTaskLater(this, () -> {
            if (p.isOnline()) sendBanner(p);
        }, 20L);
    }

    @Override
    public void onDisable() {
        if (hud != null) hud.stop();
        if (teams != null) teams.save();
    }

    public TeamManager teams() { return teams; }
    public InviteManager invites() { return invites; }
    public ChatPrompt prompts() { return prompts; }
    public NametagManager tags() { return tags; }
    public HudManager hud() { return hud; }
}
