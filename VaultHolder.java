package id.teamup.vault;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Pemilik inventory satu halaman brankas (inventory dipakai bersama semua yang membuka brankas yang sama). */
public final class VaultHolder implements InventoryHolder {
    public enum Nav { PREV, NEXT, LOG, NONE }

    private final Vault vault;
    private final int page;
    private Inventory inv;

    VaultHolder(Vault vault, int page) {
        this.vault = vault;
        this.page = page;
    }

    void setInventory(Inventory inv) {
        this.inv = inv;
    }

    public Vault vault() {
        return vault;
    }

    public int page() {
        return page;
    }

    /** Slot isi brankas yang boleh dipakai (bukan tombol, bukan slot terkunci). */
    public boolean allowed(int slot) {
        return slot >= 0 && slot < Vault.PER_PAGE && page * Vault.PER_PAGE + slot < vault.capacity();
    }

    public Nav nav(int slot) {
        if ((slot == 45 || slot == 46) && page > 0) return Nav.PREV;
        if ((slot == 52 || slot == 53) && page < vault.pageCount() - 1) return Nav.NEXT;
        if (slot >= 47 && slot <= 50) return Nav.LOG;
        return Nav.NONE;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
