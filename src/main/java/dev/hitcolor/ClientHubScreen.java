package dev.hitcolor;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Animated, modular Right Shift client hub. */
public final class ClientHubScreen extends Screen {
    private enum Page { HOME, HITCOLOR, HUD, THEME, CROSSHAIR, MODULES }
    private final Screen parent;
    private Page page = Page.HOME;
    private long openedAt = Util.getMeasuringTimeMs();
    private int panelX, panelY, panelW, panelH;
    private static final int BG = 0xF20B1019;
    private static final int PANEL = 0xE9141C29;
    private static final int MUTED = 0xFF8D9AAF;
    private static final int WHITE = 0xFFF2F5FA;

    public ClientHubScreen(Screen parent) {
        super(Text.literal("HitColor Client"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(760, Math.max(560, width - 36));
        panelH = Math.min(440, Math.max(340, height - 28));
        panelX = (width - panelW) / 2;
        panelY = (height - panelH) / 2;
        int navX = panelX + 18;
        int navY = panelY + 62;
        String[] labels = {"Overview", "HitColor", "HUD", "Theme", "Crosshair", "Modules"};
        Page[] pages = Page.values();
        int navW = (panelW - 36 - 5 * 5) / 6;
        for (int i = 0; i < pages.length; i++) {
            final Page target = pages[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(labels[i]), b -> {
                page = target;
                clearAndInit();
            }).dimensions(navX + i * (navW + 5), navY, navW, 22).build());
        }
        buildPage();
    }

    private void buildPage() {
        int x = panelX + 22;
        int y = panelY + 112;
        int colW = (panelW - 58) / 2;
        switch (page) {
            case HOME -> {
                button("Open HitColor editor", x, y, colW, () -> page(Page.HITCOLOR));
                button("Configure HUD widgets", x + colW + 14, y, colW, () -> page(Page.HUD));
                button("Choose client theme", x, y + 32, colW, () -> page(Page.THEME));
                button("Crosshair editor", x + colW + 14, y + 32, colW, () -> page(Page.CROSSHAIR));
                button("Optional mod integrations", x, y + 64, panelW - 44, () -> page(Page.MODULES));
            }
            case HITCOLOR -> {
                HitColorConfig c = HitColorConfig.get();
                button("HitColor: " + onOff(c.enabled), x, y, colW, () -> { c.enabled = !c.enabled; HitColorConfig.save(); page(Page.HITCOLOR); });
                button("Animation: " + c.animation.name(), x + colW + 14, y, colW, () -> { c.animation = HitColorConfig.Animation.values()[(c.animation.ordinal()+1)%HitColorConfig.Animation.values().length]; HitColorConfig.save(); page(Page.HITCOLOR); });
                slider("Hit strength", x, y + 38, colW, c.strength, v -> c.strength = (float)v, HitColorConfig::save);
                slider("Animation speed", x + colW + 14, y + 38, colW, (c.speed - .25f) / 1.75f, v -> c.speed = .25f + (float)v * 1.75f, HitColorConfig::save);
                button("Particles: " + onOff(c.particles), x, y + 78, colW, () -> { c.particles = !c.particles; HitColorConfig.save(); page(Page.HITCOLOR); });
                button("Sound: " + c.sound.name(), x + colW + 14, y + 78, colW, () -> { c.sound = HitColorConfig.HitSound.values()[(c.sound.ordinal()+1)%HitColorConfig.HitSound.values().length]; HitColorConfig.save(); page(Page.HITCOLOR); });
                button("Test hit effect", x, y + 112, panelW - 44, () -> HitEffects.playSound(c));
            }
            case HUD -> {
                ClientHubConfig c = ClientHubConfig.get();
                button("HUD overlay: " + onOff(c.hudEnabled), x, y, colW, () -> toggleHud("all"));
                button("FPS counter: " + onOff(c.showFps), x + colW + 14, y, colW, () -> toggleHud("fps"));
                button("CPS counter: " + onOff(c.showCps), x, y + 34, colW, () -> toggleHud("cps"));
                button("Ping counter: " + onOff(c.showPing), x + colW + 14, y + 34, colW, () -> toggleHud("ping"));
                button("TPS estimate: " + onOff(c.showTps), x, y + 68, colW, () -> toggleHud("tps"));
                button("Clock: " + onOff(c.showClock), x + colW + 14, y + 68, colW, () -> toggleHud("clock"));
                button("Playtime: " + onOff(c.showPlaytime), x, y + 102, colW, () -> toggleHud("play"));
            }
            case THEME -> {
                ClientHubConfig c = ClientHubConfig.get();
                slider("Hue", x, y, panelW - 44, c.themeHue, v -> c.themeHue = (float)v, ClientHubConfig::save);
                slider("Saturation", x, y + 42, panelW - 44, c.themeSaturation, v -> c.themeSaturation = (float)v, ClientHubConfig::save);
                slider("Brightness", x, y + 84, panelW - 44, c.themeBrightness, v -> c.themeBrightness = (float)v, ClientHubConfig::save);
                button("Preset: Ocean", x, y + 126, colW, () -> preset(.53f,.72f,.92f));
                button("Preset: Violet", x + colW + 14, y + 126, colW, () -> preset(.75f,.68f,.94f));
                button("Preset: Ember", x, y + 160, colW, () -> preset(.04f,.82f,.96f));
                button("Preset: Mint", x + colW + 14, y + 160, colW, () -> preset(.42f,.56f,.91f));
            }
            case CROSSHAIR -> {
                ClientHubConfig c = ClientHubConfig.get();
                button("Custom crosshair: " + onOff(c.customCrosshair), x, y, panelW - 44, () -> { c.customCrosshair = !c.customCrosshair; ClientHubConfig.save(); page(Page.CROSSHAIR); });
                button("Shape: " + c.crosshairShape, x, y + 36, colW, () -> { c.crosshairShape = switch(c.crosshairShape) { case "CROSS" -> "DOT"; case "DOT" -> "RING"; default -> "CROSS"; }; ClientHubConfig.save(); page(Page.CROSSHAIR); });
                button("Color: " + String.format("#%06X", c.crosshairColor), x + colW + 14, y + 36, colW, () -> { c.crosshairColor = (c.crosshairColor + 0x3355AA) & 0xFFFFFF; ClientHubConfig.save(); page(Page.CROSSHAIR); });
                slider("Size", x, y + 78, panelW - 44, (c.crosshairSize - 2f) / 22f, v -> c.crosshairSize = 2f + (float)v * 22f, ClientHubConfig::save);
                slider("Gap", x, y + 120, panelW - 44, c.crosshairGap / 16f, v -> c.crosshairGap = (float)v * 16f, ClientHubConfig::save);
                slider("Thickness", x, y + 162, panelW - 44, (c.crosshairThickness - 1f) / 7f, v -> c.crosshairThickness = 1f + (float)v * 7f, ClientHubConfig::save);
            }
            case MODULES -> {
                boolean itemSize = FabricLoader.getInstance().isModLoaded("itemsize");
                boolean auction = FabricLoader.getInstance().isModLoaded("hugoauction");
                boolean flashback = FabricLoader.getInstance().isModLoaded("flashback");
                boolean blur = FabricLoader.getInstance().isModLoaded("motionblur") || FabricLoader.getInstance().isModLoaded("blur");
                button("ItemSize  •  " + status(itemSize), x, y, colW, () -> openOptional("io.neoloni.itemsize.client.screen.ItemSizeScreen"));
                button("HugoSMP Auction  •  " + status(auction), x + colW + 14, y, colW, () -> openAuction());
                button("Flashback  •  " + status(flashback), x, y + 38, colW, () -> info("Flashback is a separate replay mod. Install it to use replay recording."));
                button("Motion Blur  •  " + status(blur), x + colW + 14, y + 38, colW, () -> info("Motion blur support is detected when a compatible mod is installed."));
                button("Back to overview", x, y + 90, panelW - 44, () -> page(Page.HOME));
            }
        }
    }

    private void page(Page target) { page = target; clearAndInit(); }
    private void preset(float h, float s, float b) { ClientHubConfig c=ClientHubConfig.get(); c.themeHue=h; c.themeSaturation=s; c.themeBrightness=b; ClientHubConfig.save(); page(Page.THEME); }
    private void toggleHud(String id) {
        ClientHubConfig c=ClientHubConfig.get();
        switch(id) { case "all" -> c.hudEnabled=!c.hudEnabled; case "fps" -> c.showFps=!c.showFps; case "cps" -> c.showCps=!c.showCps; case "ping" -> c.showPing=!c.showPing; case "tps" -> c.showTps=!c.showTps; case "clock" -> c.showClock=!c.showClock; case "play" -> c.showPlaytime=!c.showPlaytime; }
        ClientHubConfig.save(); page(Page.HUD);
    }
    private void button(String label, int x, int y, int w, Runnable action) {
        addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> action.run()).dimensions(x, y, w, 24).build());
    }
    private void slider(String label, int x, int y, int w, double value, java.util.function.DoubleConsumer setter, Runnable save) {
        addDrawableChild(new HubSlider(x, y, w, 24, label, value, setter, save));
    }
    private static String onOff(boolean b) { return b ? "ON" : "OFF"; }
    private static String status(boolean b) { return b ? "loaded" : "not installed"; }
    private void info(String message) { if (client != null && client.player != null) client.player.sendMessage(Text.literal("[HitColor] " + message), false); }
    private void openOptional(String className) {
        try {
            Class<?> type = Class.forName(className);
            Object screen = type.getConstructor().newInstance();
            if (screen instanceof Screen s && client != null) client.setScreen(s);
        } catch (Exception e) { info("Could not open optional module: " + e.getClass().getSimpleName()); }
    }
    private void openAuction() {
        if (!FabricLoader.getInstance().isModLoaded("hugoauction")) { info("HugoSMP Auction is not installed."); return; }
        try {
            Class<?> type = Class.forName("net.hugosmp.auction.HugoAuctionClient");
            var method = type.getDeclaredMethod("openMenu", MinecraftClient.class);
            method.setAccessible(true);
            method.invoke(null, client);
        } catch (Exception e) { info("Use the HugoSMP Auction keybind (J) to open its overlay."); }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context, mouseX, mouseY, delta);
        long elapsed = Util.getMeasuringTimeMs() - openedAt;
        float appear = Math.min(1f, elapsed / 220f);
        float pulse = .5f + .5f * (float)Math.sin(elapsed / 780.0);
        int accent = ClientHubConfig.accent();
        context.fill(0,0,width,height,0x99050A12);
        context.fill(panelX, panelY, panelX+panelW, panelY+panelH, BG);
        context.fill(panelX, panelY, panelX+panelW, panelY+2, 0xFF000000 | accent);
        context.fill(panelX+1,panelY+1,panelX+panelW-1,panelY+panelH-1,0x17000000);
        context.drawTextWithShadow(textRenderer, Text.literal("HITCOLOR  /  CLIENT HUB"), panelX+20, panelY+18, WHITE);
        context.drawTextWithShadow(textRenderer, Text.literal("RIGHT SHIFT  •  " + page.name()), panelX+panelW-178, panelY+20, MUTED);
        context.fill(panelX+18,panelY+49,panelX+panelW-18,panelY+50,0x553C4A60);
        context.fill(panelX+20,panelY+panelH-26,panelX+20+(int)((panelW-40)*appear),panelY+panelH-24,0xFF000000|accent);
        context.drawTextWithShadow(textRenderer, Text.literal("Smooth UI  •  local client settings  •  " + ClientHubHud.clock()), panelX+20, panelY+panelH-17, MUTED);
        if (page == Page.HOME) {
            int x=panelX+22, y=panelY+112, colW=(panelW-58)/2;
            drawCard(context,x,y+112,colW,68,"LIVE STATUS","FPS  "+(client==null?0:client.getCurrentFps())+"     CPS  "+ClientHubHud.leftCps()+"/"+ClientHubHud.rightCps());
            drawCard(context,x+colW+14,y+112,colW,68,"SESSION","PING  "+(ClientHubHud.ping()<0?"—":ClientHubHud.ping()+" ms")+"     PLAY  "+ClientHubHud.playtime());
            int c=0xFF000000|accent;
            context.fill(x+colW+14,y+192,x+colW+14+Math.round(48*pulse),y+194,c);
        } else if (page == Page.THEME) {
            int x=panelX+panelW-86, y=panelY+120;
            context.fill(x,y,x+54,y+54,0xFF000000|accent);
            context.fill(x+5,y+5,x+49,y+49,0xFF121925);
            context.drawTextWithShadow(textRenderer,Text.literal(String.format(Locale.ROOT,"#%06X",accent)),x-8,y+62,WHITE);
        } else if (page == Page.CROSSHAIR) {
            ClientHubConfig c=ClientHubConfig.get();
            int x=panelX+panelW-92, y=panelY+154, col=0xFF000000|c.crosshairColor;
            context.fill(x-22,y-22,x+22,y+22,0xFF0A0E16);
            if ("DOT".equals(c.crosshairShape)) context.fill(x-2,y-2,x+3,y+3,col);
            else if ("RING".equals(c.crosshairShape)) for(int a=0;a<360;a+=12){double r=Math.toRadians(a);int px=x+(int)(Math.cos(r)*12),py=y+(int)(Math.sin(r)*12);context.fill(px,py,px+2,py+2,col);}
            else {context.fill(x-1,y-13,x+2,y-4,col);context.fill(x-1,y+4,x+2,y+13,col);context.fill(x-13,y-1,x-4,y+2,col);context.fill(x+4,y-1,x+13,y+2,col);}
        }
        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCard(DrawContext c,int x,int y,int w,int h,String title,String value) {
        c.fill(x,y,x+w,y+h,PANEL);
        c.fill(x,y,x+2,y+h,0xFF000000|ClientHubConfig.accent());
        c.drawTextWithShadow(textRenderer,Text.literal(title),x+12,y+12,MUTED);
        c.drawTextWithShadow(textRenderer,Text.literal(value),x+12,y+34,WHITE);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) { close(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        ClientHubConfig.save();
        HitColorConfig.save();
        if (client != null) client.setScreen(parent);
    }

    private static final class HubSlider extends SliderWidget {
        private final String label;
        private final java.util.function.DoubleConsumer setter;
        private final Runnable save;
        HubSlider(int x,int y,int w,int h,String label,double value,java.util.function.DoubleConsumer setter,Runnable save) {
            super(x,y,w,h,Text.literal(label),Math.max(0,Math.min(1,value)));
            this.label=label; this.setter=setter; this.save=save; updateMessage();
        }
        @Override protected void updateMessage() { setMessage(Text.literal(label+"  "+Math.round(value*100)+"%")); }
        @Override protected void applyValue() { setter.accept(value); save.run(); }
    }
}
