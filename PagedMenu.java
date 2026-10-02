package id.teamup.gui;

import id.teamup.Items;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import id.teamup.TeamUPPlugin;
import org.bukkit.entity.Player;

/** Menu 6 baris dengan halaman: slot 0-44 isi, 45-46 prev, 48-50 kembali, 52-53 next, 47/51 tombol tambahan. */
public abstract class PagedMenu<T> extends Menu {
    protected int page = 0;

    protected PagedMenu(TeamUPPlugin plugin, Player viewer, Menu parent) {
        super(plugin, viewer, parent);
    }

    protected abstract List<T> entries();

    protected abstract ItemStack render(T entry);

    protected abstract void click(T entry, InventoryClickEvent e);

    /** hook untuk tombol tambahan (slot 47 dan 51). */
    protected void extra() {}

    protected int backSlot() {
        return 48;
    }

    @Override
    protected String titleText() {
        int pages = Math.max(1, (entries().size() + 44) / 45);
        return GuiFont.clip(title(), 20) + " &7(" + (Math.min(page, pages - 1) + 1) + "/" + pages + ")";
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        List<T> list = entries();
        int per = 45;
        int pages = Math.max(1, (list.size() + per - 1) / per);
        if (page >= pages) page = pages - 1;
        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= list.size()) break;
            T entry = list.get(idx);
            set(i, render(entry), e -> click(entry, e));
        }
        if (page > 0) btn(45, Btn.PREV2, e -> { page--; refresh(); });
        if (page < pages - 1) btn(52, Btn.NEXT2, e -> { page++; refresh(); });
        btn(backSlot(), Btn.BACK3, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
        extra();
    }
}
