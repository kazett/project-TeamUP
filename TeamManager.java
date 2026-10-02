package id.teamup.manager;

import id.teamup.Msg;
import id.teamup.TeamUPPlugin;
import id.teamup.model.Rank;
import id.teamup.model.Team;
import id.teamup.model.TeamPerm;
import net.kyori.adventure.text.format.NamedTextColor;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class TeamManager {
    public static final int HARD_MAX_MEMBERS = 44;
    private static final Pattern NAME_CHARS = Pattern.compile("[A-Za-z0-9_]+");
    private static final Pattern NAME_CHARS_SPACE = Pattern.compile("[A-Za-z0-9_]+( [A-Za-z0-9_]+)*");

    /** Panjang maksimal nama team (config team-name-max-length, default 25). */
    public int maxNameLength() {
        return Math.max(2, Math.min(32, plugin.getConfig().getInt("team-name-max-length", 25)));
    }

    public record Dissolved(String name, Set<UUID> members) {}

    private final TeamUPPlugin plugin;
    private final File file;
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, Team> byPlayer = new HashMap<>();
    private final List<Dissolved> dissolved = new ArrayList<>();

    public TeamManager(TeamUPPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    // ---------- lookup ----------

    public Team get(String name) {
        return name == null ? null : teams.get(name.toLowerCase());
    }

    public Team ofPlayer(UUID id) {
        Team t = byPlayer.get(id);
        if (t != null) return t;
        // jaga-jaga kalau index byPlayer ketinggalan (mis. data lama)
        for (Team x : teams.values()) {
            if (x.getLeader().equals(id) || x.getMembers().containsKey(id)) {
                byPlayer.put(id, x);
                return x;
            }
        }
        return null;
    }

    /**
     * Kalau team dibuat lewat command dengan nama yang beda huruf besar/kecil (server cracked),
     * UUID ketua yang tersimpan bukan UUID asli pemain. Di sini UUID lama dipindah ke UUID asli.
     * Hanya dipindah kalau UUID lama belum pernah main (bukan orang lain).
     */
    public boolean migrateByName(UUID newId, String name) {
        if (byPlayer.containsKey(newId)) return false;
        for (Team t : teams.values()) {
            for (Map.Entry<UUID, String> e : new ArrayList<>(t.getLastNames().entrySet())) {
                UUID old = e.getKey();
                if (old.equals(newId) || !name.equalsIgnoreCase(e.getValue())) continue;
                if (!t.getMembers().containsKey(old) && !t.getLeader().equals(old)) continue;
                if (Bukkit.getOfflinePlayer(old).hasPlayedBefore()) continue;
                Rank r = t.getMembers().remove(old);
                if (r == null) r = Rank.LEADER;
                t.getMembers().put(newId, r);
                t.getLastNames().remove(old);
                t.getLastNames().put(newId, name);
                if (t.getLeader().equals(old)) t.setLeader(newId);
                byPlayer.remove(old);
                byPlayer.put(newId, t);
                save();
                plugin.tags().refresh(t);
                return true;
            }
        }
        return false;
    }

    public List<Team> all() {
        List<Team> l = new ArrayList<>(teams.values());
        l.sort(Comparator.comparing(Team::getName, String.CASE_INSENSITIVE_ORDER));
        return l;
    }

    public List<Player> onlineMembers(Team t) {
        List<Player> l = new ArrayList<>();
        for (UUID id : t.getMembers().keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) l.add(p);
        }
        return l;
    }

    public void broadcast(Team t, Component c) {
        for (Player p : onlineMembers(t)) p.sendMessage(c);
    }

    // ---------- rank & permission ----------

    public String rankName(Team t, Rank r) {
        String custom = t.getRankNames().get(r);
        if (custom != null) return custom;
        return plugin.getConfig().getString("ranks." + r.name() + ".name", defaultRankName(r));
    }

    private static String defaultRankName(Rank r) {
        return switch (r) {
            case LEADER -> "KETUA";
            case DEPUTY -> "WAKIL KETUA";
            default -> r.name();
        };
    }

    public boolean hasPerm(Team t, UUID id, TeamPerm perm) {
        Rank r = t.rankOf(id);
        if (r == null) return false;
        if (perm == TeamPerm.RANK) return r == Rank.LEADER;
        if (r == Rank.LEADER && perm == TeamPerm.OPEN_MENU) return true; // ketua tidak boleh terkunci dari menu
        String base = "ranks." + r.name() + ".";
        var cfg = plugin.getConfig();
        if (cfg.contains(base + perm.key())) return cfg.getBoolean(base + perm.key());
        if (cfg.contains(base + "permissions." + perm.key())) return cfg.getBoolean(base + "permissions." + perm.key());
        return defaultPerm(r, perm);
    }

    /** Default kalau config.yml lama tidak punya kuncinya. */
    private static boolean defaultPerm(Rank r, TeamPerm perm) {
        return switch (r) {
            case LEADER -> true;
            case DEPUTY -> perm == TeamPerm.OPEN_MENU || perm == TeamPerm.VIEW_ONLINE || perm == TeamPerm.CHAT;
            case MEMBER -> perm == TeamPerm.CHAT;
            default -> false;
        };
    }

    private static Rank parseRank(String s) {
        if (s == null) return Rank.HANGEROUND;
        String k = s.trim().toUpperCase().replace(' ', '_');
        switch (k) {
            case "KETUA" -> { return Rank.LEADER; }
            case "WAKIL_KETUA", "WAKIL" -> { return Rank.DEPUTY; }
            default -> { }
        }
        try {
            return Rank.valueOf(k);
        } catch (IllegalArgumentException e) {
            return Rank.HANGEROUND;
        }
    }

    // ---------- create / members ----------

    public String create(String name, UUID leader, String leaderName) {
        name = name.trim().replaceAll("\\s+", " ");
        boolean spaces = plugin.getConfig().getBoolean("team-name-allow-spaces", true);
        Pattern pat = spaces ? NAME_CHARS_SPACE : NAME_CHARS;
        if (name.length() < 2 || name.length() > maxNameLength() || !pat.matcher(name).matches())
            return "Nama team harus 2-" + maxNameLength() + " karakter (huruf/angka/_" + (spaces ? "/spasi" : "") + ").";
        if (get(name) != null) return "Team &e" + name + " &7sudah ada.";
        if (byPlayer.containsKey(leader)) return "&e" + leaderName + " &7sudah punya team.";
        Team t = new Team(name, leader, leaderName);
        teams.put(t.getKey(), t);
        byPlayer.put(leader, t);
        save();
        plugin.tags().refresh(t);
        return null;
    }

    public String addMember(Team t, UUID id, String name) {
        if (byPlayer.containsKey(id)) return "&e" + name + " &7sudah punya team.";
        int max = Math.min(HARD_MAX_MEMBERS, plugin.getConfig().getInt("max-members", 20));
        if (t.getMembers().size() >= max) return "Team sudah penuh (max " + max + ").";
        String overlap = checkOverlap(t, id);
        if (overlap != null) return overlap;
        t.getMembers().put(id, Rank.HANGEROUND);
        t.getLastNames().put(id, name);
        byPlayer.put(id, t);
        save();
        plugin.tags().refresh(t);
        return null;
    }

    /**
     * null kalau boleh. Aturan (bisa dimatikan di config.yml, bagian recreate):
     * max N orang yang sama dari team yang dibubarkan admin.
     */
    public String checkOverlap(Team t, UUID adding) {
        var cfg = plugin.getConfig();
        if (!cfg.getBoolean("recreate.enabled", true)) return null;
        int limit = cfg.getInt("recreate.max-same-members", cfg.getInt("recreate-max-same-members", 3));
        Set<UUID> candidate = new HashSet<>(t.getMembers().keySet());
        candidate.add(adding);
        for (Dissolved d : dissolved) {
            int n = 0;
            for (UUID u : candidate) if (d.members().contains(u)) n++;
            if (n > limit)
                return "Maksimal " + limit + " orang dari bekas team &e" + d.name()
                        + " &7yang boleh berada di team baru.";
        }
        return null;
    }

    public void removeMember(Team t, UUID id) {
        t.getMembers().remove(id);
        byPlayer.remove(id);
        String n = t.nameOf(id);
        save();
        plugin.tags().clear(n);
        plugin.tags().refresh(t);
    }

    public String setRank(Team t, UUID id, Rank r) {
        if (r == Rank.LEADER) return "Rank ketua tidak bisa diubah dari sini.";
        if (t.rankOf(id) == null) return "Bukan member team.";
        t.getMembers().put(id, r);
        save();
        return null;
    }

    public void renameRank(Team t, Rank r, String name) {
        t.getRankNames().put(r, name);
        save();
    }

    // ---------- disband ----------

    public void disband(Team t, boolean forced) {
        if (forced) dissolved.add(new Dissolved(t.getName(), new HashSet<>(t.getMembers().keySet())));
        List<String> names = new ArrayList<>();
        for (UUID id : t.getMembers().keySet()) {
            names.add(t.nameOf(id));
            byPlayer.remove(id);
        }
        teams.remove(t.getKey());
        plugin.tags().removeTeam(t, names);
        save();
    }

    // ---------- warn ----------

    @SuppressWarnings({"deprecation", "unchecked", "rawtypes"})
    public void warn(Team t, int level, Collection<UUID> culprits, String reason, String by) {
        List<Integer> days = plugin.getConfig().getIntegerList("warn-ban-days");
        int d = level - 1 < days.size() ? days.get(level - 1) : level;
        Date expires = new Date(System.currentTimeMillis() + d * 86_400_000L);
        List<String> names = new ArrayList<>();
        for (UUID id : culprits) {
            String n = t.nameOf(id);
            names.add(n);
            String banMsg = "Warn " + level + " untuk team " + t.getName() + ": " + reason;
            Bukkit.getBanList(BanList.Type.NAME).addBan(n, banMsg, expires, by);
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.kick(Msg.c("&cKamu di-ban " + d + " hari\n&7" + banMsg));
        }
        t.setStrikes(t.getStrikes() + 1);
        t.getWarnLog().add("Warn " + level + " | " + String.join(",", names) + " | " + reason + " | oleh " + by);

        String who = String.join(", ", names);
        Component msg = Msg.c("&8&m                                  \n"
                + "&c&lWARN " + level + " &7untuk team &e" + t.getName() + "\n"
                + "&7Pelaku: &f" + who + " &7(ban " + d + " hari)\n"
                + "&7Alasan: &f" + reason + "\n"
                + "&7Total strike team: &c" + t.getStrikes()
                + "\n"
                + "&8&m                                  ");
        broadcast(t, msg);
        save(); // bubar atau tidaknya team sepenuhnya keputusan admin
    }

    // ---------- persistence ----------

    public void load() {
        teams.clear();
        byPlayer.clear();
        dissolved.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection ts = y.getConfigurationSection("teams");
        if (ts != null) {
            for (String key : ts.getKeys(false)) {
                ConfigurationSection s = ts.getConfigurationSection(key);
                if (s == null) continue;
                UUID leader = UUID.fromString(s.getString("leader"));
                Team t = new Team(s.getString("name", key), leader, "?");
                t.getMembers().clear();
                t.getLastNames().clear();
                ConfigurationSection ms = s.getConfigurationSection("members");
                if (ms != null) {
                    for (String u : ms.getKeys(false)) {
                        UUID id = UUID.fromString(u);
                        Rank r = parseRank(ms.getString(u + ".rank", "HANGEROUND"));
                        if (id.equals(leader)) r = Rank.LEADER;
                        t.getMembers().put(id, r);
                        t.getLastNames().put(id, ms.getString(u + ".name", "?"));
                        byPlayer.put(id, t);
                    }
                }
                ConfigurationSection rs = s.getConfigurationSection("rank-names");
                if (rs != null) {
                    for (String rk : rs.getKeys(false)) {
                        try {
                            t.getRankNames().put(Rank.valueOf(rk), rs.getString(rk));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                }
                if (!t.getMembers().containsKey(leader)) {
                    t.getMembers().put(leader, Rank.LEADER);
                    t.getLastNames().put(leader, "?");
                    byPlayer.put(leader, t);
                }
                NamedTextColor col = NamedTextColor.NAMES.value(s.getString("color", "green"));
                if (col != null) t.setColor(col);
                t.setCanRenameRanks(s.getBoolean("can-rename", false));
                t.setStrikes(s.getInt("strikes", 0));
                t.setCreated(s.getLong("created", System.currentTimeMillis()));
                t.getWarnLog().addAll(s.getStringList("warn-log"));
                teams.put(t.getKey(), t);
            }
        }
        ConfigurationSection ds = y.getConfigurationSection("dissolved");
        if (ds != null) {
            for (String k : ds.getKeys(false)) {
                Set<UUID> set = new HashSet<>();
                for (String u : ds.getStringList(k + ".members")) set.add(UUID.fromString(u));
                dissolved.add(new Dissolved(ds.getString(k + ".name", "?"), set));
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Team t : teams.values()) {
            String b = "teams." + t.getKey();
            y.set(b + ".name", t.getName());
            y.set(b + ".leader", t.getLeader().toString());
            y.set(b + ".color", NamedTextColor.NAMES.key(t.getColor()));
            y.set(b + ".can-rename", t.canRenameRanks());
            y.set(b + ".strikes", t.getStrikes());
            y.set(b + ".created", t.getCreated());
            y.set(b + ".warn-log", t.getWarnLog());
            for (Map.Entry<UUID, Rank> e : t.getMembers().entrySet()) {
                String mb = b + ".members." + e.getKey();
                y.set(mb + ".rank", e.getValue().name());
                y.set(mb + ".name", t.nameOf(e.getKey()));
            }
            for (Map.Entry<Rank, String> e : t.getRankNames().entrySet())
                y.set(b + ".rank-names." + e.getKey().name(), e.getValue());
        }
        int i = 0;
        for (Dissolved d : dissolved) {
            y.set("dissolved." + i + ".name", d.name());
            List<String> l = new ArrayList<>();
            for (UUID u : d.members()) l.add(u.toString());
            y.set("dissolved." + i + ".members", l);
            i++;
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal simpan data.yml: " + e.getMessage());
        }
    }
}
