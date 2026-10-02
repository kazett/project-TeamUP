package id.teamup;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class Items {
    private Items() {}

    public static ItemStack make(Material m, String name, String... lore) {
        return make(m, name, List.of(lore));
    }

    public static ItemStack make(Material m, String name, List<String> lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Msg.item(name));
        if (!lore.isEmpty()) {
            List<net.kyori.adventure.text.Component> l = new ArrayList<>();
            for (String s : lore) l.add(Msg.item(s));
            meta.lore(l);
        }
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack head(OfflinePlayer p, String name, List<String> lore) {
        ItemStack it = make(Material.PLAYER_HEAD, name, lore);
        SkullMeta sm = (SkullMeta) it.getItemMeta();
        sm.setOwningPlayer(p);
        it.setItemMeta(sm);
        return it;
    }

    public static ItemStack glow(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        meta.addEnchant(Enchantment.LURE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        it.setItemMeta(meta);
        return it;
    }
}
