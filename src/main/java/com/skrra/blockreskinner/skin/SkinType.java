package com.skrra.blockreskinner.skin;

public enum SkinType {
    SIMPLE,
    CONNECTED,
    // Appended last: the network codec writes this enum by constant, and the
    // storage format by name, so existing SIMPLE/CONNECTED data stays stable.
    PLAYER_HEAD
}
