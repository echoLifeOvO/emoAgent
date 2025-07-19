package com.emotest.emoAgent.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 消息实体类
 * 
 * @author emoAgent
 * @version 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    
    /**
     * 本地ID
     */
    private Integer localId;
    
    /**
     * 消息服务器ID
     */
    private Long msgSvrId;
    
    /**
     * 消息类型
     */
    private Integer type;
    
    /**
     * 子类型
     */
    private Integer subType;
    
    /**
     * 是否发送者（1=发送，0=接收）
     */
    private Integer isSender;
    
    /**
     * 创建时间（时间戳）
     */
    private Long createTime;
    
    /**
     * 对话者
     */
    private String strTalker;
    
    /**
     * 消息内容
     */
    private String strContent;
} 