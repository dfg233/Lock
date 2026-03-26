package com.dfg233.lock.screen;

import com.dfg233.lock.Lock;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 钥匙复制台 GUI 屏幕
 */
public class KeyDuplicatorScreen extends AbstractContainerScreen<KeyDuplicatorMenu> {

    // GUI 纹理路径
    private static final ResourceLocation GUI_TEXTURE =
             ResourceLocation.fromNamespaceAndPath(Lock.MODID, "textures/gui/key_duplicator.png");

    public KeyDuplicatorScreen(KeyDuplicatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        // GUI 尺寸
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // 标题位置调整
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // 绘制 GUI 背景
        graphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // 可以在这里添加进度条或其他动态元素的绘制
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
