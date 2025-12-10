package dev.erafox.monitor.executor.winapi;

import com.sun.jna.*;
import com.sun.jna.platform.win32.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WindowsProcessFreezer {
    private static final int PROCESS_QUERY_INFORMATION = 0x0400;
    private static final int TH32CS_SNAPTHREAD = 0x00000004;
    private static final int THREAD_SUSPEND_RESUME = 0x0002;
    private static final int THREAD_QUERY_INFORMATION = 0x0040;
    
    private static final Kernel32 KERNEL32 = Kernel32.INSTANCE;
    private static final Map<Integer, List<WinNT.HANDLE>> suspendedThreads = new ConcurrentHashMap<>();
    
    public static class THREADENTRY32 extends Structure {
        public int dwSize;
        public int cntUsage;
        public int th32ThreadID;
        public int th32OwnerProcessID;
        public int tpBasePri;
        public int tpDeltaPri;
        public int dwFlags;
        
        protected List<String> getFieldOrder() {
            return Arrays.asList("dwSize", "cntUsage", "th32ThreadID", 
                               "th32OwnerProcessID", "tpBasePri", "tpDeltaPri", "dwFlags");
        }
    }
    
    public interface Toolhelp32 extends Library {
        Toolhelp32 INSTANCE = Native.load("kernel32", Toolhelp32.class);
        
        WinNT.HANDLE CreateToolhelp32Snapshot(int dwFlags, int th32ProcessID);
        boolean Thread32First(WinNT.HANDLE hSnapshot, THREADENTRY32 lpte);
        boolean Thread32Next(WinNT.HANDLE hSnapshot, THREADENTRY32 lpte);
    }
    
    public static boolean suspendProcess(int pid) {
        if (suspendedThreads.containsKey(pid)) {
            return false;
        }
        
        List<WinNT.HANDLE> handles = new ArrayList<>();
        WinNT.HANDLE hSnapshot = Toolhelp32.INSTANCE.CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0);
        
        if (hSnapshot == null || WinBase.INVALID_HANDLE_VALUE.equals(hSnapshot)) {
            return false;
        }
        
        try {
            THREADENTRY32 te = new THREADENTRY32();
            te.dwSize = Native.getNativeSize(THREADENTRY32.class, null);
            
            if (Toolhelp32.INSTANCE.Thread32First(hSnapshot, te)) {
                do {
                    if (te.th32OwnerProcessID == pid) {
                        WinNT.HANDLE hThread = KERNEL32.OpenThread(
                            THREAD_SUSPEND_RESUME | THREAD_QUERY_INFORMATION,
                            false,
                            te.th32ThreadID
                        );
                        
                        if (hThread != null && !WinBase.INVALID_HANDLE_VALUE.equals(hThread)) {
                            int suspendCount = KERNEL32.SuspendThread(hThread);
                            if (suspendCount != -1) {
                                handles.add(hThread);
                            } else {
                                KERNEL32.CloseHandle(hThread);
                            }
                        }
                    }
                } while (Toolhelp32.INSTANCE.Thread32Next(hSnapshot, te));
            }
        } finally {
            KERNEL32.CloseHandle(hSnapshot);
        }
        
        if (!handles.isEmpty()) {
            suspendedThreads.put(pid, handles);
            return true;
        }
        return false;
    }
    
    public static boolean resumeProcess(int pid) {
        List<WinNT.HANDLE> handles = suspendedThreads.remove(pid);
        if (handles == null) {
            return false;
        }
        
        boolean success = false;
        for (WinNT.HANDLE hThread : handles) {
            if (KERNEL32.ResumeThread(hThread) != -1) {
                success = true;
            }
            KERNEL32.CloseHandle(hThread);
        }
        return success;
    }
    
    public static boolean terminateProcess(int pid) {
        WinNT.HANDLE hProcess = KERNEL32.OpenProcess(
            Kernel32.PROCESS_TERMINATE,
            false,
            pid
        );
        
        if (hProcess == null || WinBase.INVALID_HANDLE_VALUE.equals(hProcess)) {
            return false;
        }
        
        try {
            boolean result = KERNEL32.TerminateProcess(hProcess, 1);
            List<WinNT.HANDLE> handles = suspendedThreads.remove(pid);
            if (handles != null) {
                for (WinNT.HANDLE hThread : handles) {
                    KERNEL32.CloseHandle(hThread);
                }
            }
            return result;
        } finally {
            KERNEL32.CloseHandle(hProcess);
        }
    }
    
    public static List<Integer> getSuspendedProcesses() {
        return new ArrayList<>(suspendedThreads.keySet());
    }
    
    public static boolean isProcessSuspended(int pid) {
        return suspendedThreads.containsKey(pid);
    }
}
