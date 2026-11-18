package kote.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public class KoteHud {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int PANEL = 0x99100C1C;

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int accent = KoteConfig.accentColor();

        // Info panel (top-left)
        int lines = 0;
        if (KoteConfig.showWatermark) lines++;
        if (KoteConfig.showFps) lines++;
        if (KoteConfig.showPing) lines++;
        if (KoteConfig.showCoords) lines++;
        if (lines > 0) {
            int x = 6, y = 6, w = 118, h = lines * 11 + 6;
            g.fill(x, y, x + w, y + h, PANEL);
            g.fill(x, y, x + 2, y + h, accent);
            int ty = y + 4;
            if (KoteConfig.showWatermark) { g.drawString(mc.font, "Kote Client", x + 7, ty, accent, true); ty += 11; }
            if (KoteConfig.showFps) { g.drawString(mc.font, "FPS: " + mc.getFps(), x + 7, ty, WHITE, true); ty += 11; }
            if (KoteConfig.showPing) {
                int ping = 0;
                if (mc.getConnection() != null) {
                    var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (info != null) ping = info.getLatency();
                }
                g.drawString(mc.font, "Ping: " + ping + " ms", x + 7, ty, WHITE, true); ty += 11;
            }
            if (KoteConfig.showCoords) {
                g.drawString(mc.font, String.format("XYZ: %d %d %d", (int) Math.floor(mc.player.getX()),
                        (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())), x + 7, ty, WHITE, true);
            }
        }

        // Armor (bottom-right)
        if (KoteConfig.showArmor) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            int ax = g.guiWidth() - 22, ay = g.guiHeight() - 4 * 18 - 40;
            for (int i = 0; i < slots.length; i++) {
                ItemStack st = mc.player.getItemBySlot(slots[i]);
                if (st.isEmpty()) continue;
                g.renderItem(st, ax, ay + i * 18);
                g.renderItemDecorations(mc.font, st, ax, ay + i * 18);
            }
        }

        // Effects (left, under info panel)
        if (KoteConfig.showEffects) {
            int ey = 6 + lines * 11 + 14;
            for (MobEffectInstance e : mc.player.getActiveEffects()) {
                String name = e.getEffect().value().getDisplayName().getString();
                int amp = e.getAmplifier() + 1;
                int sec = e.getDuration() / 20;
                String text = name + (amp > 1 ? " " + amp : "") + "  " + (sec / 60) + ":" + String.format("%02d", sec % 60);
                int w = mc.font.width(text) + 10;
                g.fill(6, ey, 6 + w, ey + 12, PANEL);
                g.fill(6, ey, 8, ey + 12, accent);
                g.drawString(mc.font, text, 12, ey + 2, WHITE, true);
                ey += 14;
            }
        }
    }
}
