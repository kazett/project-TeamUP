package id.teamup.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import id.teamup.Msg;
import id.teamup.TeamUPPlugin;

/** Minta input teks dari player lewat chat (dipakai GUI). */
public class ChatPrompt {
    private final TeamUPPlugin plugin;
    private final Map<UUID, Consumer<String>> pending = new HashMap<>();

    public ChatPrompt(TeamUPPlugin plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String prompt, Consumer<String> callback) {
        pending.put(p.getUniqueId(), callback);
        Bukkit.getScheduler().runTask(plugin, () -> p.closeInventory());
        Msg.send(p, prompt);
        Msg.send(p, "Ketik &ccancel &7untuk batal.");
    }

    public boolean has(UUID id) {
        return pending.containsKey(id);
    }

    public Consumer<String> take(UUID id) {
        return pending.remove(id);
    }

    public void clear(UUID id) {
        pending.remove(id);
    }
}
