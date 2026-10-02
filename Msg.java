package id.teamup;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final String PREFIX = "&8[&bTeamUP&8] &7";
    private static final LegacyComponentSerializer L = LegacyComponentSerializer.legacyAmpersand();

    private Msg() {}

    public static Component c(String s) {
        return L.deserialize(s);
    }

    public static Component item(String s) {
        return L.deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    public static void send(CommandSender to, String s) {
        to.sendMessage(c(PREFIX + s));
    }
}
