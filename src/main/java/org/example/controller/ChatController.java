package org.example.controller;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.streaming.OutputType;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.example.dto.ChatRequest;
import org.example.dto.ChatResponse;
import org.example.dto.SseMessage;
import org.example.exception.ApiException;
import org.example.response.ApiResponse;
import org.example.service.ChatService;
import org.example.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final SessionService sessionService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public ChatController(ChatService chatService, SessionService sessionService) {
        this.chatService = chatService;
        this.sessionService = sessionService;
    }

    @PostMapping("/chat")
    public ResponseEntity<ApiResponse<ChatResponse>> chat(@RequestBody ChatRequest request) throws Exception {
        logger.info("收到对话请求 - SessionId: {}, Question: {}", request.getId(), request.getQuestion());

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw ApiException.badRequest("问题内容不能为空");
        }

        SessionService.SessionSnapshot session = sessionService.getOrCreateSession(request.getId());
        DashScopeApi dashScopeApi = chatService.createDashScopeApi();
        DashScopeChatModel chatModel = chatService.createStandardChatModel(dashScopeApi);

        chatService.logAvailableTools();
        ReactAgent agent = chatService.createReactAgent(chatModel, chatService.buildSystemPrompt(session.history()));
        String fullAnswer = chatService.executeChat(agent, request.getQuestion());

        sessionService.addExchange(session.sessionId(), request.getQuestion(), fullAnswer);
        return ResponseEntity.ok(ApiResponse.success(ChatResponse.success(fullAnswer)));
    }

    @PostMapping(value = "/chat_stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300000L);

        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            try {
                emitter.send(SseEmitter.event().name("message").data(SseMessage.error("问题内容不能为空"), MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        executor.execute(() -> {
            try {
                SessionService.SessionSnapshot session = sessionService.getOrCreateSession(request.getId());
                DashScopeApi dashScopeApi = chatService.createDashScopeApi();
                DashScopeChatModel chatModel = chatService.createStandardChatModel(dashScopeApi);

                chatService.logAvailableTools();
                ReactAgent agent = chatService.createReactAgent(chatModel, chatService.buildSystemPrompt(session.history()));

                StringBuilder fullAnswerBuilder = new StringBuilder();
                Flux<NodeOutput> stream = agent.stream(request.getQuestion());

                stream.subscribe(
                        output -> handleStreamOutput(output, emitter, fullAnswerBuilder),
                        error -> handleStreamError(error, emitter),
                        () -> handleStreamComplete(emitter, session.sessionId(), request.getQuestion(), fullAnswerBuilder.toString())
                );
            } catch (Exception e) {
                logger.error("ReactAgent 对话初始化失败", e);
                try {
                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(SseMessage.error(e.getMessage()), MediaType.APPLICATION_JSON));
                } catch (IOException ex) {
                    logger.error("发送错误消息失败", ex);
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    private void handleStreamOutput(NodeOutput output, SseEmitter emitter, StringBuilder fullAnswerBuilder) {
        try {
            if (output instanceof StreamingOutput streamingOutput) {
                OutputType type = streamingOutput.getOutputType();
                if (type == OutputType.AGENT_MODEL_STREAMING) {
                    String chunk = streamingOutput.message().getText();
                    if (chunk != null && !chunk.isEmpty()) {
                        fullAnswerBuilder.append(chunk);
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(SseMessage.content(chunk), MediaType.APPLICATION_JSON));
                    }
                } else if (type == OutputType.AGENT_TOOL_FINISHED) {
                    logger.info("工具调用完成: {}", output.node());
                } else if (type == OutputType.AGENT_HOOK_FINISHED) {
                    logger.debug("Hook 执行完成: {}", output.node());
                }
            }
        } catch (IOException e) {
            logger.error("发送流式消息失败", e);
            throw new RuntimeException(e);
        }
    }

    private void handleStreamError(Throwable error, SseEmitter emitter) {
        logger.error("ReactAgent 流式对话失败", error);
        try {
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(SseMessage.error(error.getMessage()), MediaType.APPLICATION_JSON));
        } catch (IOException ex) {
            logger.error("发送错误消息失败", ex);
        }
        emitter.completeWithError(error);
    }

    private void handleStreamComplete(SseEmitter emitter, String sessionId, String question, String fullAnswer) {
        try {
            sessionService.addExchange(sessionId, question, fullAnswer);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(SseMessage.done(), MediaType.APPLICATION_JSON));
            emitter.complete();
        } catch (IOException e) {
            logger.error("发送完成消息失败", e);
            emitter.completeWithError(e);
        }
    }
}
