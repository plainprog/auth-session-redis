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
import redis.clients.jedis.Pipeline;

import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Service;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
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
        return processSessionHash(sessionId, sessionDataByte);
    }

    /**
     * Processes raw Redis hash data into SessionData object.
     *
     * @param sessionId the session ID
     * @param sessionDataByte the raw Redis hash data
     * @return SessionData object with populated fields
     */
    private SessionData processSessionHash(String sessionId, Map<byte[], byte[]> sessionDataByte) {
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

                        // Extract authorities from session's auth, not caller's
                        Authentication sessionAuth = ((SecurityContextImpl) context).getAuthentication();
                        if (sessionAuth != null) {
                            List<String> roles = sessionAuth.getAuthorities().stream()
                                    .map(GrantedAuthority::getAuthority)
                                    .toList();
                            basicUserInfoDTO.setAuthorities(roles);
                        }
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

        sessionData.setSessionId(sessionId);
        sessionData.setUser(basicUserInfoDTO);
        sessionData.setExists(true);
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

    /**
     * Retrieves session data for multiple sessions using Redis pipelining.
     * Returns SessionData for all requested IDs. Non-existent sessions will have
     * only sessionId populated, all other fields will be null.
     *
     * @param sessionIds list of session IDs to retrieve
     * @return list of SessionData objects in the same order as input
     */
    public List<SessionData> getSessionDataBatch(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return new ArrayList<>();
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // Use pipelining to fetch all sessions in a single network round trip
            Pipeline pipeline = jedis.pipelined();

            for (String sessionId : sessionIds) {
                String redisKey = "spring:session:sessions:" + sessionId;
                pipeline.hgetAll(redisKey.getBytes());
            }

            List<Object> results = pipeline.syncAndReturnAll();
            List<SessionData> sessionDataList = new ArrayList<>();

            for (int i = 0; i < sessionIds.size(); i++) {
                String sessionId = sessionIds.get(i);
                Map<byte[], byte[]> sessionDataByte = (Map<byte[], byte[]>) results.get(i);

                if (sessionDataByte == null || sessionDataByte.isEmpty()) {
                    // Session doesn't exist - return SessionData with only ID
                    SessionData emptySession = new SessionData();
                    emptySession.setExists(false);
                    emptySession.setSessionId(sessionId);
                    emptySession.setUser(new BasicUserInfoDTO());
                    sessionDataList.add(emptySession);
                } else {
                    // Process the session data
                    sessionDataList.add(processSessionHash(sessionId, sessionDataByte));
                }
            }

            return sessionDataList;

        } catch (Exception e) {
            System.err.println("Error retrieving batch session data: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to retrieve batch session data", e);
        }
    }

}
