package kote.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public class KoteClient implements ClientModInitializer {
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("koteclient", "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.koteclient.menu", InputConstants.Type.KEYSYM, GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (menuKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new KoteScreen());
            }
        });

        HudElementRegistry.attachElementAfter(VanillaHudElements.MISC_OVERLAYS,
                Identifier.fromNamespaceAndPath("koteclient", "hud"),
                (graphics, tickCounter) -> KoteHud.render(graphics));
    }

    private static final int GLFW_KEY_RIGHT_SHIFT = GLFW.GLFW_KEY_RIGHT_SHIFT;
}
