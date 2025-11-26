package com.dialog.calendarevent.controller;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.dialog.calendarevent.domain.CalendarCreateRequest;
import com.dialog.calendarevent.domain.CalendarEventResponse;
import com.dialog.calendarevent.domain.EventCompletionRequest;
import com.dialog.calendarevent.domain.GoogleEventResponseDTO;
import com.dialog.calendarevent.service.CalendarEventService;
import com.dialog.token.service.SocialTokenService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarEventController {

	private final CalendarEventService calendarEventService;
	private final SocialTokenService tokenManagerService;

	@GetMapping("/calendar/events")
	public ResponseEntity<List<CalendarEventResponse>> getEvents(Principal principal, // ResponseEntity<?> ->
																						// ResponseEntity<List<CalendarEventResponse>>
			@RequestParam(name = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
			@RequestParam(name = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		List<CalendarEventResponse> events = calendarEventService.getEventsByDateRange(principal.getName(), startDate,
				endDate);
		return ResponseEntity.ok(events);
	}

	@PostMapping("/calendar/events")
	public ResponseEntity<GoogleEventResponseDTO> createEvent(Principal principal,
			@RequestBody @Valid CalendarCreateRequest request) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		if (request == null || request.getEventData() == null) {
			throw new IllegalArgumentException("이벤트 데이터가 비어있습니다.");
		}

		String userEmail = principal.getName();
		String provider = "google";

		// 서비스에서 토큰 관련 예외(GoogleOAuthException)를 던질 것으로 기대
		String accessToken = tokenManagerService.getToken(userEmail, provider);

		String calendarId = request.getCalendarId();
		if (calendarId == null || calendarId.isBlank()) {
			calendarId = "primary";
		}
		GoogleEventResponseDTO response = calendarEventService.createCalendarEvent(userEmail, provider, calendarId,
				accessToken, request.getEventData());
		return ResponseEntity.ok(response);
	}

	@PutMapping("/calendar/events/{id}")
	public ResponseEntity<GoogleEventResponseDTO> updateEvent(Principal principal, @PathVariable("id") String eventId,
			@RequestBody @Valid CalendarCreateRequest request) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String userEmail = principal.getName();
		String provider = "google";

		String calendarId = request.getCalendarId();
		if (calendarId == null || calendarId.isBlank()) {
			calendarId = "primary";
		}
		GoogleEventResponseDTO updatedEvent = calendarEventService.updateCalendarEvent(userEmail, provider, calendarId,
				eventId, request.getEventData());

		return ResponseEntity.ok(updatedEvent);
	}

	@DeleteMapping("/calendar/events/{id}")
	public ResponseEntity<Void> deleteEvent(Principal principal, @PathVariable("id") String eventId) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		calendarEventService.deleteCalendarEvent(principal.getName(), eventId);

		return ResponseEntity.ok().build();
	}

	// @PatchMapping("/calendar/{eventId}/importance")
	@PatchMapping("/calendar/events/{eventId}/importance")
	public ResponseEntity<Void> toggleImportance(@PathVariable("eventId") String eventId, Principal principal) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 현재 로그인한 사용자 이메일
		String userEmail = principal.getName();

		// [중요] 서비스 계층에도 userEmail과 eventId를 전달합니다.
		calendarEventService.toggleImportance(userEmail, eventId);

		return ResponseEntity.ok().build();
	}

	@PatchMapping("/calendar/events/{eventId}/completion")
	public ResponseEntity<Void> updateEventCompletion(@PathVariable("eventId") String eventId,
			@RequestBody EventCompletionRequest request, Principal principal) {

		if (principal == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		String userEmail = principal.getName();

		calendarEventService.updateCompletionStatus(userEmail, eventId, request.isCompleted());

		return ResponseEntity.ok().build();
	}

}
