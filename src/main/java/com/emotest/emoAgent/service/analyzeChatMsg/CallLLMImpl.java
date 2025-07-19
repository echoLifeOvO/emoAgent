package com.emotest.emoAgent.service.analyzeChatMsg;

import com.emotest.emoAgent.model.OpenAIResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CallLLMImpl implements CallLLM {

    private final WebClient webClient;

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.model}")
    private String model;

    @Value("${dashscope.url}")
    private String url;

    public CallLLMImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public String callLLM(String prompt) {
        return callLLM(prompt, apiKey, model);
    }

    @Override
    public String callLLM(String prompt, String customApiKey, String customModel) {
        try {
            String useApiKey = customApiKey != null ? customApiKey : apiKey;
            String useModel = customModel != null ? customModel : model;
            
            log.info("调用DashScope API，模型: {}", useModel);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", useModel);
            
            Map<String, Object> input = new HashMap<>();
            input.put("messages", new Object[]{
                Map.of("role", "user", "content", prompt)
            });
            requestBody.put("input", input);
            
            requestBody.put("parameters", Map.of("result_format", "message"));

            log.info("发送请求到: {}", url);
            log.debug("请求体: {}", requestBody);

            String response = webClient.post()
                    .uri(url)
                    .header("Authorization", "Bearer " + useApiKey)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("收到响应，长度: {}", response != null ? response.length() : 0);
            log.debug("响应内容: {}", response);

            // 解析DashScope响应
            com.google.gson.JsonObject jsonResponse = new com.google.gson.JsonParser().parse(response).getAsJsonObject();
            String result = jsonResponse.getAsJsonObject("output")
                    .getAsJsonArray("choices")
                    .get(0)
                    .getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content")
                    .getAsString();
            log.info("解析后的结果: {}", result);
            
            return result;

        } catch (Exception e) {
            log.error("调用DashScope API失败", e);
            throw new RuntimeException("调用DashScope API失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String compressContent(String content) {
        String prompt = String.format(
            "请将以下内容进行压缩总结，保留关键信息，减少冗余内容，但保持逻辑完整性：\n\n%s",
            content
        );
        return callLLM(prompt);
    }
} 