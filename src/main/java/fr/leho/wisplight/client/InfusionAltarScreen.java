package fr.leho.wisplight.client;

import fr.leho.wisplight.Wisplight;
import fr.leho.wisplight.menu.InfusionAltarMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class InfusionAltarScreen extends AbstractContainerScreen<InfusionAltarMenu> {
    private static final ResourceLocation TEXTURE = Wisplight.id("textures/gui/infusion_altar.png");
    private static final int TEXT_COLOR = 0xE8ECFF;
    private static final int ARROW_X = 85, ARROW_Y = 35, ARROW_W = 32, ARROW_H = 16;
    private static final int JAR_X = 94, JAR_Y = 56;

    public InfusionAltarScreen(InfusionAltarMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        int filled = Math.round(menu.progress() * ARROW_W);
        if (filled > 0) {
            graphics.blit(TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y, 176, 0, filled, ARROW_H);
        }
        // Témoin du follet : allumé si un bocal est à portée
        graphics.blit(TEXTURE, leftPos + JAR_X, topPos + JAR_Y, menu.hasJar() ? 176 : 192, 16, 16, 16);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, TEXT_COLOR, false);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_COLOR, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = mouseX - leftPos, y = mouseY - topPos;
        if (x >= JAR_X && x < JAR_X + 16 && y >= JAR_Y && y < JAR_Y + 16) {
            graphics.renderTooltip(font, Component.translatable(menu.hasJar()
                    ? "gui.wisplight.altar.jar_ok" : "gui.wisplight.altar.jar_missing"), mouseX, mouseY);
        } else {
            renderTooltip(graphics, mouseX, mouseY);
        }
    }
}
