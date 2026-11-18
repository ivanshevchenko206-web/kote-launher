package kote.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class KoteScreen extends Screen {
    public KoteScreen() { super(Component.literal("Kote Client")); }

    private static String onOff(String name, boolean v) { return name + ": " + (v ? "ВКЛ" : "ВЫКЛ"); }

    private void toggle(int x, int y, String name, java.util.function.BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        addRenderableWidget(Button.builder(Component.literal(onOff(name, get.getAsBoolean())), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(Component.literal(onOff(name, get.getAsBoolean())));
        }).bounds(x, y, 150, 20).build());
    }

    @Override
    protected void init() {
        int cx = this.width / 2 - 75, y = this.height / 2 - 80;
        toggle(cx, y, "Название", () -> KoteConfig.showWatermark, v -> KoteConfig.showWatermark = v);
        toggle(cx, y + 24, "FPS", () -> KoteConfig.showFps, v -> KoteConfig.showFps = v);
        toggle(cx, y + 48, "Пинг", () -> KoteConfig.showPing, v -> KoteConfig.showPing = v);
        toggle(cx, y + 72, "Координаты", () -> KoteConfig.showCoords, v -> KoteConfig.showCoords = v);
        toggle(cx, y + 96, "Броня", () -> KoteConfig.showArmor, v -> KoteConfig.showArmor = v);
        toggle(cx, y + 120, "Эффекты", () -> KoteConfig.showEffects, v -> KoteConfig.showEffects = v);
        addRenderableWidget(Button.builder(Component.literal("Цвет: " + KoteConfig.ACCENT_NAMES[KoteConfig.accent]), b -> {
            KoteConfig.accent = (KoteConfig.accent + 1) % KoteConfig.ACCENTS.length;
            b.setMessage(Component.literal("Цвет: " + KoteConfig.ACCENT_NAMES[KoteConfig.accent]));
        }).bounds(cx, y + 148, 150, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int cx = this.width / 2, top = this.height / 2 - 100;
        g.fill(cx - 95, top, cx + 95, top + 205, 0xCC100C1C);
        g.fill(cx - 95, top, cx + 95, top + 3, KoteConfig.accentColor());
        g.drawCenteredString(this.font, "Kote Client", cx, top + 10, KoteConfig.accentColor());
        super.render(g, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
