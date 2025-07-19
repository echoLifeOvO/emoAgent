package com.emotest.emoAgent.service.analyzeChatMsg;

import com.emotest.emoAgent.model.Contact;
import com.emotest.emoAgent.model.Message;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 微信聊天记录加载工具类
 * 
 * @author emoAgent
 * @version 1.0.0
 */
@Component
@Log4j2
public class LoadChatMsgUtil {

    /**
     * 查找联系人
     * 
     * @param dataPath 微信数据路径
     * @param friendName 好友姓名
     * @return 联系人列表
     */
    public List<Contact> findContacts(String dataPath, String friendName) {
        List<Contact> contacts = new ArrayList<>();
        String microMsgPath = dataPath + File.separator + "MicroMsg.db";
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + microMsgPath)) {
            String query = "SELECT UserName, NickName, ReMark, Alias FROM Contact " +
                          "WHERE NickName LIKE ? OR ReMark LIKE ? OR Alias LIKE ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                String pattern = "%" + friendName + "%";
                stmt.setString(1, pattern);
                stmt.setString(2, pattern);
                stmt.setString(3, pattern);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Contact contact = new Contact();
                        contact.setUserName(rs.getString("UserName"));
                        contact.setNickName(rs.getString("NickName"));
                        contact.setReMark(rs.getString("ReMark"));
                        contact.setAlias(rs.getString("Alias"));
                        contacts.add(contact);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("查找联系人失败", e);
        }
        
        return contacts;
    }
    


    /**
     * 查找消息记录
     * 
     * @param dataPath 微信数据路径
     * @param userName 用户名
     * @return 消息列表
     */
    public List<Message> findMessages(String dataPath, String userName) {
        List<Message> allMessages = new ArrayList<>();
        
        // 查找MSG0.db
        String msg0Path = dataPath + File.separator + "Multi" + File.separator + "MSG0.db";
        File msg0File = new File(msg0Path);
        if (msg0File.exists()) {
            List<Message> messages = queryMessages(msg0Path, userName);
            if (messages != null) {
                allMessages.addAll(messages);
            }
        }
        
        // 查找MSG1.db
        String msg1Path = dataPath + File.separator + "Multi" + File.separator + "MSG1.db";
        File msg1File = new File(msg1Path);
        if (msg1File.exists()) {
            List<Message> messages = queryMessages(msg1Path, userName);
            if (messages != null) {
                allMessages.addAll(messages);
            }
        }
        
        return allMessages;
    }

    /**
     * 查询消息
     * 
     * @param dbPath 数据库路径
     * @param userName 用户名
     * @return 消息列表
     */
    private List<Message> queryMessages(String dbPath, String userName) {
        List<Message> messages = new ArrayList<>();
        
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbPath)) {
            String query = "SELECT LocalId, MsgSvrID, Type, SubType, IsSender, CreateTime, StrTalker, StrContent " +
                          "FROM MSG WHERE StrTalker = ? ORDER BY CreateTime DESC LIMIT 1000";
            
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, userName);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Message msg = new Message();
                        msg.setLocalId(rs.getInt("LocalId"));
                        msg.setMsgSvrId(rs.getLong("MsgSvrID"));
                        msg.setType(rs.getInt("Type"));
                        msg.setSubType(rs.getInt("SubType"));
                        msg.setIsSender(rs.getInt("IsSender"));
                        msg.setCreateTime(rs.getLong("CreateTime"));
                        msg.setStrTalker(rs.getString("StrTalker"));
                        msg.setStrContent(rs.getString("StrContent"));
                        messages.add(msg);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("查询消息失败: " + dbPath, e);
        }
        
        return messages;
    }

    /**
     * 格式化时间戳
     * 
     * @param timestamp 时间戳
     * @return 格式化的时间字符串
     */
    public String formatTime(Long timestamp) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 获取发送者标识
     * 
     * @param isSender 是否发送者
     * @return 发送者标识
     */
    public String getSenderLabel(Integer isSender) {
        return isSender == 1 ? "发送" : "接收";
    }

    /**
     * 截断内容
     * 
     * @param content 原始内容
     * @param maxLength 最大长度
     * @return 截断后的内容
     */
    public String truncateContent(String content, int maxLength) {
        if (content == null || content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "...";
    }

    /**
     * 查找好友消息（主方法）
     * 
     * @param dataPath 微信数据路径
     * @param friendName 好友姓名
     * @return 查找结果
     */
    public String findFriendMessages(String dataPath, String friendName) {
        StringBuilder result = new StringBuilder();
        
        // 1. 首先在MicroMsg.db中查找好友信息
        List<Contact> contacts = findContacts(dataPath, friendName);
        
        if (contacts.isEmpty()) {
            result.append("未找到名为 '").append(friendName).append("' 的好友\n");
            return result.toString();
        }
        
        result.append("找到 ").append(contacts.size()).append(" 个匹配的联系人:\n");
        for (int i = 0; i < contacts.size(); i++) {
            Contact contact = contacts.get(i);
            result.append(String.format("%d. UserName: %s, NickName: %s, ReMark: %s, Alias: %s\n",
                i + 1, contact.getUserName(), contact.getNickName(), 
                contact.getReMark(), contact.getAlias()));
        }
        
        // 2. 查找聊天记录
        for (Contact contact : contacts) {
            result.append(String.format("\n=== 查找与 %s (%s) 的聊天记录 ===\n", 
                contact.getNickName(), contact.getUserName()));
            
            List<Message> messages = findMessages(dataPath, contact.getUserName());
            
            result.append("找到 ").append(messages.size()).append(" 条聊天记录:\n");
            
            int displayCount = Math.min(messages.size(), 20);
            for (int i = 0; i < displayCount; i++) {
                Message msg = messages.get(i);
                String msgTime = formatTime(msg.getCreateTime());
                String sender = getSenderLabel(msg.getIsSender());
                String content = truncateContent(msg.getStrContent(), 50);
                
                result.append(String.format("%d. [%s] %s: %s (类型:%d)\n",
                    i + 1, msgTime, sender, content, msg.getType()));
            }
            
            if (messages.size() > 20) {
                result.append("... 还有 ").append(messages.size() - 20).append(" 条记录\n");
            }
        }
        
        return result.toString();
    }
}
