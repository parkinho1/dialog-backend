package com.dialog.meeting.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.dialog.meeting.domain.Meeting;
import com.dialog.meeting.domain.Status;
import com.dialog.user.domain.MeetUser;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    void deleteByHostUser(MeetUser user);
  
    // 전체 회의수 반환
    long count(); 
  
    // 이번 달 회의 생성 개수
    @Query(value = "SELECT COUNT(*) FROM meeting WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    long countMeetingsInMonth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 오늘 생성한 회의 수
    @Query(value = "SELECT COUNT(*) FROM meeting WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    long countTodayCreatedMeetings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 어제 생성한 회의 수
    @Query(value = "SELECT COUNT(*) FROM meeting WHERE created_at BETWEEN :start AND :end", nativeQuery = true)
    long countYesterdayCreatedMeetings(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
  
    // 어드민 페이지 내부 현재 등록된 모든 회의 개수 조회
    List<Meeting> findAllByScheduledAtBetween(LocalDateTime startDateTime, LocalDateTime endDateTime);  
  
    // 특정 사용자의 회의 목록 조회 (예약 시간 내림차순 정렬)
    List<Meeting> findAllByHostUser_IdOrderByScheduledAtDesc(Long hostUserId);

    // 특정 유저의 기간 내 모든 회의 리스트 조회 (총 참여 시간 계산용)
    List<Meeting> findAllByHostUserAndScheduledAtBetween(MeetUser hostUser, LocalDateTime start, LocalDateTime end);

    // 특정 유저의 기간 내 회의 개수 조회 (이번 달 회의 카드용)
    long countByHostUserAndScheduledAtBetween(MeetUser hostUser, LocalDateTime start, LocalDateTime end);

    // 특정 유저의 기간 내 특정 상태(예: COMPLETED)인 회의 개수 조회 
    long countByHostUserAndStatusAndScheduledAtBetween(MeetUser hostUser, Status status, LocalDateTime start, LocalDateTime end);
  
}