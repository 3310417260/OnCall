package org.example.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 召回阶段上下文，用于归一化和统一打分
 */
@Getter
@Setter
public class RecallContext {

    private List<String> keywords;
    private float minVectorDistance;
    private float maxVectorDistance;
}
