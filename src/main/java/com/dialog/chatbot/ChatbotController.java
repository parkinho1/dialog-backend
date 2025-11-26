package com.dialog.chatbot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import com.dialog.user.service.CustomUserDetails;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import com.dialog.exception.ChatbotApiException;
import com.dialog.user.domain.MeetUser;

@RestController
@RequestMapping("/api/chatbot")
@Slf4j	// sysout 대신 log 찍는 어노테이션
public class ChatbotController {
    
    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;
    
    private final RestTemplate restTemplate;
    
    @Autowired
    public ChatbotController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    // 회의록 검색 챗봇 (Python으로 전달)
    @PostMapping("/search")
    public ResponseEntity<String> searchChat(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        log.info("🔹 [ChatBot] 요청 시작");
        
        if (userDetails != null) {
            MeetUser meetUser = userDetails.getMeetUser();
            
            Long userId = meetUser.getId();
            String job = meetUser.getJob() != null 
                ? meetUser.getJob().name() 
                : "NONE";
            String position = meetUser.getPosition() != null 
                ? meetUser.getPosition().name() 
                : "NONE";
            String userName = meetUser.getName();
            
            request.put("user_id", userId);
            request.put("user_job", job);
            request.put("user_position", position);
            request.put("user_name", userName);
            
            log.info("[ChatBot] User: " + userName + " (ID: " + userId + ", Job: " + job + ", Position: " + position + ")");
        }
        
        String url = fastApiBaseUrl + "/api/chat";
        log.info("[ChatBot] 전송 데이터: " + request);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ChatbotApiException("Python API 호출 실패: 상태 코드 " + response.getStatusCodeValue());
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("HTTP 오류 발생: {}", e.getStatusCode(), e);
            throw new ChatbotApiException("Python API 호출 HTTP 오류", e);
        } catch (ResourceAccessException e) {
            log.error("네트워크 오류: {}", e.getMessage(), e);
            throw new ChatbotApiException("Python API 호출 네트워크 오류", e);
        } catch (Exception e) {
            log.error("기타 오류: {}", e.getMessage(), e);
            throw new ChatbotApiException("Python API 호출 실패", e);
        }
    }
    
    // FAQ 챗봇 (Python으로 전달)
    @PostMapping("/faq")
    public ResponseEntity<String> faqChat(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
    	log.info("[FAQ] 요청 시작");
        
        if (userDetails != null) {
            MeetUser meetUser = userDetails.getMeetUser();
            
            Long userId = meetUser.getId();
            String job = meetUser.getJob() != null 
                ? meetUser.getJob().name() 
                : "NONE";
            String position = meetUser.getPosition() != null 
                ? meetUser.getPosition().name() 
                : "NONE";
            String userName = meetUser.getName();
            
            request.put("user_id", userId);
            request.put("user_job", job);
            request.put("user_position", position);
            request.put("user_name", userName);
            
            log.info("[FAQ] User: " + userName + " (ID: " + userId + ", Job: " + job + ", Position: " + position + ")");
        }
        
        String url = fastApiBaseUrl + "/api/faq";
        log.info("[FAQ] 전송 데이터: " + request);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ChatbotApiException("Python API 호출 실패: 상태 코드 " + response.getStatusCodeValue());
            }

            log.info("[FAQ] Python 응답 성공");
            return ResponseEntity.ok(response.getBody());

        } catch (HttpClientErrorException | HttpServerErrorException e) {
            log.error("[FAQ] HTTP 오류: {}", e.getStatusCode(), e);
            throw new ChatbotApiException("Python API 호출 HTTP 오류", e);
        } catch (ResourceAccessException e) {
            log.error("[FAQ] 네트워크 오류: {}", e.getMessage(), e);
            throw new ChatbotApiException("Python API 호출 네트워크 오류", e);
        } catch (Exception e) {
            log.error("[FAQ] Python 호출 실패: {}", e.getMessage(), e);
            throw new ChatbotApiException("Python API 호출 실패", e);
        }
    }
}