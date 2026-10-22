/*
 * ClickGUI window layout and interaction are inspired by BleachHack Legacy:
 * https://github.com/BleachDev/bleachhack-legacy/ (GPL-3.0).
 * This is a minimal Forge 1.8.9 adaptation for LunarSpoof settings only.
 * No BleachHack gameplay modules or cheat features are included. See LICENSE.
 */
package org.afterlike.moonlight.gui;

import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.afterlike.moonlight.Moonlight;
import org.afterlike.moonlight.config.LunarSpoofConfig;
import org.afterlike.moonlight.peers.SubscriptionManager;
import org.afterlike.moonlight.rpc.DiscordPresenceManager;

/**
 * Draggable, collapsible settings windows for the client-only LunarSpoof options.
 * The screen is intentionally non-pausing and only edits the local client config.
 */
public final class LunarSpoofClickGui extends GuiScreen {
    private static final int HEADER_HEIGHT = 18;
    private static final int BRAND_WIDTH = 190;
    private static final int PRIVACY_WIDTH = 180;
    private static final int DISCORD_WIDTH = 220;
    private static final int PADDING = 8;

    private final LunarSpoofConfig config;
    private Panel brandPanel;
    private Panel privacyPanel;
    private Panel discordPanel;
    private Panel draggingPanel;
    private int dragOffsetX;
    private int dragOffsetY;

    private GuiTextField customBrandField;
    private GuiTextField applicationIdField;
    private GuiTextField detailsField;
    private GuiTextField stateField;
    private GuiTextField imageKeyField;
    private String notice = "Drag the title bars to arrange windows. Right-click a title to collapse it.";

    public LunarSpoofClickGui() {
        this.config = LunarSpoofConfig.get();
    }

