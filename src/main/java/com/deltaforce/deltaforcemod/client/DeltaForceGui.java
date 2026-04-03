package com.deltaforce.deltaforcemod.client;

import com.deltaforce.deltaforcemod.DeltaForceMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class DeltaForceGui extends Screen {

    public DeltaForceGui() {
        super(Component.literal("三角洲行动"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 80;
        int buttonWidth = 180;
        int buttonHeight = 25;
        int spacing = 20;

        // 第1行：功能1 和 功能2
        int row1Y = startY;
        int leftX = centerX - buttonWidth - spacing;
        int rightX = centerX + spacing;

        // 功能1：模式切换
        String currentMode = DeltaForceMod.getInstance().getGameMode();
        String modeText = currentMode.equals("test") ? "模式: 测试模式" : "模式: 正常模式";

        this.addRenderableWidget(Button.builder(
                        Component.literal(modeText),
                        button -> {
                            String current = DeltaForceMod.getInstance().getGameMode();
                            if (current.equals("test")) {
                                DeltaForceMod.getInstance().setGameMode("normal");
                            } else {
                                DeltaForceMod.getInstance().setGameMode("test");
                            }
                            this.minecraft.setScreen(new DeltaForceGui());
                        }
                )
                .bounds(leftX, row1Y, buttonWidth, buttonHeight)
                .build());

        // 功能2：占位按钮（后续添加功能）
        this.addRenderableWidget(Button.builder(
                        Component.literal("功能2"),
                        button -> {}
                )
                .bounds(rightX, row1Y, buttonWidth, buttonHeight)
                .build());

        // 第2行：功能3 和 功能4
        int row2Y = row1Y + buttonHeight + spacing;

        // 功能3：占位按钮
        this.addRenderableWidget(Button.builder(
                        Component.literal("功能3"),
                        button -> {}
                )
                .bounds(leftX, row2Y, buttonWidth, buttonHeight)
                .build());

        // 功能4：占位按钮
        this.addRenderableWidget(Button.builder(
                        Component.literal("功能4"),
                        button -> {}
                )
                .bounds(rightX, row2Y, buttonWidth, buttonHeight)
                .build());

        // 关闭按钮（底部居中）
        int closeButtonWidth = 100;
        int closeButtonHeight = 20;
        int closeButtonX = centerX - closeButtonWidth / 2;
        int closeButtonY = this.height - 40;

        this.addRenderableWidget(Button.builder(
                        Component.literal("关闭"),
                        button -> this.onClose()
                )
                .bounds(closeButtonX, closeButtonY, closeButtonWidth, closeButtonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // 渲染标题
        guiGraphics.drawCenteredString(this.font, "§6三角洲行动", this.width / 2, 30, 0xFFFFFF);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(null);
    }
}