package kote.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class KoteConfig {
    public static final String[] CATEGORIES = {"ИНФО", "ИГРОК", "ВИЗУАЛ"};
    public static final String[] ACCENT_NAMES = {"Фиолетовый", "Голубой", "Розовый", "Зелёный", "Жёлтый", "Радуга"};
    public static final int[] ACCENTS = {0xFFB388FF, 0xFF4FC3F7, 0xFFFF6E9C, 0xFF69F0AE, 0xFFFFD54F};

    public static class Setting {
        public final String id, name;
        public final boolean mode;
        public final String[] options;
        public final double min, max;
        public double value;

        Setting(String id, String name, double min, double max, double def) {
            this.id = id; this.name = name; this.mode = false; this.options = null;
            this.min = min; this.max = max; this.value = def;
        }

        Setting(String id, String name, String[] options, int def) {
            this.id = id; this.name = name; this.mode = true; this.options = options;
            this.min = 0; this.max = options.length - 1; this.value = def;
        }

        public String display() {
            return mode ? options[(int) value] : String.valueOf((int) Math.round(value));
        }
    }

    public static class Mod {
        public final String id, name, category;
        public final boolean alwaysOn;
        public boolean enabled;
        public boolean expanded;
        public float anim;
        public final List<Setting> settings = new ArrayList<>();

        Mod(String id, String name, String category, boolean enabled, boolean alwaysOn) {
            this.id = id; this.name = name; this.category = category;
            this.enabled = enabled; this.alwaysOn = alwaysOn;
            this.anim = enabled ? 1f : 0f;
        }

        Mod slider(String sid, String sname, double min, double max, double def) {
            settings.add(new Setting(sid, sname, min, max, def)); return this;
        }

        Mod mode(String sid, String sname, String[] options, int def) {
            settings.add(new Setting(sid, sname, options, def)); return this;
        }

        public Setting setting(String sid) {
            for (Setting s : settings) if (s.id.equals(sid)) return s;
            throw new IllegalArgumentException(sid);
        }
    }

    public static final List<Mod> ALL = new ArrayList<>();
    public static final Map<String, int[]> PANELS = new LinkedHashMap<>();

    private static Mod add(String id, String name, String cat, boolean def) {
        Mod m = new Mod(id, name, cat, def, false);
        ALL.add(m);
        return m;
    }

    // ИНФО
    public static final Mod WATERMARK = add("watermark", "Название", "ИНФО", true);
    public static final Mod FPS = add("fps", "FPS", "ИНФО", true);
    public static final Mod PING = add("ping", "Пинг", "ИНФО", true);
    public static final Mod COORDS = add("coords", "Координаты", "ИНФО", true);
    public static final Mod SPEED = add("speed", "Скорость", "ИНФО", false);
    public static final Mod DIRECTION = add("direction", "Направление", "ИНФО", false);
    public static final Mod TIME = add("time", "Время", "ИНФО", false);
    public static final Mod CPS = add("cps", "CPS", "ИНФО", true);
    // ИГРОК
    public static final Mod KEYS = add("keys", "Клавиши", "ИГРОК", true);
    public static final Mod ARMOR = add("armor", "Броня", "ИГРОК", true);
    public static final Mod EFFECTS = add("effects", "Эффекты", "ИГРОК", true);
    public static final Mod ZOOM = add("zoom", "Зум (C)", "ИГРОК", true).slider("fov", "Угол", 30, 60, 30);
    // ВИЗУАЛ
    public static final Mod ARRAYLIST = add("arraylist", "Список модулей", "ВИЗУАЛ", true);
    public static final Mod BRIGHTNESS = add("brightness", "Макс. яркость", "ВИЗУАЛ", false);
    public static final Mod THEME;

    static {
        THEME = new Mod("theme", "Тема", "ВИЗУАЛ", true, true)
                .mode("color", "Цвет", ACCENT_NAMES, 0)
                .slider("bg", "Фон", 20, 90, 60);
        ALL.add(THEME);
    }

    private static int hsv(float h) {
        float s = 0.55f, v = 1f;
        float r, g, b;
        int i = (int) (h * 6);
        float f = h * 6 - i, p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    /** Цвет оформления. offset сдвигает фазу для радуги (градиент по списку). */
    public static int accent(int offset) {
        int idx = (int) THEME.setting("color").value;
        if (idx >= ACCENTS.length) {
            float h = ((System.currentTimeMillis() + offset * 90L) % 6000L) / 6000f;
            return hsv(h);
        }
        return ACCENTS[idx];
    }

    public static int panelColor() {
        int a = (int) (THEME.setting("bg").value / 100.0 * 255);
        return (a << 24) | 0x100C1C;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("koteclient.properties");
    }

    public static void load() {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file())) {
            p.load(in);
        } catch (Exception ignored) { }
        for (Mod m : ALL) {
            if (!m.alwaysOn) m.enabled = Boolean.parseBoolean(p.getProperty(m.id + ".on", String.valueOf(m.enabled)));
            m.anim = m.enabled ? 1f : 0f;
            for (Setting s : m.settings) {
                try {
                    s.value = Double.parseDouble(p.getProperty(m.id + "." + s.id, String.valueOf(s.value)));
                } catch (NumberFormatException ignored) { }
            }
        }
        for (String c : CATEGORIES) {
            try {
                int x = Integer.parseInt(p.getProperty("panel." + c + ".x"));
                int y = Integer.parseInt(p.getProperty("panel." + c + ".y"));
                PANELS.put(c, new int[]{x, y});
            } catch (Exception ignored) { }
        }
    }

    public static void save() {
        Properties p = new Properties();
        for (Mod m : ALL) {
            p.setProperty(m.id + ".on", String.valueOf(m.enabled));
            for (Setting s : m.settings) p.setProperty(m.id + "." + s.id, String.valueOf(s.value));
        }
        for (Map.Entry<String, int[]> e : PANELS.entrySet()) {
            p.setProperty("panel." + e.getKey() + ".x", String.valueOf(e.getValue()[0]));
            p.setProperty("panel." + e.getKey() + ".y", String.valueOf(e.getValue()[1]));
        }
        try (OutputStream out = Files.newOutputStream(file())) {
            p.store(out, "Kote Client");
        } catch (Exception ignored) { }
    }
}
