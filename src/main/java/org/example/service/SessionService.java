package org.example.service;

import org.example.dto.SessionInfoResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SessionService {

    private static final int MAX_WINDOW_SIZE = 6;

    private final Map<String, SessionContext> sessions = new ConcurrentHashMap<>();

    public SessionSnapshot getOrCreateSession(String requestedSessionId) {
        String sessionId = normalizeSessionId(requestedSessionId);
        SessionContext context = sessions.computeIfAbsent(sessionId, SessionContext::new);
        return context.snapshot();
    }

    public void addExchange(String requestedSessionId, String userQuestion, String aiAnswer) {
        String sessionId = normalizeSessionId(requestedSessionId);
        SessionContext context = sessions.computeIfAbsent(sessionId, SessionContext::new);
        context.addMessage(userQuestion, aiAnswer);
    }

    public boolean clearSession(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        if (context == null) {
            return false;
        }
        context.clearHistory();
        return true;
    }

    public Optional<SessionInfoResponse> getSessionInfo(String sessionId) {
        SessionContext context = sessions.get(sessionId);
        if (context == null) {
            return Optional.empty();
        }

        SessionSnapshot snapshot = context.snapshot();
        SessionInfoResponse response = new SessionInfoResponse();
        response.setSessionId(snapshot.sessionId());
        response.setMessagePairCount(snapshot.messagePairCount());
        response.setCreateTime(snapshot.createTime());
        return Optional.of(response);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return sessionId;
    }

    public record SessionSnapshot(
            String sessionId,
            List<Map<String, String>> history,
            int messagePairCount,
            long createTime
    ) {
    }

    private static class SessionContext {
        private final String sessionId;
        private final List<Map<String, String>> messageHistory = new ArrayList<>();
        private final long createTime = System.currentTimeMillis();
        private final ReentrantLock lock = new ReentrantLock();

        private SessionContext(String sessionId) {
            this.sessionId = sessionId;
        }

        private void addMessage(String userQuestion, String aiAnswer) {
            lock.lock();
            try {
                Map<String, String> userMsg = new HashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", userQuestion);
                messageHistory.add(userMsg);

                Map<String, String> assistantMsg = new HashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", aiAnswer);
                messageHistory.add(assistantMsg);

                int maxMessages = MAX_WINDOW_SIZE * 2;
                while (messageHistory.size() > maxMessages) {
                    messageHistory.remove(0);
                    if (!messageHistory.isEmpty()) {
                        messageHistory.remove(0);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        private void clearHistory() {
            lock.lock();
            try {
                messageHistory.clear();
            } finally {
                lock.unlock();
            }
        }

        private SessionSnapshot snapshot() {
            lock.lock();
            try {
                return new SessionSnapshot(
                        sessionId,
                        new ArrayList<>(messageHistory),
                        messageHistory.size() / 2,
                        createTime
                );
            } finally {
                lock.unlock();
            }
        }
    }
}
