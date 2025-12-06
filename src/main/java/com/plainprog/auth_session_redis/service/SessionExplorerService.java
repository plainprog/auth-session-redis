package com.plainprog.auth_session_redis.service;

import com.plainprog.auth_session_redis.model.BasicUserInfoDTO;
import com.plainprog.auth_session_redis.model.SessionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class SessionExplorerService {
    @Autowired
    RedisSerializer serializer;

    @Autowired
    JedisPool jedisPool;

    @Autowired
    SessionRepository<? extends Session> sessionRepository;

    private Map<byte[], byte[]> getRedisKey(String redisKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.hgetAll(redisKey.getBytes());
        }
    }

    public SessionData getSessionData(String sessionId) {
        String redisKey = "spring:session:sessions:" + sessionId;
        Map<byte[], byte[]> sessionDataByte = getRedisKey(redisKey);
        BasicUserInfoDTO basicUserInfoDTO = new BasicUserInfoDTO();
        SessionData sessionData = new SessionData();
        for (Map.Entry<byte[], byte[]> entry : sessionDataByte.entrySet()) {
            String field = new String(entry.getKey());
            byte[] value = entry.getValue();
            try {
                if (field.equals("sessionAttr:data")) {
                    Object data = serializer.deserialize(value);
                    sessionData.setData(data);
                } else if (field.equals("creationTime")) {
                    Object creationTime = serializer.deserialize(value);
                    if (creationTime instanceof Long) {
                        long creationTimeLong = (long) creationTime;
                        Instant instant = Instant.ofEpochMilli(creationTimeLong);
                        sessionData.setCreationTime(instant);
                    }
                } else if (field.equals("lastAccessedTime")) {
                    Object lastAccessedTime = serializer.deserialize(value);
                    if (lastAccessedTime instanceof Long) {
                        long lastAccessedTimeLong = (long) lastAccessedTime;
                        Instant instant = Instant.ofEpochMilli(lastAccessedTimeLong);
                        sessionData.setLastAccessedTime(instant);
                    }
                } else if (field.equals("sessionAttr:SPRING_SECURITY_CONTEXT")) {
                    Object context = serializer.deserialize(value);
                    if (context instanceof SecurityContextImpl) {
                        Principal principal = ((SecurityContextImpl) context).getAuthentication();
                        basicUserInfoDTO.setUsername(principal.getName());
                    }
                } else if (field.equals("maxInactiveInterval")) {
                    Object maxInactiveInterval = serializer.deserialize(value);
                    if (maxInactiveInterval instanceof Integer) {
                        sessionData.setMaxInactiveInterval((int) maxInactiveInterval);
                    }
                }
            } catch (Exception e) {
                System.out.println(field + " : " + "Could not deserialize");
                e.printStackTrace();
            }
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        basicUserInfoDTO.setAuthorities(roles);
        sessionData.setSessionId(sessionId);
        sessionData.setUser(basicUserInfoDTO);
        return sessionData;
    }

    /**
     * Deletes a session by its ID, removing it from Spring Session and Redis.
     *
     * @param sessionId the session ID to delete
     * @return true if session existed and was deleted, false if not found
     */
    public boolean deleteSession(String sessionId) {
        try {
            Session session = sessionRepository.findById(sessionId);

            if (session == null) {
                return false;
            }

            sessionRepository.deleteById(sessionId);
            return true;

        } catch (Exception e) {
            System.err.println("Error deleting session " + sessionId + ": " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to delete session: " + sessionId, e);
        }
    }

}
