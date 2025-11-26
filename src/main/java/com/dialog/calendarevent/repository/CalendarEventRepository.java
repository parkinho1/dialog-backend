package com.dialog.calendarevent.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.calendarevent.domain.CalendarEvent;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

	// 기간별 조회
	List<CalendarEvent> findByUserIdAndEventDateBetween(Long userId, LocalDate startDate, LocalDate endDate);

	// 구글 이벤트 ID로 조회
	Optional<CalendarEvent> findByGoogleEventIdAndUserId(String eventId, Long id);

	// 회의 삭제 시 캘린더 연동 삭제를 위한 메서드 2개
	boolean existsByMeetingId(Long meetingId);

	void deleteByMeetingId(Long meetingId);
}