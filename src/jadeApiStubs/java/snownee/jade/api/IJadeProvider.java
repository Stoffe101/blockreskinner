package snownee.jade.api;

import net.minecraft.util.Identifier;

public interface IJadeProvider {
    Identifier getUid();

    default int getDefaultPriority() {
        return 0;
    }
}