    @Override
    public void initGui() {
        int totalWidth = BRAND_WIDTH + 12 + PRIVACY_WIDTH + 12 + DISCORD_WIDTH;
        int left = Math.max(8, (width - totalWidth) / 2);
        int top = 36;

        brandPanel = new Panel("CLIENT BRAND", left, top, BRAND_WIDTH, 116);
        privacyPanel = new Panel("PRIVACY", left + BRAND_WIDTH + 12, top, PRIVACY_WIDTH, 104);
        discordPanel = new Panel("DISCORD RPC", left + BRAND_WIDTH + 12 + PRIVACY_WIDTH + 12, top,
                DISCORD_WIDTH, 194);

        if (width < totalWidth + 16) {
            int narrowLeft = Math.max(8, (width - DISCORD_WIDTH) / 2);
            brandPanel.x = Math.max(8, (width - BRAND_WIDTH) / 2);
            privacyPanel.x = Math.max(8, (width - PRIVACY_WIDTH) / 2);
            discordPanel.x = narrowLeft;
            brandPanel.y = top;
            privacyPanel.y = top + 128;
            discordPanel.y = top + 244;
        }

        customBrandField = new GuiTextField(0, fontRendererObj, 0, 0, BRAND_WIDTH - PADDING * 2, 18);
        customBrandField.setMaxStringLength(128);
        customBrandField.setText(config.customBrand);

        applicationIdField = new GuiTextField(1, fontRendererObj, 0, 0, DISCORD_WIDTH - PADDING * 2, 18);
        applicationIdField.setMaxStringLength(22);
        applicationIdField.setText(config.discordApplicationId);

        detailsField = new GuiTextField(2, fontRendererObj, 0, 0, DISCORD_WIDTH - PADDING * 2, 18);
        detailsField.setMaxStringLength(128);
        detailsField.setText(config.discordDetails);

        stateField = new GuiTextField(3, fontRendererObj, 0, 0, DISCORD_WIDTH - PADDING * 2, 18);
        stateField.setMaxStringLength(128);
        stateField.setText(config.discordState);

        imageKeyField = new GuiTextField(4, fontRendererObj, 0, 0, DISCORD_WIDTH - PADDING * 2, 18);
        imageKeyField.setMaxStringLength(256);
        imageKeyField.setText(config.discordLargeImageKey);

        updateFieldPositions();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawGradientRect(0, 0, width, height, 0xD9101220, 0xE8060710);

        if (draggingPanel != null) {
            draggingPanel.x = clamp(mouseX - dragOffsetX, 2, Math.max(2, width - draggingPanel.width - 2));
            draggingPanel.y = clamp(mouseY - dragOffsetY, 2, Math.max(2, height - HEADER_HEIGHT - 2));
        }

        updateFieldPositions();
        drawCenteredString(fontRendererObj, "LunarSpoof  •  ClickGUI", width / 2, 12, 0xFFE0D6FF);

        drawPanel(brandPanel, mouseX, mouseY);
        drawPanel(privacyPanel, mouseX, mouseY);
        drawPanel(discordPanel, mouseX, mouseY);

        drawBrandContents(mouseX, mouseY);
        drawPrivacyContents(mouseX, mouseY);
        drawDiscordContents(mouseX, mouseY);

        if (!brandPanel.collapsed) {
            customBrandField.drawTextBox();
        }
        if (!discordPanel.collapsed) {
            applicationIdField.drawTextBox();
            detailsField.drawTextBox();
            stateField.drawTextBox();
            imageKeyField.drawTextBox();
        }

        if (notice != null && !notice.isEmpty()) {
            drawCenteredString(fontRendererObj, trimForWidth(notice, Math.max(100, width - 24)), width / 2,
                    height - 20, 0xFFC9B8F2);
        }
        drawString(fontRendererObj, "ESC: close   •   Drag: move window   •   Right-click title: collapse", 8, height - 9,
                0xFF85859E);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPanel(Panel panel, int mouseX, int mouseY) {
        int panelHeight = panel.collapsed ? HEADER_HEIGHT : panel.height;
        drawRect(panel.x - 1, panel.y - 1, panel.x + panel.width + 1, panel.y + panelHeight + 1, 0xFF5E568A);
        drawRect(panel.x, panel.y, panel.x + panel.width, panel.y + panelHeight, 0xE8171A27);
        drawGradientRect(panel.x + 1, panel.y + 1, panel.x + panel.width - 1, panel.y + HEADER_HEIGHT,
                0xFF4C4775, 0xFF242943);
        if (inside(mouseX, mouseY, panel.x, panel.y, panel.x + panel.width, panel.y + HEADER_HEIGHT)) {
            drawRect(panel.x + 1, panel.y + 1, panel.x + panel.width - 1, panel.y + HEADER_HEIGHT,
                    0x28FFFFFF);
        }
        drawString(fontRendererObj, panel.title, panel.x + 7, panel.y + 5, 0xFFF0EAFE);
        drawString(fontRendererObj, panel.collapsed ? "+" : "-", panel.x + panel.width - 13, panel.y + 4,
                0xFFD7CCFF);
    }

    private void drawBrandContents(int mouseX, int mouseY) {
        if (brandPanel.collapsed) return;
        int x = brandPanel.x + PADDING;
        int y = brandPanel.y + 24;
        drawString(fontRendererObj, "Reported client brand", x, y, 0xFFD0D0E4);
        drawActionButton(x, y + 13, brandPanel.width - PADDING * 2, 18,
                config.brandMode.toUpperCase(java.util.Locale.ROOT) + "  (click to cycle)",
                inside(mouseX, mouseY, x, y + 13, x + brandPanel.width - PADDING * 2, y + 31), 0xFF51477E);
        drawString(fontRendererObj, "Custom value (custom mode only)", x, y + 39, 0xFFD0D0E4);
        drawString(fontRendererObj, "Current: " + trimForWidth(Moonlight.get().getClientBrand(), 24), x, y + 68,
                0xFFB0C6DF);
        drawString(fontRendererObj, "Reconnect to apply brand changes.", x, y + 82, 0xFF9C9CB4);
    }

    private void drawPrivacyContents(int mouseX, int mouseY) {
        if (privacyPanel.collapsed) return;
        int x = privacyPanel.x + PADDING;
        int y = privacyPanel.y + 27;
        drawToggleRow(privacyPanel, y, "Hide Forge handshake", config.hideForgeHandshake, mouseX, mouseY);
        drawToggleRow(privacyPanel, y + 26, "Lunar player icons", config.showPeerIcons, mouseX, mouseY);
        drawString(fontRendererObj, "Icons use Lunar services.", x, y + 57, 0xFF9C9CB4);
        drawString(fontRendererObj, "Off closes that connection.", x, y + 70, 0xFF9C9CB4);
    }

    private void drawDiscordContents(int mouseX, int mouseY) {
        if (discordPanel.collapsed) return;
        int x = discordPanel.x + PADDING;
        int y = discordPanel.y + 23;
        drawToggleRow(discordPanel, y, "Rich Presence", config.discordRpcEnabled, mouseX, mouseY);

        drawString(fontRendererObj, "Discord Application ID", x, y + 24, 0xFFD0D0E4);
        drawString(fontRendererObj, "Activity details", x, y + 60, 0xFFD0D0E4);
        drawString(fontRendererObj, "Activity state", x, y + 96, 0xFFD0D0E4);
        drawString(fontRendererObj, "Large image asset key", x, y + 132, 0xFFD0D0E4);
    }

    private void drawToggleRow(Panel panel, int y, String label, boolean enabled, int mouseX, int mouseY) {
        int x = panel.x + PADDING;
        int buttonX = panel.x + panel.width - 53;
        int buttonY = y - 3;
        int buttonWidth = 45;
        drawString(fontRendererObj, label, x, y, 0xFFD8D8E9);
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonX + buttonWidth, buttonY + 17);
        drawActionButton(buttonX, buttonY, buttonWidth, 17, enabled ? "ON" : "OFF", hovered,
                enabled ? 0xFF326F61 : 0xFF4A4B5E);
    }

