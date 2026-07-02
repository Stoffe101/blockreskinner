package com.skrra.blockreskinner.util;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Optional;

public final class BlockStateUtil {
    private BlockStateUtil() {
    }

    public static String toString(BlockState state) {
        StringBuilder builder = new StringBuilder(Registries.BLOCK.getId(state.getBlock()).toString());
        if (!state.getEntries().isEmpty()) {
            builder.append("[");
            boolean first = true;
            for (var entry : state.getEntries().entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                first = false;
                builder.append(entry.getKey().getName()).append("=").append(name(entry.getKey(), entry.getValue()));
            }
            builder.append("]");
        }
        return builder.toString();
    }

    public static BlockState parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String idPart = value;
        String propertiesPart = null;
        int bracket = value.indexOf('[');
        if (bracket >= 0 && value.endsWith("]")) {
            idPart = value.substring(0, bracket);
            propertiesPart = value.substring(bracket + 1, value.length() - 1);
        }

        Identifier id = Identifier.tryParse(idPart);
        if (id == null || !Registries.BLOCK.containsId(id)) {
            return null;
        }

        Block block = Registries.BLOCK.get(id);
        BlockState state = block.getDefaultState();
        if (propertiesPart == null || propertiesPart.isBlank()) {
            return state;
        }

        for (String pair : propertiesPart.split(",")) {
            String[] split = pair.split("=", 2);
            if (split.length != 2) {
                return null;
            }
            Property<?> property = block.getStateManager().getProperty(split[0]);
            if (property == null) {
                return null;
            }
            state = withProperty(state, property, split[1].toLowerCase(Locale.ROOT));
            if (state == null) {
                return null;
            }
        }

        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState withProperty(BlockState state, Property property, String value) {
        Optional<? extends Comparable> parsed = property.parse(value);
        return parsed.map(comparable -> (BlockState) state.with(property, comparable)).orElse(null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String name(Property property, Comparable value) {
        return property.name(value);
    }
}
