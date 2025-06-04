package com.rsupport.board.common.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaFallbackController {

    // API 혹은 정적 리소스가 모두 매핑되지 않은 URL에 대해
    // index.html로 포워드 시킵니다.
    @RequestMapping(value = "/{path:[^\\.]*}")
    public String redirect() {
        return "forward:/index.html";
    }
}
