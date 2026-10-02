package id.teamup.manager;

import id.teamup.TeamUPPlugin;
import id.teamup.model.Team;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Scoreboard;

/** Tanda teman: prefix [TEAM] berwarna sesuai pilihan ketua di atas kepala & tab list (scoreboard team). */
public class NametagManager {
    private final TeamUPPlugin plugin;

    public NametagManager(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("nametag.enabled", true);
    }

    private List<Scoreboard> boards() {
        return List.of(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    private String sbName(Team t) {
        String n = "tu_" + t.getKey();
        // nama scoreboard dibatasi 16 karakter; nama panjang / bersepasi diganti hash ('~' tidak mungkin ada di nama team)
        return n.length() > 16 || !n.matches("[a-z0-9_]+") ? "tu_~" + Integer.toHexString(t.getKey().hashCode()) : n;
    }

    public void refresh(Team t) {
        if (!enabled()) return;
        for (Scoreboard b : boards()) refresh(b, t);
    }

    private void refresh(Scoreboard b, Team t) {
        org.bukkit.scoreboard.Team st = b.getTeam(sbName(t));
        if (st == null) st = b.registerNewTeam(sbName(t));
        st.prefix(Component.text("[" + t.getName() + "] ", t.getColor()));
        st.color(t.getColor());
        st.setCanSeeFriendlyInvisibles(true);
        List<String> names = new ArrayList<>();
        for (UUID id : t.getMembers().keySet()) {
            String n = t.nameOf(id);
            if (!n.equals("?")) names.add(n); // nama belum diketahui, jangan dimasukkan ke scoreboard
        }
        for (String e : new ArrayList<>(st.getEntries())) {
            if (!names.contains(e)) st.removeEntry(e);
        }
        for (String n : names) {
            if (!st.hasEntry(n)) st.addEntry(n);
        }
    }

    public void clear(String playerName) {
        if (!enabled()) return;
        for (Scoreboard b : boards()) {
            org.bukkit.scoreboard.Team st = b.getEntryTeam(playerName);
            if (st != null && st.getName().startsWith("tu_")) {
                st.removeEntry(playerName);
                if (st.getEntries().isEmpty()) st.unregister();
            }
        }
    }

    public void removeTeam(Team t, List<String> memberNames) {
        if (!enabled()) return;
        for (Scoreboard b : boards()) {
            org.bukkit.scoreboard.Team st = b.getTeam(sbName(t));
            if (st != null) st.unregister();
        }
    }

    public void refreshAll() {
        for (Team t : plugin.teams().all()) refresh(t);
    }
}
