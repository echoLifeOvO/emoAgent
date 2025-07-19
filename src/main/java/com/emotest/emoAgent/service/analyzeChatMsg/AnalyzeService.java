package com.emotest.emoAgent.service.analyzeChatMsg;

import com.emotest.emoAgent.model.Message;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 聊天分析服务
 * 
 * @author emoAgent
 * @version 1.0.0
 */
@Service
@Log4j2
public class AnalyzeService {

    @Autowired
    private LoadChatMsgUtil loadChatMsgUtil;

    @Autowired
    private CallLLM callLLM;

    // 每批处理的聊天记录数量（针对130000 tokens模型优化）
    private static final int BATCH_SIZE = 180;
    
    // 最大token限制（针对130000 tokens模型）
    private static final int MAX_TOKENS = 8000;
    
    // 并发线程数（限制为2）
    private static final int CONCURRENT_THREADS = 2;
    
    // 重叠度配置（调整为最大重叠）
    private static final double MIN_OVERLAP_RATIO = 0.3; // 最小重叠比例 30%
    private static final double MAX_OVERLAP_RATIO = 0.6; // 最大重叠比例 60%
    private static final int MIN_OVERLAP_SIZE = 30; // 最小重叠条数
    private static final int MAX_OVERLAP_SIZE = 80; // 最大重叠条数
    
    // API限流配置
    private static final long API_CALL_DELAY_MS = 2000; // API调用间隔2秒
    private static final int MAX_RETRY_ATTEMPTS = 3; // 最大重试次数

    /**
     * 分析聊天记录并生成报告（使用默认配置）
     * 
     * @param dataPath 微信数据路径
     * @param friendName 好友姓名
     * @return 分析结果
     */
    public String analyzeChatAndGenerateReport(String dataPath, String friendName) {
        return analyzeChatAndGenerateReport(dataPath, friendName, null, null);
    }

    /**
     * 分析聊天记录并生成报告（使用自定义API Key和模型）
     * 
     * @param dataPath 微信数据路径
     * @param friendName 好友姓名
     * @param apiKey 阿里云DashScope API Key
     * @param model 模型名称
     * @return 分析结果
     */
    public String analyzeChatAndGenerateReport(String dataPath, String friendName, String apiKey, String model) {
        try {
            log.info("开始分析聊天记录，好友: {}, 模型: {}", friendName, model);
            
            // 1. 获取联系人信息
            var contacts = loadChatMsgUtil.findContacts(dataPath, friendName);
            if (contacts.isEmpty()) {
                return "未找到指定好友";
            }
            
            // 2. 获取聊天记录
            List<Message> allMessages = new ArrayList<>();
            for (var contact : contacts) {
                var messages = loadChatMsgUtil.findMessages(dataPath, contact.getUserName());
                allMessages.addAll(messages);
            }
            
            if (allMessages.isEmpty()) {
                return "未找到聊天记录";
            }
            
            log.info("找到 {} 条聊天记录", allMessages.size());
            
            // 3. 过滤文字消息并格式化
            List<String> textMessages = formatTextMessages(allMessages);
            
            // 4. 并发分批处理聊天记录（使用自定义API Key和模型）
            String analysisResult = processChatInBatchesConcurrently(textMessages, apiKey, model);
            
            // 5. 生成最终报告（使用自定义API Key和模型）
            String finalReport = generateFinalReport(analysisResult, friendName, apiKey, model);
            
            // 6. 保存报告到文件
            saveReportToFile(finalReport, friendName);
            
            log.info("聊天分析完成");
            return finalReport;
            
        } catch (Exception e) {
            log.error("分析聊天记录失败", e);
            return "分析失败：" + e.getMessage();
        }
    }

