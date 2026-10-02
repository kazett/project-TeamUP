package id.teamup.model;

public enum Rank {
    LEADER, DEPUTY, MEMBER, SOLDIER, HANGEROUND;

    public Rank next() { // turun pangkat
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public Rank prev() { // naik pangkat
        return ordinal() > 0 ? values()[ordinal() - 1] : null;
    }
}
