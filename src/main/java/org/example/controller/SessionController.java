package org.example.controller;

import org.example.dto.ClearSessionRequest;
import org.example.dto.SessionInfoResponse;
import org.example.exception.ApiException;
import org.example.response.ApiResponse;
import org.example.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/chat")
public class SessionController {

    private static final Logger logger = LoggerFactory.getLogger(SessionController.class);

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearChatHistory(@RequestBody ClearSessionRequest request) {
        logger.info("收到清空会话历史请求 - SessionId: {}", request.getId());

        if (request.getId() == null || request.getId().isEmpty()) {
            throw ApiException.badRequest("会话ID不能为空");
        }

        if (sessionService.clearSession(request.getId())) {
            return ResponseEntity.ok(ApiResponse.success("会话历史已清空"));
        }

        throw ApiException.notFound("会话不存在");
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> getSessionInfo(@PathVariable String sessionId) {
        Optional<SessionInfoResponse> response = sessionService.getSessionInfo(sessionId);
        if (response.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(response.get()));
        }
        throw ApiException.notFound("会话不存在");
    }
}
