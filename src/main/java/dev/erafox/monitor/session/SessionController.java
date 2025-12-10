package dev.erafox.monitor.session;

import java.util.concurrent.ConcurrentHashMap;

public class SessionController {
    private static SessionController instance;
    private final ConcurrentHashMap<Long, UserSession> sessions = new ConcurrentHashMap<>();

    private SessionController() {}

    public static synchronized SessionController getInstance() {
        if (instance == null) {
            instance = new SessionController();
        }
        return instance;
    }

    public UserSession getOrCreateSession(long userId, long chatId) {
        return sessions.computeIfAbsent(userId, k -> new UserSession(userId, chatId));
    }

    public UserSession getSession(long userId) {
        return sessions.get(userId);
    }

    public void removeSession(long userId) {
        sessions.remove(userId);
    }

    public void cleanupInactiveSessions() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
}
