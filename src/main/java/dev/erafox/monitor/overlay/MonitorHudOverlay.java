package dev.erafox.monitor.overlay;

import net.fabricmc.fabric.api.client.rendering.v1.HudElementRegistry;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import dev.erafox.monitor.EraFoxMonitorMod;

public class MonitorHudOverlay {
    private static final Identifier OVERLAY_ID = Identifier.of("erafox-monitor", "status");
    
    public static void register() {
        HudElementRegistry.addLast(OVERLAY_ID, (DrawContext context, float tickDelta) -> {
            // Этот метод пустой - оверлей невидим для пользователя
            // Можно добавить отладочную информацию для администратора
            if (EraFoxMonitorMod.getBotEngine() != null && 
                EraFoxMonitorMod.getBotEngine().isRunning()) {
                // Опционально: можно рисовать статус мода в углу экрана
                // context.drawTextWithShadow(context.getTextRenderer(), 
                //     "EFM: ACTIVE", 5, 5, 0x00FF00);
            }
        });
        
        EraFoxMonitorMod.LOGGER.info("[EFM] HUD overlay зарегистрирован");
    }
}
