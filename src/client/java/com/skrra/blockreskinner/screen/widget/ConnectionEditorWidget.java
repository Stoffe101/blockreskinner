package com.skrra.blockreskinner.screen.widget;

import com.skrra.blockreskinner.skin.ConnectionOverride;

public final class ConnectionEditorWidget {
    private ConnectionEditorWidget() {
    }

    public static ConnectionOverride next(ConnectionOverride value) {
        return switch (value) {
            case AUTO -> ConnectionOverride.FORCE_ON;
            case FORCE_ON -> ConnectionOverride.FORCE_OFF;
            case FORCE_OFF -> ConnectionOverride.AUTO;
        };
    }
}
