package com.skrra.blockreskinner.util;

import net.minecraft.util.Identifier;

public final class IdentifierUtil {
    private IdentifierUtil() {
    }

    public static Identifier parseOrNull(String value) {
        return value == null ? null : Identifier.tryParse(value);
    }
}
