package id.teamup.gui;

/** Tombol berlabel (teks sudah digambar di resource pack). Dibuat otomatis oleh pack, jangan ubah urutan. */
public enum Btn {
    ANGGOTA(4),  // ANGGOTA
    ONLINE(4),  // ONLINE
    TAMBAH(4),  // TAMBAH
    NAMA_RANK(4),  // NAMA RANK
    WARN_TEAM(4),  // WARN TEAM
    HAPUS_TEAM(4),  // HAPUS TEAM
    IZIN_ON(4),  // IZIN: ON
    IZIN_OFF(4),  // IZIN: OFF
    BACK4(4),  // < KEMBALI
    CLOSE4(4),  // TUTUP
    ADD_TEAM(4),  // ADD TEAM
    LIST_TEAM(4),  // LIST TEAM
    YES(4),  // YA
    NO(4),  // BATAL
    LV1(3),  // LEVEL 1
    LV2(3),  // LEVEL 2
    LV3(3),  // LEVEL 3
    BACK3(3),  // KEMBALI
    PREV2(2),  // <
    NEXT2(2),
    NAIK(4),
    TURUN(4),
    KICK(4),
    OK2(2),
    OK2_OFF(2),
    COLOR(4);

    private final int width;

    Btn(int width) {
        this.width = width;
    }

    /** lebar dalam slot */
    public int width() {
        return width;
    }
}