    private void drawActionButton(int x, int y, int buttonWidth, int buttonHeight, String text, boolean hovered,
            int color) {
        drawRect(x, y, x + buttonWidth, y + buttonHeight, 0xFF090A12);
        drawRect(x + 1, y + 1, x + buttonWidth - 1, y + buttonHeight - 1,
                hovered ? blend(color, 0xFFFFFFFF, 0.22f) : color);
        drawCenteredString(fontRendererObj, trimForWidth(text, Math.max(40, buttonWidth - 6)), x + buttonWidth / 2,
                y + (buttonHeight - 8) / 2, 0xFFF4F1FF);
    }

    private void updateFieldPositions() {
        if (customBrandField != null) {
            customBrandField.xPosition = brandPanel.x + PADDING;
            customBrandField.yPosition = brandPanel.y + 73;
        }
        if (applicationIdField != null) {
            applicationIdField.xPosition = discordPanel.x + PADDING;
            applicationIdField.yPosition = discordPanel.y + 23 + 34;
            detailsField.xPosition = discordPanel.x + PADDING;
            detailsField.yPosition = discordPanel.y + 23 + 70;
            stateField.xPosition = discordPanel.x + PADDING;
            stateField.yPosition = discordPanel.y + 23 + 106;
            imageKeyField.xPosition = discordPanel.x + PADDING;
            imageKeyField.yPosition = discordPanel.y + 23 + 142;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        updateFieldPositions();
        boolean fieldClicked = false;
        if (!brandPanel.collapsed && insideField(customBrandField, mouseX, mouseY)) fieldClicked = true;
        if (!discordPanel.collapsed && (insideField(applicationIdField, mouseX, mouseY)
                || insideField(detailsField, mouseX, mouseY) || insideField(stateField, mouseX, mouseY)
                || insideField(imageKeyField, mouseX, mouseY))) fieldClicked = true;

        if (!brandPanel.collapsed) customBrandField.mouseClicked(mouseX, mouseY, mouseButton);
        if (!discordPanel.collapsed) {
            applicationIdField.mouseClicked(mouseX, mouseY, mouseButton);
            detailsField.mouseClicked(mouseX, mouseY, mouseButton);
            stateField.mouseClicked(mouseX, mouseY, mouseButton);
            imageKeyField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (fieldClicked) {
            if (mouseButton == 0) commitTextFields(false);
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        commitTextFields(false);
        unfocusFields();

        if (handlePanelHeaderClick(brandPanel, mouseX, mouseY, mouseButton)
                || handlePanelHeaderClick(privacyPanel, mouseX, mouseY, mouseButton)
                || handlePanelHeaderClick(discordPanel, mouseX, mouseY, mouseButton)) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
            return;
        }

        if (mouseButton == 0) {
            handleBrandClick(mouseX, mouseY);
            handlePrivacyClick(mouseX, mouseY);
            handleDiscordClick(mouseX, mouseY);
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private boolean handlePanelHeaderClick(Panel panel, int mouseX, int mouseY, int mouseButton) {
        if (!inside(mouseX, mouseY, panel.x, panel.y, panel.x + panel.width, panel.y + HEADER_HEIGHT)) return false;
        if (mouseButton == 1 || mouseX >= panel.x + panel.width - 20) {
            panel.collapsed = !panel.collapsed;
            if (panel.collapsed) {
                if (panel == brandPanel) customBrandField.setFocused(false);
                if (panel == discordPanel) {
                    applicationIdField.setFocused(false);
                    detailsField.setFocused(false);
                    stateField.setFocused(false);
                    imageKeyField.setFocused(false);
                }
            }
        } else if (mouseButton == 0) {
            draggingPanel = panel;
            dragOffsetX = mouseX - panel.x;
            dragOffsetY = mouseY - panel.y;
        }
        return true;
    }

    private void handleBrandClick(int mouseX, int mouseY) {
        if (brandPanel.collapsed) return;
        int x = brandPanel.x + PADDING;
        int y = brandPanel.y + 37;
        if (inside(mouseX, mouseY, x, y, x + brandPanel.width - PADDING * 2, y + 18)) {
            String[] modes = {"lunar", "vanilla", "forge", "custom"};
            int current = 0;
            for (int i = 0; i < modes.length; i++) {
                if (modes[i].equals(config.brandMode)) current = i;
            }
            config.brandMode = modes[(current + 1) % modes.length];
            applySettings(false);
            notice = "Brand set to " + config.brandMode + ". Reconnect to apply it.";
        }
    }

    private void handlePrivacyClick(int mouseX, int mouseY) {
        if (privacyPanel.collapsed) return;
        int rowX = privacyPanel.x + privacyPanel.width - 53;
        int y = privacyPanel.y + 24;
        if (inside(mouseX, mouseY, rowX, y, rowX + 45, y + 17)) {
            config.hideForgeHandshake = !config.hideForgeHandshake;
            applySettings(false);
            notice = "Forge/FML handshake hiding " + (config.hideForgeHandshake ? "enabled." : "disabled.");
        } else if (inside(mouseX, mouseY, rowX, y + 26, rowX + 45, y + 43)) {
            config.showPeerIcons = !config.showPeerIcons;
            if (!config.showPeerIcons) {
                SubscriptionManager.disconnect();
                notice = "Lunar icons disabled; Lunar service connection closed.";
            } else {
                notice = "Lunar icons enabled. Reconnect to a server to activate lookup.";
            }
            applySettings(false);
        }
    }

    private void handleDiscordClick(int mouseX, int mouseY) {
        if (discordPanel.collapsed) return;
        int buttonX = discordPanel.x + discordPanel.width - 53;
        int y = discordPanel.y + 23;
        if (!inside(mouseX, mouseY, buttonX, y - 3, buttonX + 45, y + 14)) return;

        commitTextFields(true);
        if (!config.discordRpcEnabled && !LunarSpoofConfig.isValidApplicationId(config.discordApplicationId)) {
            notice = "Enter a valid Discord Application ID before enabling Rich Presence.";
            return;
        }
        config.discordRpcEnabled = !config.discordRpcEnabled;
        config.save();
        DiscordPresenceManager.refresh();
        notice = "Discord Rich Presence " + (config.discordRpcEnabled ? "enabled." : "disabled.");
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0) draggingPanel = null;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) {
            super.keyTyped(typedChar, keyCode);
            return;
        }

        boolean changed = customBrandField.textboxKeyTyped(typedChar, keyCode);
        changed |= applicationIdField.textboxKeyTyped(typedChar, keyCode);
        changed |= detailsField.textboxKeyTyped(typedChar, keyCode);
        changed |= stateField.textboxKeyTyped(typedChar, keyCode);
        changed |= imageKeyField.textboxKeyTyped(typedChar, keyCode);
        if (changed) {
            commitTextFields(false);
            notice = "Text edited. Close the GUI or toggle Rich Presence to save and apply.";
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        commitTextFields(true);
        Moonlight.get().configureBrand(config.brandMode, config.customBrand);
        DiscordPresenceManager.refresh();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void commitTextFields(boolean save) {
        if (customBrandField != null) config.customBrand = LunarSpoofConfig.trimTo(customBrandField.getText(), 128);
        if (applicationIdField != null) config.discordApplicationId = applicationIdField.getText().trim();
        if (detailsField != null) config.discordDetails = LunarSpoofConfig.trimTo(detailsField.getText(), 128);
        if (stateField != null) config.discordState = LunarSpoofConfig.trimTo(stateField.getText(), 128);
        if (imageKeyField != null) config.discordLargeImageKey = LunarSpoofConfig.trimTo(imageKeyField.getText(), 256);
        if (save) {
            config.save();
            Moonlight.get().configureBrand(config.brandMode, config.customBrand);
        }
    }

    private void applySettings(boolean refreshRpc) {
        config.save();
        Moonlight.get().configureBrand(config.brandMode, config.customBrand);
        if (refreshRpc) DiscordPresenceManager.refresh();
    }

    private void unfocusFields() {
        customBrandField.setFocused(false);
        applicationIdField.setFocused(false);
        detailsField.setFocused(false);
        stateField.setFocused(false);
        imageKeyField.setFocused(false);
    }

    private static boolean insideField(GuiTextField field, int mouseX, int mouseY) {
        return inside(mouseX, mouseY, field.xPosition, field.yPosition, field.xPosition + field.getWidth(),
                field.yPosition + 18);
    }

    private static boolean inside(int mouseX, int mouseY, int left, int top, int right, int bottom) {
        return mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
    }

    private String trimForWidth(String text, int approximateCharacters) {
        if (text == null) return "";
        String trimmed = text.replace('\n', ' ');
        if (trimmed.length() <= approximateCharacters) return trimmed;
        return trimmed.substring(0, Math.max(0, approximateCharacters - 3)) + "...";
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int blend(int color1, int color2, float factor) {
        int a = (color1 >>> 24) & 0xFF;
        int r = (color1 >>> 16) & 0xFF;
        int g = (color1 >>> 8) & 0xFF;
        int b = color1 & 0xFF;
        int ar = (color2 >>> 24) & 0xFF;
        int rr = (color2 >>> 16) & 0xFF;
        int gr = (color2 >>> 8) & 0xFF;
        int br = color2 & 0xFF;
        return ((int) (a + (ar - a) * factor) << 24)
                | ((int) (r + (rr - r) * factor) << 16)
                | ((int) (g + (gr - g) * factor) << 8)
                | (int) (b + (br - b) * factor);
    }

    private static final class Panel {
        private final String title;
        private final int width;
        private final int height;
        private int x;
        private int y;
        private boolean collapsed;

        private Panel(String title, int x, int y, int width, int height) {
            this.title = title;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
