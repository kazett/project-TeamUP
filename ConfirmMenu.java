package id.teamup.gui;

import id.teamup.TeamUPPlugin;
import org.bukkit.entity.Player;

public class ConfirmMenu extends Menu {
    private final String title;
    private final String description;
    private final Runnable onYes;

    public ConfirmMenu(TeamUPPlugin plugin, Player viewer, Menu parent, String title, String description, Runnable onYes) {
        super(plugin, viewer, parent);
        this.title = title;
        this.description = description;
        this.onYes = onYes;
    }

    @Override
    protected GuiTheme theme() {
        return GuiTheme.WARN;
    }

    @Override
    protected String title() {
        return title;
    }

    @Override
    protected int rows() {
        return 6;
    }

    @Override
    protected void build() {
        // keterangan digambar sebagai teks (bukan item), dipecah per baris
        java.util.List<String> lines = GuiFont.wrap(description, 148);
        for (int i = 0; i < lines.size() && i < 3; i++) label(i * 9, 0, "&e" + lines.get(i));
        btn(27, Btn.YES, e -> onYes.run());
        btn(32, Btn.NO, e -> {
            if (parent != null) parent.open();
            else viewer.closeInventory();
        });
    }
}
