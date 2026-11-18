package kote.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Deque;

public class KoteHud {
    private static final int WHITE = 0xFFFFFFFF;
    
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    // CPS tracking (polled every frame)
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean leftWas = false, rightWas = false;

    private static int cps(Deque<Long> q, long now) {
        while (!q.isEmpty() && now - q.peekFirst() > 1000) q.pollFirst();
        return q.size();
    }

    private static String dirName(String d) {
        return switch (d) {
            case "north" -> "Север";
            case "south" -> "Юг";
            case "east" -> "Восток";
            case "west" -> "Запад";
            default -> d;
        };
    }

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        int accent = KoteConfig.accent(0);
        int PANEL = KoteConfig.panelColor();
        long now = System.currentTimeMillis();

        boolean left = mc.options.keyAttack.isDown();
        boolean right = mc.options.keyUse.isDown();
        if (left && !leftWas) leftClicks.addLast(now);
        if (right && !rightWas) rightClicks.addLast(now);
        leftWas = left; rightWas = right;
        int lcps = cps(leftClicks, now), rcps = cps(rightClicks, now);

        // Info panel (top-left)
        int lines = 0;
        if (KoteConfig.WATERMARK.enabled) lines++;
        if (KoteConfig.FPS.enabled) lines++;
        if (KoteConfig.PING.enabled) lines++;
        if (KoteConfig.COORDS.enabled) lines++;
        if (KoteConfig.SPEED.enabled) lines++;
        if (KoteConfig.DIRECTION.enabled) lines++;
        if (KoteConfig.TIME.enabled) lines++;
        if (KoteConfig.CPS.enabled) lines++;
        if (lines > 0) {
            int x = 6, y = 6, w = 124, h = lines * 11 + 6;
            g.fill(x, y, x + w, y + h, PANEL);
            g.fill(x, y, x + 2, y + h, accent);
            int ty = y + 4;
            if (KoteConfig.WATERMARK.enabled) { g.drawString(mc.font, "Kote Client", x + 7, ty, accent, true); ty += 11; }
            if (KoteConfig.FPS.enabled) { g.drawString(mc.font, "FPS: " + mc.getFps(), x + 7, ty, WHITE, true); ty += 11; }
            if (KoteConfig.PING.enabled) {
                int ping = 0;
                if (mc.getConnection() != null) {
                    var info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                    if (info != null) ping = info.getLatency();
                }
                g.drawString(mc.font, "Ping: " + ping + " ms", x + 7, ty, WHITE, true); ty += 11;
            }
            if (KoteConfig.COORDS.enabled) {
                g.drawString(mc.font, String.format("XYZ: %d %d %d", (int) Math.floor(mc.player.getX()),
                        (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())), x + 7, ty, WHITE, true);
                ty += 11;
            }
            if (KoteConfig.SPEED.enabled) {
                var v = mc.player.getDeltaMovement();
                double bps = Math.sqrt(v.x * v.x + v.z * v.z) * 20.0;
                g.drawString(mc.font, String.format("Скорость: %.1f б/с", bps), x + 7, ty, WHITE, true); ty += 11;
            }
            if (KoteConfig.DIRECTION.enabled) {
                g.drawString(mc.font, "Куда: " + dirName(mc.player.getDirection().getSerializedName()), x + 7, ty, WHITE, true);
                ty += 11;
            }
            if (KoteConfig.TIME.enabled) {
                g.drawString(mc.font, "Время: " + LocalTime.now().format(TIME), x + 7, ty, WHITE, true); ty += 11;
            }
            if (KoteConfig.CPS.enabled) {
                g.drawString(mc.font, "CPS: " + lcps + " | " + rcps, x + 7, ty, WHITE, true);
            }
        }

        // Keystrokes (left side, middle)
        if (KoteConfig.KEYS.enabled) {
            int kx = 8, ky = g.guiHeight() / 2 - 20;
            key(g, mc, kx + 24, ky, 20, "W", mc.options.keyUp.isDown());
            key(g, mc, kx, ky + 22, 20, "A", mc.options.keyLeft.isDown());
            key(g, mc, kx + 24, ky + 22, 20, "S", mc.options.keyDown.isDown());
            key(g, mc, kx + 48, ky + 22, 20, "D", mc.options.keyRight.isDown());
            key(g, mc, kx, ky + 44, 32, "ЛКМ", left);
            key(g, mc, kx + 36, ky + 44, 32, "ПКМ", right);
            key(g, mc, kx, ky + 66, 68, "ПРОБЕЛ", mc.options.keyJump.isDown());
        }

        // Список включённых модулей (справа сверху)
        if (KoteConfig.ARRAYLIST.enabled) {
            java.util.List<String> names = new java.util.ArrayList<>();
            for (KoteConfig.Mod m : KoteConfig.ALL) {
                if (m.enabled && !m.alwaysOn && m != KoteConfig.ARRAYLIST) names.add(m.name);
            }
            names.sort((a, b) -> Integer.compare(mc.font.width(b), mc.font.width(a)));
            int ly = 4, li = 0;
            for (String n : names) {
                int w = mc.font.width(n) + 9;
                int lx = g.guiWidth() - w - 3;
                g.fill(lx, ly, lx + w, ly + 11, PANEL);
                g.fill(lx + w - 2, ly, lx + w, ly + 11, KoteConfig.accent(li));
                g.drawString(mc.font, n, lx + 3, ly + 2, WHITE, true);
                ly += 11; li++;
            }
        }

        // Armor (bottom-right)
        if (KoteConfig.ARMOR.enabled) {
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
        if (KoteConfig.EFFECTS.enabled) {
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

    private static void key(GuiGraphics g, Minecraft mc, int x, int y, int w, String label, boolean down) {
        int accent = KoteConfig.accent(0);
        int bg = down ? ((0xAA << 24) | (accent & 0xFFFFFF)) : KoteConfig.panelColor();
        g.fill(x, y, x + w, y + 20, bg);
        g.drawCenteredString(mc.font, label, x + w / 2, y + 6, WHITE);
    }
}
