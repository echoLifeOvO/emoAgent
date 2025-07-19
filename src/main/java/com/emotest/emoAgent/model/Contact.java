package com.emotest.emoAgent.model;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 联系人实体类
 * 
 * @author emoAgent
 * @version 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Contact {
    
    /**
     * 用户名
     */
    private String userName;
    
    /**
     * 昵称
     */
    private String nickName;
    
    /**
     * 备注
     */
    private String reMark;
    
    /**
     * 别名
     */
    private String alias;
} 