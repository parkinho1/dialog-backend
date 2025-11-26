package com.dialog.meeting.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.dialog.actionitem.domain.ActionItem;
import com.dialog.meeting.domain.Meeting;
import com.dialog.meetingresult.domain.MeetingResult;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MeetingCreateResponseDto {

    private Long meetingId;   // 새로 생성된 회의 ID
    private String title;     // 생성된 회의 제목
    private Status status;    // 현재 상태 (예: SCHEDULED)
    private LocalDateTime scheduledAt; // 예약 시간
    private List<String> participants; // 참가자 이름 리스트
    private List<KeywordDto> keywords; // 키워드 리스트
    private String authorName;         // 작성자 이름 관련 변수 추가
    private String purpose;
    private String agenda;
    private String summary;
    private ImportanceData importance;
    private List<ActionItemDto> actionItems;
    
	  // --- 내부 DTO 클래스들 ---
    @Getter
    @Setter
    @NoArgsConstructor
    public static class KeywordDto {
        private String text;
        private String source;
        public KeywordDto(String text, String source) {
            this.text = text;
            this.source = source;
        }
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ImportanceData {
        private String level;
        private String reason;
        public ImportanceData(String level, String reason) {
            this.level = level;
            this.reason = reason;
        }
    }
    
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ActionItemDto {
        private String task;
        private String assignee;
        private LocalDateTime dueDate;
        private String source;
        private boolean isCompleted;

        public ActionItemDto(ActionItem item) {
            this.task = item.getTask();
            this.assignee = (item.getAssignee() != null) ? item.getAssignee().getName() : null;
            this.dueDate = item.getDueDate();
            this.source = item.getSource();
            this.isCompleted = item.isCompleted();
        }
    }

    public MeetingCreateResponseDto(Meeting meeting) {
        this.meetingId = meeting.getId();
        this.title = meeting.getTitle();
        this.status = meeting.getStatus();
        this.scheduledAt = meeting.getScheduledAt();
        
        // 참가자 Null 방지
        if (meeting.getParticipants() != null) {
            this.participants = meeting.getParticipants().stream()
                    .map(p -> p.getName()) 
                    .collect(Collectors.toList());
        } else {
            this.participants = new ArrayList<>();
        }
        
        if (meeting.getHostUser() != null) {
            this.authorName = meeting.getHostUser().getName();
        }

        // MeetingResult 데이터 매핑
        MeetingResult result = meeting.getMeetingResult();
        
        // 1. AI 결과가 있을 때 (Completed 상태 등)
        if (result != null) {
            this.purpose = result.getPurpose();
            this.agenda = result.getAgenda();
            this.summary = result.getSummary();
            
            if (result.getImportance() != null) {
                this.importance = new ImportanceData(
                        result.getImportance().name(), 
                        result.getImportanceReason()
                ); 
            } else {
                this.importance = new ImportanceData("MEDIUM", "");
            }

            // 키워드 리스트가 null일 경우 에러 방지
            if (result.getKeywords() != null) {
                this.keywords = result.getKeywords().stream()
                        .map(mrk -> new KeywordDto(
                            mrk.getKeyword().getName(), 
                            (mrk.getSource() != null) ? mrk.getSource().name() : "AI"
                        ))
                        .collect(Collectors.toList());
            } else {
                this.keywords = new ArrayList<>();
            }

            // 액션 아이템 리스트가 null일 경우 에러 방지
            if (result.getActionItems() != null) {
                this.actionItems = result.getActionItems().stream()
                        .map(ActionItemDto::new)
                        .collect(Collectors.toList());
            } else {
                this.actionItems = new ArrayList<>();
            }
        } else {
            // 2. AI 결과가 없을 때 (기본값 or 하이라이트 키워드 사용)
            this.purpose = "";
            this.agenda = "";
            this.summary = "";
            this.importance = new ImportanceData("MEDIUM", "");
            
            this.keywords = new ArrayList<>();
            // DB 문자열("키워드1,키워드2") -> 리스트 변환 로직
            if (meeting.getHighlightKeywords() != null && !meeting.getHighlightKeywords().isEmpty()) { 
                for(String s : meeting.getHighlightKeywords().split(",")) {
                    this.keywords.add(new KeywordDto(s.trim(), "USER")); 
                }
            }
            
            this.actionItems = new ArrayList<>();
        }
    }
}