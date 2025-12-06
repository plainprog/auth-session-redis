package com.plainprog.auth_session_redis.model;

import java.util.List;

public class BatchSessionRequest {
    private List<String> sessionIds;

    public BatchSessionRequest() {
    }

    public BatchSessionRequest(List<String> sessionIds) {
        this.sessionIds = sessionIds;
    }

    public List<String> getSessionIds() {
        return sessionIds;
    }

    public void setSessionIds(List<String> sessionIds) {
        this.sessionIds = sessionIds;
    }
}
