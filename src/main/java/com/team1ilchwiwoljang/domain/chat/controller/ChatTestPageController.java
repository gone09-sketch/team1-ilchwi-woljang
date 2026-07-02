package com.team1ilchwiwoljang.domain.chat.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * WebSocket 채팅 테스트 화면을 local 프로파일에서만 제공합니다.
 * 테스트 화면은 회원가입/로그인/관리자 조회 호출까지 포함하므로 static 리소스로 항상 노출하지 않습니다.
 */
@Controller
@Profile("local")
public class ChatTestPageController {

    @GetMapping(value = "/chat-test.html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> getChatTestPage() {
        return ResponseEntity.ok(new ClassPathResource("chat-test/chat-test.html"));
    }
}
