package com.skrra.blockreskinner.screen.widget;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public class BlockSearchWidget extends TextFieldWidget {
    public BlockSearchWidget(TextRenderer textRenderer, int x, int y, int width, int height) {
        super(textRenderer, x, y, width, height, Text.translatable("screen.blockreskinner.search"));
        setPlaceholder(Text.translatable("screen.blockreskinner.search"));
        setMaxLength(64);
    }
}
