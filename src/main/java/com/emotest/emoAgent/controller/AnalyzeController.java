package com.emotest.emoAgent.controller;

import com.emotest.emoAgent.service.analyzeChatMsg.AnalyzeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 微信聊天记录分析控制器
 * 
 * @author 微信聊天记录分析
 * @version 1.0.0
 */
@RestController
@RequestMapping("/analyze")
@Log4j2
public class AnalyzeController {

    @Autowired
    private AnalyzeService analyzeService;

    /**
     * 分析微信聊天记录并生成报告
     * 
     * @param dataPath 微信数据路径
     * @param friendName 好友姓名
     * @param apiKey 阿里云DashScope API Key
     * @param model 模型名称
     * @return 分析结果
     */
    @GetMapping("/chat")
    public String analyzeChat(@RequestParam String dataPath, 
                             @RequestParam String friendName,
                             @RequestParam String apiKey,
                             @RequestParam(defaultValue = "Moonshot-Kimi-K2-Instruct") String model) {
        log.info("开始分析微信聊天记录，路径: {}, 好友: {}, 模型: {}", dataPath, friendName, model);
        return analyzeService.analyzeChatAndGenerateReport(dataPath, friendName, apiKey, model);
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public String health() {
        return "微信聊天记录分析服务运行正常";
    }
} 