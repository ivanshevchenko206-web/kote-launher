package kote.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class KoteScreen extends Screen {
    private static final int PW = 122, HH = 20, RH = 16, SH = 14;
    private static final int GREY = 0xFFB9AFD6, WHITE = 0xFFFFFFFF;

    private record Hit(int x, int y, int w, int h, int type, String cat, KoteConfig.Mod m, KoteConfig.Setting s) { }

    private final List<Hit> hits = new ArrayList<>();
    private String dragCat = null;
    private int dragOffX, dragOffY;
    private KoteConfig.Setting dragSlider = null;
    private int sliderX, sliderW;
    private long lastNs = System.nanoTime();

    public KoteScreen() { super(Component.literal("Kote Client")); }

    @Override
    protected void init() {
        for (int i = 0; i < KoteConfig.CATEGORIES.length; i++) {
            final int idx = i;
            KoteConfig.PANELS.computeIfAbsent(KoteConfig.CATEGORIES[i], k -> new int[]{30 + idx * (PW + 16), 30});
        }
    }

    private static int darken(int c) {
        int r = (c >> 16 & 0xFF) * 55 / 100, g = (c >> 8 & 0xFF) * 55 / 100, b = (c & 0xFF) * 55 / 100;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        long now = System.nanoTime();
        float dt = (now - lastNs) / 1_000_000_000f;
        lastNs = now;
        hits.clear();
        int accent = KoteConfig.accent(0);
        int bg = KoteConfig.panelColor();

        for (String cat : KoteConfig.CATEGORIES) {
            int[] pos = KoteConfig.PANELS.get(cat);
            pos[0] = Math.max(0, Math.min(this.width - PW, pos[0]));
            pos[1] = Math.max(0, Math.min(this.height - HH, pos[1]));
            int x = pos[0], y = pos[1];

            g.fillGradient(x, y, x + PW, y + HH, accent, darken(accent));
            g.drawCenteredString(this.font, cat, x + PW / 2, y + 6, WHITE);
            hits.add(new Hit(x, y, PW, HH, 0, cat, null, null));

            int cy = y + HH;
            for (KoteConfig.Mod m : KoteConfig.ALL) {
                if (!m.category.equals(cat)) continue;
                float target = m.enabled ? 1f : 0f;
                m.anim += (target - m.anim) * Math.min(1f, dt * 12f);

                g.fill(x, cy, x + PW, cy + RH, bg);
                if (m.anim > 0.02f) {
                    int a = (int) (m.anim * 140);
                    g.fill(x, cy, x + PW, cy + RH, (a << 24) | (accent & 0xFFFFFF));
                    g.fill(x, cy, x + 2, cy + RH, accent);
                }
                g.drawString(this.font, m.name, x + 7, cy + 4, m.enabled ? WHITE : GREY, false);
                if (!m.settings.isEmpty()) {
                    g.drawString(this.font, m.expanded ? "-" : "+", x + PW - 10, cy + 4, GREY, false);
                }
                hits.add(new Hit(x, cy, PW, RH, 1, cat, m, null));
                cy += RH;

                if (m.expanded) {
                    for (KoteConfig.Setting s : m.settings) {
                        g.fill(x, cy, x + PW, cy + SH, 0xCC0B0813);
                        if (!s.mode) {
                            double t = (s.value - s.min) / (s.max - s.min);
                            g.fill(x + 4, cy + SH - 3, x + 4 + (int) ((PW - 8) * t), cy + SH - 1, accent);
                        }
                        g.drawString(this.font, s.name + ": " + s.display(), x + 7, cy + 2, GREY, false);
                        hits.add(new Hit(x, cy, PW, SH, 2, cat, m, s));
                        cy += SH;
                    }
                }
            }
            g.fill(x, cy, x + PW, cy + 2, accent);
        }
        g.drawCenteredString(this.font, "ЛКМ: вкл/выкл   ПКМ: настройки   Shift: закрыть", this.width / 2, this.height - 14, 0xFF8D82A8);
    }

    private void applySlider(double mx) {
        if (dragSlider == null) return;
        double t = Math.max(0, Math.min(1, (mx - sliderX - 4) / (sliderW - 8)));
        dragSlider.value = Math.round(dragSlider.min + t * (dragSlider.max - dragSlider.min));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int b = event.button();
        for (Hit h : hits) {
            if (mx < h.x() || mx >= h.x() + h.w() || my < h.y() || my >= h.y() + h.h()) continue;
            if (h.type() == 0) {
                if (b == 0) {
                    dragCat = h.cat();
                    int[] p = KoteConfig.PANELS.get(h.cat());
                    dragOffX = (int) mx - p[0];
                    dragOffY = (int) my - p[1];
                }
                return true;
            } else if (h.type() == 1) {
                KoteConfig.Mod m = h.m();
                if (b == 0 && !m.alwaysOn) {
                    m.enabled = !m.enabled;
                } else if (!m.settings.isEmpty()) {
                    m.expanded = !m.expanded;
                }
                return true;
            } else {
                KoteConfig.Setting s = h.s();
                if (s.mode) {
                    int n = s.options.length;
                    s.value = (s.value + (b == 1 ? n - 1 : 1)) % n;
                } else {
                    dragSlider = s;
                    sliderX = h.x();
                    sliderW = h.w();
                    applySlider(mx);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragCat != null) {
            int[] p = KoteConfig.PANELS.get(dragCat);
            p[0] = (int) event.x() - dragOffX;
            p[1] = (int) event.y() - dragOffY;
            return true;
        }
        if (dragSlider != null) {
            applySlider(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragCat = null;
        dragSlider = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void removed() {
        KoteConfig.save();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
