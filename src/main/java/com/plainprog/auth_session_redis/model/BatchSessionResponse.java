package com.plainprog.auth_session_redis.model;

import java.util.List;

public class BatchSessionResponse {
    private List<SessionData> sessions;

    public BatchSessionResponse() {
    }

    public BatchSessionResponse(List<SessionData> sessions) {
        this.sessions = sessions;
    }

    public List<SessionData> getSessions() {
        return sessions;
    }

    public void setSessions(List<SessionData> sessions) {
        this.sessions = sessions;
    }
}
