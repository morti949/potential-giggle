package dev.erafox.monitor.profile;

import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;
import java.net.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class SystemScanner {
    private final SystemInfo systemInfo = new SystemInfo();
    private final HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private final OperatingSystem os = systemInfo.getOperatingSystem();
    private String cachedPublicIp = null;

    public String getCpuInfo() {
        CentralProcessor cpu = hardware.getProcessor();
        return cpu.getProcessorIdentifier().getName().trim();
    }

    public String getGpuInfo() {
        List<GraphicsCard> gpus = hardware.getGraphicsCards();
        if (gpus.isEmpty()) return "Не обнаружена";
        StringBuilder gpuInfo = new StringBuilder();
        for (GraphicsCard gpu : gpus) {
            gpuInfo.append(gpu.getName()).append(" (").append(gpu.getVendor()).append(")");
            if (gpu.getDeviceId() != null) {
                gpuInfo.append(" [").append(gpu.getDeviceId()).append("]");
            }
            gpuInfo.append("; ");
        }
        return gpuInfo.toString();
    }

    public String getRamInfo() {
        GlobalMemory memory = hardware.getMemory();
        long totalGB = memory.getTotal() / (1024 * 1024 * 1024);
        long availableGB = memory.getAvailable() / (1024 * 1024 * 1024);
        return totalGB + " GB (доступно: " + availableGB + " GB)";
    }

    public String getPublicIp() {
        if (cachedPublicIp != null) {
            return cachedPublicIp;
        }
        
        String[] services = {
            "https://api.ipify.org",
            "https://icanhazip.com",
            "https://checkip.amazonaws.com"
        };
        
        for (String service : services) {
            try {
                URL url = new URL(service);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String ip = reader.readLine().trim();
                reader.close();
                
                if (ip != null && !ip.isEmpty()) {
                    cachedPublicIp = ip;
                    return ip;
                }
            } catch (Exception e) {
                continue;
            }
        }
        return "Не определен";
    }

    public String getOsInfo() {
        return os.toString();
    }

    public String getMacAddress() {
        try {
            NetworkInterface network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            byte[] mac = network.getHardwareAddress();
            if (mac != null) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return "Не определен";
        }
        return "Не определен";
    }
}
