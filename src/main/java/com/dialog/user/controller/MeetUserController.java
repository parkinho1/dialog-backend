package com.dialog.user.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.exception.UserRoleAccessDeniedException;
import com.dialog.global.utill.CookieUtil;
import com.dialog.security.jwt.JwtTokenProvider;
import com.dialog.security.oauth2.SocialUserInfo;
import com.dialog.security.oauth2.SocialUserInfoFactory;
import com.dialog.token.domain.RefreshTokenDto;
import com.dialog.token.service.RefreshTokenServiceImpl;
import com.dialog.user.domain.ForgotPasswordRequestDTO;
import com.dialog.user.domain.HomeStatsDto;
import com.dialog.user.domain.Job;
import com.dialog.user.domain.LoginDto;
import com.dialog.user.domain.MeetUser;
import com.dialog.user.domain.MeetUserDto;
import com.dialog.user.domain.ResetPasswordRequestDTO;
import com.dialog.user.domain.UserApiResponseDTO;
import com.dialog.user.domain.UserSettingsUpdateDto;
import com.dialog.user.service.CustomUserDetails;
import com.dialog.user.service.MeetuserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController 
@RequiredArgsConstructor
public class MeetUserController {

	private final MeetuserService meetuserService;
	private final JwtTokenProvider jwtTokenProvider;
	private final RefreshTokenServiceImpl refreshTokenService;
	private final CookieUtil cookieUtil;

    // 회원가입
    @PostMapping("/api/auth/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody MeetUserDto dto) {
        // 클라이언트로부터 회원가입 요청 데이터(dto)를 받음 (JSON → MeetUserDto 변환)
        // Service 계층의 signup 메서드 호출하여 회원가입 처리
        // (가입정보 유효성 검증, 중복체크, 비밀번호 암호화, DB 저장 포함)
        meetuserService.signup(dto);

        // 회원가입 성공 메시지와 함께 HTTP 200 OK 응답 반환
        return ResponseEntity.ok(Map.of("success", true, "message", "회원가입 성공"));
    }

    // 로그인
    @PostMapping(value = "/api/auth/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginDto dto, HttpServletResponse response) {
        // 클라이언트로부터 로그인 요청 데이터(email, password)를 받음
        // Service 계층 login() 호출하여 사용자 인증 및 검증 수행
        // (DB에서 사용자 조회, 비밀번호 검증, 활성화 상태 확인)
        MeetUser user = meetuserService.login(dto.getEmail(), dto.getPassword());

        // 인증 정보를 토대로 Authentication 객체 생성 (Spring Security 내 권한정보 포함)
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            user.getEmail(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        // Access Token(JWT) 토큰 생성
        String accessToken = jwtTokenProvider.createToken(authentication);
        // Refresh Token 생성 및 반환할 DTO 생성
        RefreshTokenDto refreshTokenDto = refreshTokenService.createRefreshTokenDto(user);

        // HTTP 응답 설정 (CookieUtill 클래스에서 쿠키 처리)
        response.addCookie(cookieUtil.createAccessTokenCookie(accessToken));
        response.addCookie(cookieUtil.createRefreshTokenCookie(refreshTokenDto.getRefreshToken()));

        // 아이디 기억하기 처리
        if (dto.isRememberId()) {
            response.addCookie(cookieUtil.createRememberMeCookie(user.getEmail()));
        } else {
        	// 아이디 기억하기는 js 에서 접근해도 위험이 적어 HttpOnly=false 로 설정해줌
            response.addCookie(cookieUtil.deleteCookie("savedEmail", false));
        }
        // 직무가 None 인지 확인
        boolean needJobSetup = (user.getJob() == Job.NONE);
        
        // 로그인 성공 정보와 토큰, 사용자 정보 등을 JSON 응답의 형태로 반환
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "로그인 성공",
            "refreshTokenExpiresAt", refreshTokenDto.getExpiresAt(),
            "needJobSetup", needJobSetup,
            "user", Map.of(
                "name", user.getName(),
                "email", user.getEmail(),
                "role", user.getRole().name()
            )
        ));
    }
    
    // 로그아웃 (HttpOnly 쿠키 삭제를 위해 필수)
    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletResponse response, Authentication authentication) {   	
    	// 로그아웃 시 쿠키 삭제
        response.addCookie(cookieUtil.deleteCookie("jwt"));
        response.addCookie(cookieUtil.deleteCookie("refreshToken"));
        // DB에서 Refresh Token 삭제 
        if (authentication != null && authentication.isAuthenticated()) {
            refreshTokenService.deleteByEmail(authentication.getName());
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "로그아웃 성공"));
    }

    // 현재 로그인된 사용자 정보 조회
    @GetMapping("/api/auth/me")
    public ResponseEntity<?> getCurrentUserInfo(Authentication authentication) {
        // 현재 인증된 사용자 인증 정보를 파라미터로 받아서,
        // 서비스 계층 getCurrentUser() 호출해 사용자 상세정보 조회
        MeetUserDto dto = meetuserService.getCurrentUser(authentication);

        // 사용자 이름, 이메일, 역할 정보를 JSON 형태로 응답 반환
        return ResponseEntity.ok(Map.of(
            "name", dto.getName(),
            "email", dto.getEmail(),
            "role", dto.getRole()
        ));
    }

    // 설정 페이지에서 사용자 정보(직무/직급) 업데이트
    @PutMapping("/api/user/settings")
    public ResponseEntity<?> updateUserSettings(Authentication authentication, 
                                                @Valid @RequestBody UserSettingsUpdateDto dto) {
        // 인증 정보 유무 확인, 인증 안 된 경우 커스텀 예외 던짐
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UserRoleAccessDeniedException("인증되지 않은 사용자입니다.");
        }

        // 사용자 인증 정보를 바탕으로 서비스 계층에 업데이트 작업 위임
        meetuserService.updateUserSettings(authentication, dto);

        // 성공 메시지를 포함한 HTTP 200 OK 응답 반환
        return ResponseEntity.ok(Map.of("success", true, "message", "개인정보가 성공적으로 저장되었습니다."));
    }
    
    // 비밀번호 재설정 메일 발송 요청
    @PostMapping("/api/auth/forgotPassword")
    public ResponseEntity<UserApiResponseDTO> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        meetuserService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok(new UserApiResponseDTO(true, "비밀번호 재설정 이메일이 발송되었습니다."));
    }

    // 비밀번호 재설정 (토큰검증 + 비밀번호 변경)
    @PostMapping("/api/auth/resetPassword")
    public ResponseEntity<UserApiResponseDTO> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO request) {
        meetuserService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(new UserApiResponseDTO(true, "비밀번호가 성공적으로 변경되었습니다."));
    }
    
    @GetMapping("api/home/stats")
    public ResponseEntity<HomeStatsDto> getHomeStats(@AuthenticationPrincipal CustomUserDetails userDetails) {
        // 로그인 체크
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        // 서비스 호출 및 응답
        HomeStatsDto stats = meetuserService.getHomeStats(userDetails.getId());
        return ResponseEntity.ok(stats);
    }
}
