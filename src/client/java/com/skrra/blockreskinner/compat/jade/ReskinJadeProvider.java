package com.skrra.blockreskinner.compat.jade;

import com.skrra.blockreskinner.BlockReskinnerMod;
import com.skrra.blockreskinner.skin.ClientSkinCache;
import com.skrra.blockreskinner.skin.ConnectedSkinData;
import com.skrra.blockreskinner.skin.ConnectionOverride;
import com.skrra.blockreskinner.skin.SimpleSkinData;
import com.skrra.blockreskinner.skin.SkinData;
import net.minecraft.text.Text;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.ArrayList;
import java.util.List;

public enum ReskinJadeProvider implements IBlockComponentProvider {
    INSTANCE;

    public static final Identifier UID = BlockReskinnerMod.id("visual_skin");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        SkinData data = ClientSkinCache.get(accessor.getPosition());
        if (data instanceof SimpleSkinData simple) {
            tooltip.add(Text.translatable("jade.blockreskinner.visual_skin", simple.visualState().getBlock().getName()));
            return;
        }
        if (data instanceof ConnectedSkinData connected) {
            tooltip.add(Text.translatable("jade.blockreskinner.visual_material", connected.visualMaterialState().getBlock().getName()));
            Text overrides = connectionOverrides(connected);
            if (overrides != null) {
                tooltip.add(Text.translatable("jade.blockreskinner.connection_overrides", overrides));
            }
        }
    }

    @Override
    public Identifier getUid() {
        return UID;
    }

    private static Text connectionOverrides(ConnectedSkinData data) {
        List<Text> parts = new ArrayList<>();
        addOverride(parts, "N", data.north());
        addOverride(parts, "E", data.east());
        addOverride(parts, "S", data.south());
        addOverride(parts, "W", data.west());
        if (parts.isEmpty()) {
            return null;
        }
        MutableText text = Text.empty();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                text.append(", ");
            }
            text.append(parts.get(i));
        }
        return text;
    }

    private static void addOverride(List<Text> parts, String label, ConnectionOverride override) {
        if (override == ConnectionOverride.AUTO) {
            return;
        }
        Text value = switch (override) {
            case AUTO -> Text.translatable("jade.blockreskinner.auto");
            case FORCE_ON -> Text.translatable("jade.blockreskinner.connected");
            case FORCE_OFF -> Text.translatable("jade.blockreskinner.disconnected");
        };
        parts.add(Text.literal(label + " ").append(value));
    }
}
