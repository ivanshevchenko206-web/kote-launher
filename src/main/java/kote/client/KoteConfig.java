package kote.client;

public class KoteConfig {
    public static boolean showWatermark = true;
    public static boolean showFps = true;
    public static boolean showPing = true;
    public static boolean showCoords = true;
    public static boolean showArmor = true;
    public static boolean showEffects = true;

    public static final int[] ACCENTS = {0xFFB388FF, 0xFF4FC3F7, 0xFFFF6E9C, 0xFF69F0AE, 0xFFFFD54F};
    public static final String[] ACCENT_NAMES = {"Фиолетовый", "Голубой", "Розовый", "Зелёный", "Жёлтый"};
    public static int accent = 0;

    public static int accentColor() { return ACCENTS[accent]; }
}
