package id.teamup.vault;

import java.util.UUID;

/** Satu baris riwayat brankas. action: P = masukin barang, T = ambil barang, S = catatan sistem. */
public record LogEntry(long ts, String name, UUID uuid, char action, String material, int amount, String item) {

    public static final UUID SYSTEM = new UUID(0L, 0L);

    static String clean(String s) {
        if (s == null) return "";
        return s.replace('|', '/').replace('\n', ' ').replace('\r', ' ').replace('\u00a7', ' ').trim();
    }

    String toLine() {
        return ts + "|" + action + "|" + clean(name) + "|" + uuid + "|" + clean(material) + "|" + amount + "|" + clean(item);
    }

    static LogEntry parse(String line) {
        try {
            String[] p = line.split("\\|", 7);
            if (p.length < 7) return null;
            return new LogEntry(Long.parseLong(p[0]), p[2], UUID.fromString(p[3]), p[1].charAt(0), p[4],
                    Integer.parseInt(p[5]), p[6]);
        } catch (Exception e) {
            return null; // baris rusak dilewati, tidak boleh merusak brankas
        }
    }
}
