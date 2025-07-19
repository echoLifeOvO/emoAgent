package com.emotest.emoAgent.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * OpenAI API 响应实体类
 * 
 * @author emoAgent
 * @version 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OpenAIResponse {
    
    /**
     * 选择列表
     */
    private Choice[] choices;
    
    /**
     * 对象类型
     */
    private String object;
    
    /**
     * 使用情况
     */
    private Usage usage;
    
    /**
     * 创建时间
     */
    private Long created;
    
    /**
     * 系统指纹
     */
    private String systemFingerprint;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 响应ID
     */
    private String id;
    
    /**
     * 选择实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Choice {
        /**
         * 消息内容
         */
        private Message message;
        
        /**
         * 完成原因
         */
        private String finishReason;
        
        /**
         * 索引
         */
        private Integer index;
        
        /**
         * 日志概率
         */
        private Object logprobs;
    }
    
    /**
     * 消息实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        /**
         * 角色
         */
        private String role;
        
        /**
         * 内容
         */
        private String content;
    }
    
    /**
     * 使用情况实体
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Usage {
        /**
         * 提示词token数
         */
        private Integer promptTokens;
        
        /**
         * 完成token数
         */
        private Integer completionTokens;
        
        /**
         * 总token数
         */
        private Integer totalTokens;
        
        /**
         * 提示词token详情
         */
        private PromptTokensDetails promptTokensDetails;
    }
    
    /**
     * 提示词token详情
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PromptTokensDetails {
        /**
         * 缓存的token数
         */
        private Integer cachedTokens;
    }
} 