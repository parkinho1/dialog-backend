package com.dialog.meeting.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.meeting.domain.MeetingCreateRequestDto;
import com.dialog.meeting.domain.MeetingCreateResponseDto;
import com.dialog.meeting.domain.MeetingFinishRequestDto;
import com.dialog.meeting.domain.MeetingUpdateResultDto;
import com.dialog.meeting.service.MeetingService;
import com.dialog.meetingresult.domain.MeetingResult;
import com.dialog.security.oauth2.CustomOAuth2User;
import com.dialog.user.service.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

	private final MeetingService meetingService;

	// 새 회의 생성
	@PostMapping
	public ResponseEntity<MeetingCreateResponseDto> createMeeting(@RequestBody MeetingCreateRequestDto requestDto,
			Authentication authentication) throws IllegalAccessException {

		// 인증 정보가 없으면 401 Unauthorized 반환
		if (authentication == null) {
			log.warn("인증 정보 없음 - 인증되지 않은 요청");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		Object principal = authentication.getPrincipal(); // 인증된 사용자 주체 객체
		Long currentHostUserId; // 현재 회의 호스트(생성자) ID

		// 인증 주체(principal) 타입별로 사용자 ID 추출 (소셜/일반 가입 모두 처리)
		if (principal instanceof CustomOAuth2User) {
			currentHostUserId = ((CustomOAuth2User) principal).getMeetuser().getId();
		} else if (principal instanceof CustomUserDetails) {
			CustomUserDetails userDetails = (CustomUserDetails) principal;
			currentHostUserId = userDetails.getId();
		} else if (principal instanceof UserDetails) {
			// UserDetails 기본 구현체(일반 사용)는 CustomUserDetails가 아니라면 거부
			log.warn("인증 객체가 CustomUserDetails가 아님: {}", principal.getClass());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		} else {
			log.warn("알 수 없는 인증 객체 타입: {}", principal.getClass());
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 서비스 계층에 회의 생성 요청 (요청 데이터와 호스트 ID 전달)
		MeetingCreateResponseDto responseDto = meetingService.createMeeting(requestDto, currentHostUserId);

		// 생성된 회의 정보를 담은 DTO와 201(CREATED) 상태로 응답
		return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
	}

  // 전체 회의 목록 조회 (GET /api/meetings)
  @GetMapping
  public ResponseEntity<List<MeetingCreateResponseDto>> getAllMeetings() {
      List<MeetingCreateResponseDto> meetings = meetingService.getAllMeetings();
      return ResponseEntity.ok(meetings);
  }

	// 특정 회의 상세 조회 엔드포인트
	@GetMapping("/{meetingId}")
	public ResponseEntity<MeetingCreateResponseDto> getMeeting(@PathVariable("meetingId") Long meetingId) {
		try {
			// 서비스에서 ID로 회의 엔티티 조회
			MeetingCreateResponseDto responseDto = meetingService.findById(meetingId);
			// 회의를 찾을 수 없는 경우 404 반환
			if (responseDto == null) {
				return ResponseEntity.notFound().build();
			}
			// Meeting 엔티티를 DTO로 변환해 반환
			return ResponseEntity.ok(responseDto);
		} catch (Exception e) {
			// 예외 발생 시 500 에러 반환 (서버 로그 기록)
			log.error("Meeting 조회 중 예외 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PostMapping("/{meetingId}/finish")
	public ResponseEntity<?> finishMeeting(@PathVariable("meetingId") Long meetingId,
			@RequestBody MeetingFinishRequestDto requestDto) {
		try {
			log.info("회의 종료 요청 - meetingId: {}", meetingId);

			// 서비스에서 회의 종료 처리
			meetingService.finishMeeting(meetingId, requestDto);

			log.info("회의 종료 완료 - meetingId: {}", meetingId);
			return ResponseEntity.ok().build();

		} catch (IllegalArgumentException e) {
			log.error("회의 종료 실패 - 회의를 찾을 수 없음: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("회의를 찾을 수 없습니다: " + e.getMessage());
		} catch (Exception e) {
			log.error("회의 종료 중 예외 발생", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("회의 종료 중 오류가 발생했습니다: " + e.getMessage());
		}
	}

	// 회의 결과 저장 및 수정 (요약, 키워드, 액션아이템 등)
	// PATCH /api/meetings/{meetingId}
	@PatchMapping("/{meetingId}")
	public ResponseEntity<?> updateMeetingResult(@PathVariable("meetingId") Long meetingId,
			@RequestBody MeetingUpdateResultDto updateDto) {
		try {
			log.info("회의 결과 업데이트 요청 - ID: {}, DTO: {}", meetingId, updateDto);

			meetingService.updateMeetingResult(meetingId, updateDto);

			return ResponseEntity.ok().body("회의 결과가 성공적으로 저장되었습니다.");

		} catch (IllegalArgumentException e) {
			log.error("회의 결과 저장 실패 (잘못된 요청): {}", e.getMessage());
			return ResponseEntity.badRequest().body(e.getMessage());
		} catch (Exception e) {
			log.error("회의 결과 저장 중 서버 오류", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
		}
	}

  // 회의 삭제 엔드포인트
  @DeleteMapping("/{meetingId}")
  public ResponseEntity<?> deleteMeeting(@PathVariable("meetingId") Long meetingId) {
      try {
          log.info("회의 삭제 요청 - meetingId: {}", meetingId);
          meetingService.deleteMeeting(meetingId);
          return ResponseEntity.ok("성공적으로 삭제되었습니다.");
      } catch (IllegalArgumentException e) {
          log.error("회의 삭제 실패 - 찾을 수 없음: {}", e.getMessage());
          return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
      } catch (Exception e) {
          log.error("회의 삭제 중 서버 오류", e);
          return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 오류가 발생했습니다.");
      }
  }
	
	  // AI 요약 생성 요청
    // POST /api/meetings/summarize?meetingId={id}
    @PostMapping("/summarize")
    public ResponseEntity<?> generateSummary(@RequestParam("meetingId") Long meetingId) {
        try {
            log.info("AI 요약 생성 요청 - meetingId: {}", meetingId);
            
            // 1. 서비스가 반환한 결과 받기 (반환 타입 변경됨)
            MeetingResult result = meetingService.generateAISummary(meetingId);
            
            // 2. 프론트엔드가 기대하는 JSON 구조 생성 ("data.summary.purpose" 형태로 접근함)
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("purpose", result.getPurpose());
            summaryData.put("agenda", result.getAgenda());
            summaryData.put("overallSummary", result.getSummary()); // DB 컬럼명은 summary지만 프론트엔드 변수명 확인 필요
            summaryData.put("importance", result.getImportance());

            // 키워드 리스트 추출 (키워드 엔티티 -> 문자열 리스트)
            List<String> keywords = result.getKeywords().stream()
                .map(k -> k.getKeyword().getName())
                .collect(Collectors.toList());
            summaryData.put("keywords", keywords);

            // 3. 최종 응답 맵핑
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("summary", summaryData); // 여기가 중요! 프론트에서 data.summary 로 접근함
            
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("요약 생성 실패 (잘못된 요청): {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("요약 생성 중 서버 오류", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류가 발생했습니다: " + e.getMessage());
        }
    }
    
    // 전체 액션 아이템 생성 요청 (generate-all-actions 추가)
    @PostMapping("/generate-all-actions")
    public ResponseEntity<?> generateAllActions(
            @RequestParam("meetingId") Long meetingId, 
            @RequestBody Map<String, Object> requestData) { // DTO 대신 유연하게 Map으로 수신
        try {
            log.info("전체 액션 아이템 생성 요청 - meetingId: {}", meetingId);
            
            // 서비스 로직 호출 및 결과 반환
            Map<String, Object> result = meetingService.generateAllActions(meetingId, requestData);
            
            return ResponseEntity.ok(result); // { success: true, actions: [...] } 형태 반환
            
        } catch (Exception e) {
            log.error("액션 아이템 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }
}