package com.skrra.blockreskinner.skin;

public enum ConnectionOverride {
    AUTO,
    FORCE_ON,
    FORCE_OFF;

    public static ConnectionOverride byOrdinal(int ordinal) {
        ConnectionOverride[] values = values();
        return ordinal >= 0 && ordinal < values.length ? values[ordinal] : AUTO;
    }
}
