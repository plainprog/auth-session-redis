package com.plainprog.auth_session_redis.controllers;

import com.plainprog.auth_session_redis.model.BatchSessionRequest;
import com.plainprog.auth_session_redis.model.BatchSessionResponse;
import com.plainprog.auth_session_redis.model.SessionData;
import com.plainprog.auth_session_redis.service.SessionExplorerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/session")
public class SessionController {
    @Autowired
    private SessionExplorerService sessionExplorerService;

    @PostMapping("/validate")
    public ResponseEntity<SessionData> validateSession(HttpSession session) {
        //this endpoint protected by spring boot authentication
        //if we are here, then session cookie is valid
        SessionData data = sessionExplorerService.getSessionData(session.getId());
        return ResponseEntity.ok(data);
    }

    @PostMapping("/initiate")
    public String initiateSession(@RequestBody Object data, @RequestHeader String user, @RequestHeader String authorities, HttpSession session) {
        List<String> authoritiesList = List.of(authorities.split(","));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                authoritiesList.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
        session.setAttribute("data", data);
        return "Session initiated";
    }

    /**
     * Terminates a session by ID. Unprotected endpoint for backend service use.
     * This endpoint is stateless and does not access the caller's session.
     *
     * @param sessionId the session ID to terminate
     * @return 204 if deleted, 404 if not found, 500 on error
     */
    @DeleteMapping("/terminate/{sessionId}")
    public ResponseEntity<?> terminateSession(@PathVariable String sessionId) {
        try {
            boolean deleted = sessionExplorerService.deleteSession(sessionId);

            if (deleted) {
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Session not found: " + sessionId);
            }

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error terminating session: " + e.getMessage());
        }
    }

    /**
     * Validates multiple sessions in a batch. Unprotected endpoint for backend service use.
     * Returns session data for all requested IDs. Non-existent sessions will have only
     * sessionId populated with other fields null.
     *
     * @param request batch session request containing list of session IDs
     * @return BatchSessionResponse containing list of SessionData objects
     */
    @PostMapping("/validate-batch")
    public ResponseEntity<BatchSessionResponse> validateSessionsBatch(@RequestBody BatchSessionRequest request) {
        try {
            if (request == null || request.getSessionIds() == null || request.getSessionIds().isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<SessionData> results = sessionExplorerService.getSessionDataBatch(request.getSessionIds());
            BatchSessionResponse response = new BatchSessionResponse(results);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
