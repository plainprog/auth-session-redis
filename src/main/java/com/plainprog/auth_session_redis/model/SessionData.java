package com.plainprog.auth_session_redis.model;

import java.time.Instant;

public class SessionData {
    // This object is for attaching any custom data to the session
    private Object data;

    private BasicUserInfoDTO user;
    private String sessionId;
    private Instant creationTime;
    private Instant lastAccessedTime;
    private Integer maxInactiveInterval;
    private Boolean exists;

    public SessionData() {
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }

    public Instant getLastAccessedTime() {
        return lastAccessedTime;
    }

    public void setLastAccessedTime(Instant lastAccessedTime) {
        this.lastAccessedTime = lastAccessedTime;
    }

    public Integer getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    public void setMaxInactiveInterval(Integer maxInactiveInterval) {
        this.maxInactiveInterval = maxInactiveInterval;
    }

    public BasicUserInfoDTO getUser() {
        return user;
    }

    public void setUser(BasicUserInfoDTO user) {
        this.user = user;
    }

    public Boolean getExists() {
        return exists;
    }

    public void setExists(Boolean exists) {
        this.exists = exists;
    }
}
