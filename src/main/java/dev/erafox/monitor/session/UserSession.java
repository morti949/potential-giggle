package dev.erafox.monitor.session;

public class UserSession {
    private final long userId;
    private final long chatId;
    private final long creationTime;
    private String targetUser;
    private String currentPath;
    private Integer selectedPid;
    private boolean awaitingCommand;
    private boolean awaitingFile;
    private int lastMessageId;
    private String selectedFile;

    public UserSession(long userId, long chatId) {
        this.userId = userId;
        this.chatId = chatId;
        this.creationTime = System.currentTimeMillis();
        this.currentPath = "";
        this.awaitingCommand = false;
        this.awaitingFile = false;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - creationTime > 3600000;
    }

    public long getUserId() { return userId; }
    public long getChatId() { return chatId; }
    public String getTargetUser() { return targetUser; }
    public void setTargetUser(String targetUser) { this.targetUser = targetUser; }
    public String getCurrentPath() { return currentPath; }
    public void setCurrentPath(String currentPath) { this.currentPath = currentPath; }
    public Integer getSelectedPid() { return selectedPid; }
    public void setSelectedPid(Integer selectedPid) { this.selectedPid = selectedPid; }
    public boolean isAwaitingCommand() { return awaitingCommand; }
    public void setAwaitingCommand(boolean awaitingCommand) { this.awaitingCommand = awaitingCommand; }
    public boolean isAwaitingFile() { return awaitingFile; }
    public void setAwaitingFile(boolean awaitingFile) { this.awaitingFile = awaitingFile; }
    public int getLastMessageId() { return lastMessageId; }
    public void setLastMessageId(int lastMessageId) { this.lastMessageId = lastMessageId; }
    public String getSelectedFile() { return selectedFile; }
    public void setSelectedFile(String selectedFile) { this.selectedFile = selectedFile; }
}