    /**
     * 格式化文字消息
     */
    private List<String> formatTextMessages(List<Message> messages) {
        List<String> textMessages = new ArrayList<>();
        
        for (Message msg : messages) {
            // 只处理文字消息（type=1）
            if (msg.getType() == 1 && msg.getStrContent() != null && !msg.getStrContent().trim().isEmpty()) {
                String sender = msg.getIsSender() == 1 ? "我" : "对方";
                String time = loadChatMsgUtil.formatTime(msg.getCreateTime());
                String content = msg.getStrContent().trim();
                
                String formattedMessage = String.format("[%s] %s: %s", time, sender, content);
                textMessages.add(formattedMessage);
            }
        }
        
        log.info("格式化后得到 {} 条文字消息", textMessages.size());
        return textMessages;
    }

    /**
     * 并发分批处理聊天记录
     */
    private String processChatInBatchesConcurrently(List<String> textMessages) {
        return processChatInBatchesConcurrently(textMessages, null, null);
    }

    /**
     * 并发分批处理聊天记录（使用自定义API Key和模型）
     */
    private String processChatInBatchesConcurrently(List<String> textMessages, String apiKey, String model) {
        List<List<String>> batches = splitIntoOverlappingBatches(textMessages);
        log.info("将聊天记录分为 {} 批进行并发处理", batches.size());
        
        // 创建线程池
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        List<CompletableFuture<String>> futures = new ArrayList<>();
        
        // 提交并发任务（带限流控制）
        for (int i = 0; i < batches.size(); i++) {
            final int batchIndex = i;
            final List<String> batch = batches.get(i);
            
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                return processBatchWithRetry(batch, batchIndex + 1, batches.size(), apiKey, model);
            }, executor);
            
            futures.add(future);
            
            // 添加延迟，避免API调用过于频繁
            if (i > 0 && i % CONCURRENT_THREADS == 0) {
                try {
                    Thread.sleep(API_CALL_DELAY_MS);
                    log.info("添加API调用延迟，已处理 {} 个批次", i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("延迟被中断");
                }
            }
        }
        
        // 等待所有任务完成
        try {
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );
            allFutures.get(5, TimeUnit.MINUTES); // 设置超时时间
            
            // 收集结果
            List<String> batchResults = new ArrayList<>();
            for (CompletableFuture<String> future : futures) {
                batchResults.add(future.get());
            }
            
            log.info("所有批次分析完成，开始合并结果");
            
