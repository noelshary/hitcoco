package dev.hitcolor;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Small, low-overhead client telemetry and HUD renderer. */
public final class ClientHubHud {
    private static final Deque<Long> leftClicks = new ArrayDeque<>();
    private static final Deque<Long> rightClicks = new ArrayDeque<>();
    private static boolean leftWasDown;
    private static boolean rightWasDown;
    private static long lastWorldTime = Long.MIN_VALUE;
    private static long tpsSampleStart = System.currentTimeMillis();
    private static long tpsSampleTicks;
    private static float measuredTps = 20f;
    private static long lastPlaytimeSave = System.currentTimeMillis();
    private static long lastMousePoll;
    private static boolean registered;

    private ClientHubHud() {}

    public static void register() {
        if (registered) return;
        registered = true;
        HudElementRegistry.addLast(Identifier.of("hitcolor", "client_hud"),
            (DrawContext context, RenderTickCounter tickCounter) -> render(context));
    }

    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        if (client == null) return;

        if (now - lastMousePoll >= 25L && client.getWindow() != null) {
            lastMousePoll = now;
            long handle = client.getWindow().getHandle();
            boolean l = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean r = GLFW.glfwGetMouseButton(handle, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
            if (l && !leftWasDown) leftClicks.addLast(now);
            if (r && !rightWasDown) rightClicks.addLast(now);
            leftWasDown = l;
            rightWasDown = r;
        }
        prune(leftClicks, now);
        prune(rightClicks, now);

        if (client.world != null) {
            long worldTime = client.world.getTime();
            if (lastWorldTime == Long.MIN_VALUE) {
                lastWorldTime = worldTime;
                tpsSampleStart = now;
                tpsSampleTicks = 0L;
            } else if (worldTime > lastWorldTime) {
                tpsSampleTicks += Math.min(100L, worldTime - lastWorldTime);
                lastWorldTime = worldTime;
            }
            long elapsed = now - tpsSampleStart;
            if (elapsed >= 1000L) {
                measuredTps = Math.max(0f, Math.min(20f, tpsSampleTicks * 1000f / elapsed));
                tpsSampleTicks = 0L;
                tpsSampleStart = now;
            }
        } else {
            lastWorldTime = Long.MIN_VALUE;
            measuredTps = 20f;
        }

        if (now - lastPlaytimeSave >= 30_000L) {
            ClientHubConfig.get().playtimeMillis += now - lastPlaytimeSave;
            lastPlaytimeSave = now;
            ClientHubConfig.save();
        }
    }

    private static void prune(Deque<Long> clicks, long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000L) clicks.removeFirst();
    }

    public static int leftCps() { return leftClicks.size(); }
    public static int rightCps() { return rightClicks.size(); }
    public static float tps() { return measuredTps; }

    public static int ping() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) return -1;
        PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(client.player.getUuid());
        return entry == null ? -1 : entry.getLatency();
    }

    public static String playtime() {
        long millis = ClientHubConfig.get().playtimeMillis + Math.max(0L, System.currentTimeMillis() - lastPlaytimeSave);
        long totalSeconds = millis / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds / 60L) % 60L;
        return hours + "h " + String.format("%02dm", minutes);
    }

    public static String clock() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    private static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientHubConfig cfg = ClientHubConfig.get();
        if (!cfg.hudEnabled || client.player == null || client.world == null || client.currentScreen != null) return;

        List<String> modules = new ArrayList<>();
        if (cfg.showFps) modules.add("FPS  " + client.getCurrentFps());
        if (cfg.showCps) modules.add("CPS  " + leftCps() + " / " + rightCps());
        if (cfg.showPing) modules.add("PING  " + (ping() < 0 ? "—" : ping() + " ms"));
        if (cfg.showTps) modules.add("TPS  " + String.format("%.1f", tps()));
        if (cfg.showClock) modules.add("TIME  " + clock());
        if (cfg.showPlaytime) modules.add("PLAY  " + playtime());
        if (modules.isEmpty()) return;

        int x = 12;
        int y = 12;
        int accent = ClientHubConfig.accent();
        for (String label : modules) {
            int w = Math.min(190, client.textRenderer.getWidth(label) + 18);
            context.fill(x, y, x + w, y + 19, 0xD9101420);
            context.fill(x, y, x + 2, y + 19, 0xFF000000 | accent);
            context.drawTextWithShadow(client.textRenderer, Text.literal(label), x + 8, y + 6, 0xFFF3F5FA);
            x += w + 5;
            if (x > client.getWindow().getScaledWidth() - 200) {
                x = 12;
                y += 23;
            }
        }
    }

    /** Called by the crosshair mixin only when the user's custom crosshair is enabled. */
    public static void drawCustomCrosshair(DrawContext context) {
        ClientHubConfig cfg = ClientHubConfig.get();
        MinecraftClient client = MinecraftClient.getInstance();
        int cx = client.getWindow().getScaledWidth() / 2;
        int cy = client.getWindow().getScaledHeight() / 2;
        int color = 0xFF000000 | (cfg.crosshairColor & 0xFFFFFF);
        int size = Math.round(cfg.crosshairSize);
        int gap = Math.round(cfg.crosshairGap);
        int thick = Math.max(1, Math.round(cfg.crosshairThickness));

        if ("DOT".equals(cfg.crosshairShape)) {
            for (int yy = -thick; yy <= thick; yy++) {
                for (int xx = -thick; xx <= thick; xx++) {
                    if (xx * xx + yy * yy <= thick * thick + 1) {
                        context.fill(cx + xx, cy + yy, cx + xx + 1, cy + yy + 1, color);
                    }
                }
            }
        } else if ("RING".equals(cfg.crosshairShape)) {
            float radius = Math.max(3f, size);
            for (int angle = 0; angle < 360; angle += 12) {
                double radians = Math.toRadians(angle);
                int px = cx + Math.round((float) Math.cos(radians) * radius);
                int py = cy + Math.round((float) Math.sin(radians) * radius);
                context.fill(px, py, px + thick, py + thick, color);
            }
        } else {
            context.fill(cx - thick / 2, cy - size - gap, cx + (thick + 1) / 2, cy - gap, color);
            context.fill(cx - thick / 2, cy + gap + 1, cx + (thick + 1) / 2, cy + size + gap + 1, color);
            context.fill(cx - size - gap, cy - thick / 2, cx - gap, cy + (thick + 1) / 2, color);
            context.fill(cx + gap + 1, cy - thick / 2, cx + size + gap + 1, cy + (thick + 1) / 2, color);
        }
    }
}
