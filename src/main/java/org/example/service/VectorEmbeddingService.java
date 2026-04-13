package org.example.service;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingOutput;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 向量嵌入服务
 * 使用阿里云 DashScope Text Embedding API
 */
@Service
public class VectorEmbeddingService {

    private static final Logger logger = LoggerFactory.getLogger(VectorEmbeddingService.class);

    @Value("${dashscope.api.key}")
    private String apiKey;

    @Value("${dashscope.embedding.model}")
    private String model;

    @Value("${dashscope.embedding.max-attempts:3}")
    private int maxAttempts;

    @Value("${dashscope.embedding.retry-interval-ms:2000}")
    private long retryIntervalMs;

    @Value("${dashscope.embedding.max-retry-interval-ms:10000}")
    private long maxRetryIntervalMs;

    @Value("${dashscope.embedding.retry-multiplier:2.0}")
    private double retryMultiplier;

    private TextEmbedding textEmbedding;

    @PostConstruct
    public void init() {
        // 验证 API Key
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("your-api-key-here")) {
            logger.error("API Key 未正确配置！当前值: {}", apiKey);
            throw new IllegalStateException("请设置环境变量 DASHSCOPE_API_KEY 或在 application.yml 中配置正确的 API Key");
        }
        
        // 打印 API Key 前缀用于调试（不打印完整 Key 保证安全）
        String maskedKey = apiKey.length() > 8 ? 
            apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4) : 
            "***";
        logger.info("API Key 已加载: {}", maskedKey);
        
        // 设置全局 API Key（确保设置成功）
        Constants.apiKey = apiKey;
        
        // 验证 API Key 是否设置成功
        if (Constants.apiKey == null || Constants.apiKey.isEmpty()) {
            logger.error("Constants.apiKey 设置失败！");
            throw new IllegalStateException("API Key 设置到 Constants 失败");
        }
        
        logger.info("Constants.apiKey 已设置: {}", Constants.apiKey.substring(0, Math.min(8, Constants.apiKey.length())) + "...");
        
        // 创建 TextEmbedding 实例
        textEmbedding = new TextEmbedding();
        
        logger.info("阿里云 DashScope Embedding 服务初始化完成，模型: {}, 最大重试次数: {}", model, maxAttempts);
    }

    /**
     * 生成向量嵌入
     * 调用阿里云 DashScope Text Embedding API
     * 
     * @param content 文本内容
     * @return 向量嵌入（浮点数列表）
     */
    public List<Float> generateEmbedding(String content) {
        try {
            if (content == null || content.trim().isEmpty()) {
                logger.warn("内容为空，无法生成向量");
                throw new IllegalArgumentException("内容不能为空");
            }

            logger.debug("开始生成向量嵌入, 内容长度: {} 字符", content.length());
            
            // 确保 API Key 已设置（防止被其他地方覆盖）
            if (Constants.apiKey == null || Constants.apiKey.isEmpty()) {
                logger.warn("检测到 Constants.apiKey 为空，重新设置");
                Constants.apiKey = apiKey;
            }
            
            logger.debug("调用 API 前 Constants.apiKey: {}", 
                Constants.apiKey != null ? Constants.apiKey.substring(0, Math.min(8, Constants.apiKey.length())) + "..." : "null");

            // 构建请求参数
            TextEmbeddingParam param = TextEmbeddingParam
                    .builder()
                    .model(model)
                    .texts(Collections.singletonList(content))
                    .build();

            TextEmbeddingResult result = executeWithRetry(
                    "单文本向量生成",
                    String.format("内容长度=%d", content.length()),
                    () -> textEmbedding.call(param)
            );

            // 检查结果
            List<Float> floatEmbedding = getFloats(result);

            logger.info("成功生成向量嵌入, 内容长度: {} 字符, 向量维度: {}", 
                content.length(), floatEmbedding.size());

            return floatEmbedding;

        } catch (NoApiKeyException e) {
            logger.error("API Key 未设置或无效", e);
            throw new RuntimeException("API Key 未设置，请配置 dashscope.api.key", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            logger.error("生成向量嵌入失败, 内容长度: {}", content != null ? content.length() : 0, e);
            throw buildEmbeddingException("单文本向量生成", content != null ? "内容长度=" + content.length() : "内容为空", e);
        }
    }

    @NotNull
    private static List<Float> getFloats(TextEmbeddingResult result) {
        if (result == null || result.getOutput() == null || result.getOutput().getEmbeddings() == null) {
            throw new RuntimeException("DashScope API 返回空结果");
        }

        TextEmbeddingOutput output = result.getOutput();
        List<TextEmbeddingResultItem> embeddings = output.getEmbeddings();

        if (embeddings.isEmpty()) {
            throw new RuntimeException("DashScope API 返回空向量列表");
        }

        // 获取第一个文本的向量
        List<Double> embeddingDoubles = embeddings.get(0).getEmbedding();

        // 转换为 List<Float>
        List<Float> floatEmbedding = new ArrayList<>(embeddingDoubles.size());
        for (Double value : embeddingDoubles) {
            floatEmbedding.add(value.floatValue());
        }
        return floatEmbedding;
    }

    /**
     * 批量生成向量嵌入
     * 
     * @param contents 文本内容列表
     * @return 向量嵌入列表
     */
    public List<List<Float>> generateEmbeddings(List<String> contents) {
        try {
            if (contents == null || contents.isEmpty()) {
                logger.warn("内容列表为空，无法生成向量");
                return Collections.emptyList();
            }

            logger.info("开始批量生成向量嵌入, 数量: {}", contents.size());
            
            // 确保 API Key 已设置
            if (Constants.apiKey == null || Constants.apiKey.isEmpty()) {
                logger.warn("检测到 Constants.apiKey 为空，重新设置");
                Constants.apiKey = apiKey;
            }

            // 构建请求参数 - 批量输入
            TextEmbeddingParam param = TextEmbeddingParam
                    .builder()
                    .model(model)
                    .texts(contents)
                    .build();

            TextEmbeddingResult result = executeWithRetry(
                    "批量向量生成",
                    String.format("批量数量=%d", contents.size()),
                    () -> textEmbedding.call(param)
            );

            // 检查结果
            if (result == null || result.getOutput() == null || result.getOutput().getEmbeddings() == null) {
                throw new RuntimeException("批量 DashScope API 返回空结果");
            }

            List<TextEmbeddingResultItem> embeddingItems = result.getOutput().getEmbeddings();
            
            if (embeddingItems.isEmpty()) {
                throw new RuntimeException("批量 DashScope API 返回空向量列表");
            }

            // 转换结果
            List<List<Float>> embeddings = new ArrayList<>();
            for (TextEmbeddingResultItem item : embeddingItems) {
                List<Double> embeddingDoubles = item.getEmbedding();
                List<Float> embedding = new ArrayList<>(embeddingDoubles.size());
                for (Double value : embeddingDoubles) {
                    embedding.add(value.floatValue());
                }
                embeddings.add(embedding);
            }

            logger.info("成功批量生成向量嵌入, 数量: {}, 维度: {}", 
                embeddings.size(), 
                embeddings.isEmpty() ? 0 : embeddings.get(0).size());

            return embeddings;

        } catch (NoApiKeyException e) {
            logger.error("批量调用时 API Key 未设置或无效", e);
            throw new RuntimeException("API Key 未设置，请配置 dashscope.api.key", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            logger.error("批量生成向量嵌入失败", e);
            throw buildEmbeddingException("批量向量生成", contents != null ? "批量数量=" + contents.size() : "内容列表为空", e);
        }
    }

    /**
     * 生成查询向量
     * 
     * @param query 查询文本
     * @return 向量嵌入
     */
    public List<Float> generateQueryVector(String query) {
        return generateEmbedding(query);
    }

    /**
     * 计算两个向量的余弦相似度
     * 
     * @param vector1 向量1
     * @param vector2 向量2
     * @return 余弦相似度 [-1, 1]
     */
    public float calculateCosineSimilarity(List<Float> vector1, List<Float> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("向量维度不匹配");
        }

        float dotProduct = 0.0f;
        float norm1 = 0.0f;
        float norm2 = 0.0f;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += vector1.get(i) * vector1.get(i);
            norm2 += vector2.get(i) * vector2.get(i);
        }

        return dotProduct / (float) (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private TextEmbeddingResult executeWithRetry(String operation, String context, EmbeddingCall call) throws Exception {
        Exception lastException = null;
        long currentDelayMs = retryIntervalMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.call();
            } catch (NoApiKeyException e) {
                throw e;
            } catch (Exception e) {
                lastException = e;
                boolean retryable = isRetryableException(e);
                String conciseMessage = extractConciseMessage(e);

                if (!retryable || attempt >= maxAttempts) {
                    logger.error("{}失败，{}，第 {}/{} 次尝试，错误: {}",
                            operation, context, attempt, maxAttempts, conciseMessage, e);
                    break;
                }

                logger.warn("{}失败，{}，第 {}/{} 次尝试，准备在 {} ms 后重试。错误: {}",
                        operation, context, attempt, maxAttempts, currentDelayMs, conciseMessage);

                sleepBeforeRetry(currentDelayMs);
                currentDelayMs = nextDelay(currentDelayMs);
            }
        }

        if (lastException == null) {
            throw new IllegalStateException(operation + "失败，但未捕获到底层异常");
        }

        throw buildEmbeddingException(operation, context, lastException);
    }

    private RuntimeException buildEmbeddingException(String operation, String context, Exception exception) {
        String conciseMessage = extractConciseMessage(exception);
        String userMessage;

        if (isRetryableException(exception)) {
            userMessage = String.format(
                    "%s失败，已重试 %d 次仍未成功。当前更像是 DashScope 网络超时或服务暂时不可用，请稍后重试。上下文: %s。原始错误: %s",
                    operation,
                    maxAttempts,
                    context,
                    conciseMessage
            );
        } else {
            userMessage = String.format(
                    "%s失败。请检查 DashScope 配置或请求内容。上下文: %s。原始错误: %s",
                    operation,
                    context,
                    conciseMessage
            );
        }

        return new RuntimeException(userMessage, exception);
    }

    private boolean isRetryableException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            String message = current.getMessage();
            if (className.contains("sockettimeoutexception")
                    || className.contains("connectexception")
                    || className.contains("timeoutexception")) {
                return true;
            }
            if (message != null) {
                String normalized = message.toLowerCase(Locale.ROOT);
                if (normalized.contains("timed out")
                        || normalized.contains("timeout")
                        || normalized.contains("deadline exceeded")
                        || normalized.contains("network error")
                        || normalized.contains("connectexception")
                        || normalized.contains("connection reset")
                        || normalized.contains("temporarily unavailable")
                        || normalized.contains("503")
                        || normalized.contains("429")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private String extractConciseMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null) {
            root = root.getCause();
        }

        String message = root.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.getMessage();
        }
        if (message == null || message.isBlank()) {
            message = throwable.getClass().getSimpleName();
        }
        return message;
    }

    private void sleepBeforeRetry(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等待 DashScope 重试时被中断", interruptedException);
        }
    }

    private long nextDelay(long currentDelayMs) {
        long nextDelayMs = (long) Math.ceil(currentDelayMs * retryMultiplier);
        return Math.min(nextDelayMs, maxRetryIntervalMs);
    }

    @FunctionalInterface
    private interface EmbeddingCall {
        TextEmbeddingResult call() throws Exception;
    }
}
