package com.emotest.emoAgent.service.analyzeChatMsg;

/**
 * LLM调用服务接口
 * 
 * @author emoAgent
 * @version 1.0.0
 */
public interface CallLLM {
    
    /**
     * 调用LLM进行分析
     * 
     * @param prompt 提示词
     * @return LLM响应结果
     */
    String callLLM(String prompt);
    
    /**
     * 调用LLM进行分析（使用自定义API Key和模型）
     * 
     * @param prompt 提示词
     * @param apiKey 阿里云DashScope API Key
     * @param model 模型名称
     * @return LLM响应结果
     */
    String callLLM(String prompt, String apiKey, String model);
    
    /**
     * 调用LLM进行压缩总结
     * 
     * @param content 需要压缩的内容
     * @return 压缩后的内容
     */
    String compressContent(String content);
}
