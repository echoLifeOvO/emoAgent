package com.emotest.emoAgent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 微信聊天记录分析Web页面控制器
 * 
 * @author 微信聊天记录分析
 * @version 1.0.0
 */
@Controller
public class WebController {

    /**
     * 主页面
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
} 