            // 合并和压缩结果
            return mergeAndCompressResults(batchResults);
            
        } catch (Exception e) {
            log.error("并发处理失败", e);
            return "并发处理失败：" + e.getMessage();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 将消息列表分割成重叠批次
     * 使用动态重叠算法确保上下文连续性
     */
    private List<List<String>> splitIntoOverlappingBatches(List<String> textMessages) {
        List<List<String>> batches = new ArrayList<>();
        
        if (textMessages.isEmpty()) {
            return batches;
        }
        
        // 计算动态重叠大小
        int overlapSize = calculateDynamicOverlapSize(textMessages.size());
        log.info("消息总数: {}, 批次大小: {}, 重叠大小: {}", 
                textMessages.size(), BATCH_SIZE, overlapSize);
        
        // 计算步长（每次前进的距离）
        int stepSize = BATCH_SIZE - overlapSize;
        
        // 生成重叠批次
        int startIndex = 0;
        int batchIndex = 1;
        
        while (startIndex < textMessages.size()) {
            int endIndex = Math.min(startIndex + BATCH_SIZE, textMessages.size());
            List<String> batch = textMessages.subList(startIndex, endIndex);
            
            log.info("生成批次 {}: 索引范围 [{}, {}), 消息数量: {}", 
                    batchIndex, startIndex, endIndex, batch.size());
            
            batches.add(new ArrayList<>(batch));
            
            // 移动到下一个批次
            startIndex += stepSize;
            batchIndex++;
            
            // 如果剩余消息不足一个完整批次，但还有重叠空间，则调整
            if (startIndex + BATCH_SIZE > textMessages.size() && startIndex < textMessages.size()) {
                int remainingSize = textMessages.size() - startIndex;
                if (remainingSize >= MIN_OVERLAP_SIZE) {
                    // 创建最后一个批次，包含剩余所有消息
                    List<String> lastBatch = textMessages.subList(startIndex, textMessages.size());
                    log.info("生成最终批次 {}: 索引范围 [{}, {}), 消息数量: {}", 
                            batchIndex, startIndex, textMessages.size(), lastBatch.size());
                    batches.add(new ArrayList<>(lastBatch));
                }
                break;
            }
        }
        
        log.info("总共生成 {} 个重叠批次", batches.size());
        return batches;
    }
    
    /**
     * 计算动态重叠大小
     * 根据总消息数量和并发数动态调整重叠度
     */
    private int calculateDynamicOverlapSize(int totalMessages) {
        // 基础重叠比例：消息越多，重叠比例越大
        double baseOverlapRatio;
        
        if (totalMessages <= 200) {
            // 少量消息：使用中等重叠
            baseOverlapRatio = MIN_OVERLAP_RATIO + (MAX_OVERLAP_RATIO - MIN_OVERLAP_RATIO) * 0.3;
        } else if (totalMessages <= 800) {
            // 中等消息：线性增长重叠比例
            baseOverlapRatio = MIN_OVERLAP_RATIO + 
                (MAX_OVERLAP_RATIO - MIN_OVERLAP_RATIO) * 
                (totalMessages - 200) / 600.0;
        } else {
            // 大量消息：使用最大重叠
            baseOverlapRatio = MAX_OVERLAP_RATIO;
        }
        
        // 由于并发数只有2，增加重叠度以确保更好的连续性
        baseOverlapRatio = Math.min(MAX_OVERLAP_RATIO, baseOverlapRatio * 1.2);
        
        // 计算重叠大小
        int overlapSize = (int) Math.round(BATCH_SIZE * baseOverlapRatio);
        
        // 确保重叠大小在合理范围内
        overlapSize = Math.max(MIN_OVERLAP_SIZE, Math.min(MAX_OVERLAP_SIZE, overlapSize));
        
        // 确保重叠大小不超过批次大小的70%（允许更大重叠）
        overlapSize = Math.min(overlapSize, (int)(BATCH_SIZE * 0.7));
        
        log.info("动态重叠计算 - 总消息: {}, 基础重叠比例: {:.2f}, 最终重叠大小: {} (并发数: {})", 
                totalMessages, baseOverlapRatio, overlapSize, CONCURRENT_THREADS);
        
        return overlapSize;
    }
    
    /**
     * 处理批次并支持重试机制
     */
    private String processBatchWithRetry(List<String> batch, int batchIndex, int totalBatches) {
        return processBatchWithRetry(batch, batchIndex, totalBatches, null, null);
    }

    /**
     * 处理批次并支持重试机制（使用自定义API Key和模型）
     */
    private String processBatchWithRetry(List<String> batch, int batchIndex, int totalBatches, String apiKey, String model) {
        int retryCount = 0;
        
        while (retryCount < MAX_RETRY_ATTEMPTS) {
            try {
                log.info("线程 {} 开始处理第 {} 批，包含 {} 条消息 (重试次数: {})", 
                        Thread.currentThread().getName(), batchIndex, batch.size(), retryCount);
                
                String batchContent = String.join("\n", batch);
                String prompt = createDetailedAnalysisPrompt(batchContent, batchIndex, totalBatches);
                
                // 添加随机延迟，避免同时调用
                if (retryCount > 0) {
                    long delay = API_CALL_DELAY_MS + (long)(Math.random() * 1000);
                    Thread.sleep(delay);
                    log.info("重试前等待 {} ms", delay);
                }
                
                String batchResult = callLLM.callLLM(prompt, apiKey, model);
                
                log.info("线程 {} 完成第 {} 批分析，结果长度: {}", 
                        Thread.currentThread().getName(), batchIndex, batchResult.length());
                
                return batchResult;
                
            } catch (Exception e) {
                retryCount++;
                log.error("线程 {} 处理第 {} 批时发生错误 (重试 {}/{}): {}", 
                        Thread.currentThread().getName(), batchIndex, retryCount, MAX_RETRY_ATTEMPTS, e.getMessage());
                
                if (retryCount >= MAX_RETRY_ATTEMPTS) {
                    return "批次 " + batchIndex + " 分析失败（已重试" + MAX_RETRY_ATTEMPTS + "次）：" + e.getMessage();
                }
                
                // 重试前等待更长时间
                try {
                    Thread.sleep(API_CALL_DELAY_MS * retryCount);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return "批次 " + batchIndex + " 处理被中断";
                }
            }
        }
        
        return "批次 " + batchIndex + " 处理失败";
    }

    /**
     * 合并和压缩结果
     */
    private String mergeAndCompressResults(List<String> batchResults) {
        log.info("开始合并 {} 批分析结果", batchResults.size());
        
        String mergedContent = String.join("\n\n=== 批次分隔 ===\n\n", batchResults);
        
        // 如果合并后的内容太长，需要进一步压缩
        if (mergedContent.length() > MAX_TOKENS * 3) { // 估算字符数
            log.info("合并内容过长，进行压缩，长度: {}", mergedContent.length());
            return callLLM.compressContent(mergedContent);
        }
        
        return mergedContent;
    }

    /**
     * 生成最终报告
     */
    private String generateFinalReport(String analysisResult, String friendName) {
        return generateFinalReport(analysisResult, friendName, null, null);
    }

    /**
     * 生成最终报告（使用自定义API Key和模型）
     */
    private String generateFinalReport(String analysisResult, String friendName, String apiKey, String model) {
        log.info("生成最终报告");
        
        String prompt = createFinalReportPrompt(analysisResult, friendName);
        return callLLM.callLLM(prompt, apiKey, model);
    }

    /**
     * 创建详细的分析提示词（支持重叠批次）
     */
    private String createDetailedAnalysisPrompt(String chatContent, int batchIndex, int totalBatches) {
        return String.format(
            "你是一位专业的聊天记录分析师，请对以下聊天记录片段进行深入分析。\n\n" +
            "当前分析：第 %d/%d 批（重叠批次分析）\n" +
            "注意：此批次可能与前后批次有重叠内容，请重点关注本批次的核心内容，同时考虑上下文连续性。\n\n" +
            "分析要求：\n" +
            "1. **对话主题识别**：识别这段对话的主要话题、讨论内容、事件背景\n" +
            "2. **情感状态分析**：分析双方在这段对话中的情绪状态、情感表达、态度倾向\n" +
            "3. **沟通模式观察**：观察双方的沟通方式、语言风格、互动模式\n" +
            "4. **关系动态分析**：分析这段对话中体现的两人关系特征、亲密度、信任度\n" +
            "5. **关键信息提取**：提取重要的时间节点、关键事件、情感转折点\n" +
            "6. **潜在问题识别**：识别可能存在的沟通问题、情感冲突、关系隐患\n" +
            "7. **上下文连续性**：注意与前后对话的连贯性，避免重复分析重叠部分\n\n" +
            "输出格式：\n" +
            "## 对话主题\n" +
            "[详细描述对话的主要内容和背景]\n\n" +
            "## 情感状态\n" +
            "- 对方情感：[分析对方的情感状态和表达]\n" +
            "- 我的情感：[分析我的情感状态和表达]\n\n" +
            "## 沟通特点\n" +
            "[分析双方的沟通方式和互动模式]\n\n" +
            "## 关系体现\n" +
            "[分析这段对话体现的关系特征]\n\n" +
            "## 关键信息\n" +
            "[提取的重要信息和时间节点]\n\n" +
            "## 潜在问题\n" +
            "[识别的问题和隐患]\n\n" +
            "## 上下文连续性\n" +
            "[简要说明与前后对话的连贯性]\n\n" +
            "聊天记录：\n%s",
            batchIndex, totalBatches, chatContent
        );
    }

    /**
     * 创建最终报告提示词
     */
    private String createFinalReportPrompt(String analysisResult, String friendName) {
        return String.format(
            "你是一位资深的心理咨询师和人际关系专家，请基于以下多批次聊天分析结果，生成一份专业、全面、深入的聊天关系分析报告。\n\n" +
            "报告要求：\n" +
            "1. **关系类型判断**：基于聊天内容、互动模式、情感表达，准确判断两人的关系类型（情侣、朋友、同事、家人等）\n" +
            "2. **关系质量评估**：评估关系的健康度、稳定性、发展潜力\n" +
            "3. **情感态度分析**：深入分析双方对彼此的真实情感态度、依赖程度、期望值\n" +
            "4. **沟通模式诊断**：诊断双方的沟通模式、表达方式、理解能力\n" +
            "5. **问题识别与建议**：识别关系中存在的问题，提供具体的改进建议\n" +
            "6. **发展建议**：为关系发展提供专业建议\n\n" +
            "报告结构：\n" +
            "=== 聊天关系分析报告 ===\n" +
            "分析对象：%s\n" +
            "分析时间：%s\n" +
            "分析师：AI心理咨询师\n\n" +
            "## 关系类型判断\n" +
            "### 关系类型\n" +
            "[明确判断关系类型，并说明判断依据]\n\n" +
            "### 关系特征\n" +
            "[详细描述关系的特征、模式、特点]\n\n" +
            "### 关系质量评估\n" +
            "[评估关系的健康度、稳定性、满意度]\n\n" +
            "## 情感态度深度分析\n" +
            "### 对方对\"我\"的情感态度\n" +
            "- 情感依赖度：[分析依赖程度]\n" +
            "- 信任程度：[分析信任水平]\n" +
            "- 期望值：[分析对关系的期望]\n" +
            "- 情感表达方式：[分析情感表达的特点]\n\n" +
            "### \"我\"对对方的情感态度\n" +
            "- 情感投入度：[分析情感投入程度]\n" +
            "- 关心程度：[分析关心和重视程度]\n" +
            "- 包容度：[分析包容和理解程度]\n" +
            "- 情感表达方式：[分析情感表达的特点]\n\n" +
            "## 沟通模式诊断\n" +
            "### 沟通特点\n" +
            "[分析双方的沟通方式、语言风格、表达习惯]\n\n" +
            "### 沟通效果\n" +
            "[评估沟通的有效性、理解度、共鸣度]\n\n" +
            "### 沟通问题\n" +
            "[识别沟通中存在的问题和障碍]\n\n" +
            "## 问题识别与改进建议\n" +
            "### 主要问题\n" +
            "[识别关系中的主要问题和隐患]\n\n" +
            "### 改进建议\n" +
            "#### 对\"我\"的建议\n" +
            "[针对\"我\"的具体改进建议]\n\n" +
            "#### 对对方的建议\n" +
            "[针对对方的建议（如果适用）]\n\n" +
            "#### 关系发展建议\n" +
            "[对关系发展的整体建议]\n\n" +
            "## 总结\n" +
            "[对整体关系的总结和展望]\n\n" +
            "=== 报告结束 ===\n\n" +
            "分析数据：\n%s",
            friendName,
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            analysisResult
        );
    }

    /**
     * 保存报告到文件
     */
    private String saveReportToFile(String report, String friendName) {
        try {
            String resourcePath = "src/main/resources";
            // 生成文件名：friendName+report.txt
            String fileName = friendName + "+report.txt";
            String reportPath = resourcePath + File.separator + fileName;
            
            // 确保目录存在
            Files.createDirectories(Paths.get(resourcePath));
            
            // 写入报告
            try (FileWriter writer = new FileWriter(reportPath, false)) {
                writer.write(report);
            }
            
            log.info("报告已保存到: {}", reportPath);
            return reportPath;
            
        } catch (IOException e) {
            log.error("保存报告失败", e);
            return null;
        }
    }
}
