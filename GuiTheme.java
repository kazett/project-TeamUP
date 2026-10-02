package id.teamup.gui;

/** Tema warna menu. URUTAN harus sama dengan urutan tema di resource pack. */
public enum GuiTheme {
    TEAM(0x3A86FF),
    ADMIN(0xE63946),
    WARN(0xFF9628);

    private final int rgb;

    GuiTheme(int rgb) {
        this.rgb = rgb;
    }

    public int rgb() {
        return rgb;
    }
}
