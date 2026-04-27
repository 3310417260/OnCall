package org.example.config;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 重排配置
 */
@Getter
@Configuration
@ConfigurationProperties(prefix = "rag.rerank")
public class RerankProperties {

    /**
     * 是否启用重排
     */
    private boolean enabled = false;

    /**
     * 向量召回候选数
     */
    private int recallTopN = 10;

    /**
     * 最终保留的结果数量
     */
    private int finalTopK = 3;

    /**
     * 语义分权重
     */
    private double semanticWeight = 0.50;

    /**
     * 关键词分权重
     */
    private double keywordWeight = 0.25;

    /**
     * 标题分权重
     */
    private double titleWeight = 0.15;

    /**
     * 结构分权重
     */
    private double structureWeight = 0.10;

    /**
     * 最低接受分数
     */
    private double minFinalScore = 0.25;

    /**
     * 单个来源最多保留多少个 chunk
     */
    private int maxChunksPerSource = 2;

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void setRecallTopN(int recallTopN) {
        this.recallTopN = recallTopN;
    }

    public void setFinalTopK(int finalTopK) {
        this.finalTopK = finalTopK;
    }

    public void setSemanticWeight(double semanticWeight) {
        this.semanticWeight = semanticWeight;
    }

    public void setKeywordWeight(double keywordWeight) {
        this.keywordWeight = keywordWeight;
    }

    public void setTitleWeight(double titleWeight) {
        this.titleWeight = titleWeight;
    }

    public void setStructureWeight(double structureWeight) {
        this.structureWeight = structureWeight;
    }

    public void setMinFinalScore(double minFinalScore) {
        this.minFinalScore = minFinalScore;
    }

    public void setMaxChunksPerSource(int maxChunksPerSource) {
        this.maxChunksPerSource = maxChunksPerSource;
    }
}
