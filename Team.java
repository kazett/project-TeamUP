package id.teamup.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.format.NamedTextColor;

public class Team {
    private final String name;
    private UUID leader;
    private final Map<UUID, Rank> members = new LinkedHashMap<>();
    private final Map<UUID, String> lastNames = new HashMap<>();
    private final Map<Rank, String> rankNames = new EnumMap<>(Rank.class);
    private NamedTextColor color = NamedTextColor.GREEN;
    private boolean canRenameRanks;
    private int strikes;
    private final List<String> warnLog = new ArrayList<>();
    private long created = System.currentTimeMillis();

    public Team(String name, UUID leader, String leaderName) {
        this.name = name;
        this.leader = leader;
        members.put(leader, Rank.LEADER);
        lastNames.put(leader, leaderName);
    }

    public String getName() { return name; }
    public String getKey() { return name.toLowerCase(); }
    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }
    public Map<UUID, Rank> getMembers() { return members; }
    public Map<UUID, String> getLastNames() { return lastNames; }
    public Map<Rank, String> getRankNames() { return rankNames; }
    public NamedTextColor getColor() { return color; }
    public void setColor(NamedTextColor color) { this.color = color; }
    public boolean canRenameRanks() { return canRenameRanks; }
    public void setCanRenameRanks(boolean v) { this.canRenameRanks = v; }
    public int getStrikes() { return strikes; }
    public void setStrikes(int strikes) { this.strikes = strikes; }
    public List<String> getWarnLog() { return warnLog; }
    public long getCreated() { return created; }
    public void setCreated(long created) { this.created = created; }

    public Rank rankOf(UUID id) {
        Rank r = members.get(id);
        return r == null && id.equals(leader) ? Rank.LEADER : r;
    }

    public String nameOf(UUID id) {
        String n = lastNames.get(id);
        return n != null ? n : "?";
    }
}
