package dev.erafox.monitor.overlay;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import dev.erafox.monitor.EraFoxMonitorMod;

public class MonitorHudOverlay implements HudRenderCallback {

    private static final MonitorHudOverlay INSTANCE = new MonitorHudOverlay();
    private boolean isActive = false;

    public static void register() {
        // Регистрируем этот класс как обработчик события отрисовки HUD
        HudRenderCallback.EVENT.register(INSTANCE);
        EraFoxMonitorMod.LOGGER.info("[EFM] HUD overlay зарегистрирован через HudRenderCallback");
    }

    @Override
    public void onHudRender(DrawContext drawContext, float tickDelta) {
        // Этот метод будет вызываться каждый кадр при отрисовке интерфейса.
        // Оставляем его пустым для невидимости, но можно добавить отладочную информацию.

        // Пример: отобразить небольшой статус в углу, если мод активен
        // if (EraFoxMonitorMod.getBotEngine() != null && EraFoxMonitorMod.getBotEngine().isRunning()) {
        //     drawContext.drawTextWithShadow(MinecraftClient.getInstance().textRenderer,
        //             "[EFM]", 5, 5, 0x00FF00);
        // }
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }
}
