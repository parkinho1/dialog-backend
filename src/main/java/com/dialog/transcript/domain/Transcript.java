package com.dialog.transcript.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.dialog.meeting.domain.Meeting;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transcript")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class Transcript {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 회의의 발화인지 (FK) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    /** STT 원본 화자 ID (예: Speaker 1, spk-1) */
    @Column(name = "speaker_id", length = 50, nullable = false)
    private String speakerId;

    /** 사용자가 매핑한 최종 화자명 (예: 가나디) */
    @Column(name = "speaker_name", length = 100)
    private String speakerName;

    /** CLOVA STT의 speaker label (정수값) */
    @Column(name = "speaker_label")
    private Integer speakerLabel;

    /** 실제 발화 내용 */
    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String text;

    /** 시작 시간 (밀리초) */
    @Column(name = "start_time", nullable = false)
    private Long startTime;

    /** 종료 시간 (밀리초) */
    @Column(name = "end_time", nullable = false)
    private Long endTime;

    /** 발화 순서 */
    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    /** 삭제(숨김) 여부 : 프론트의 delete/undo 반영 */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private boolean isDeleted = false;

    /** 생성 시간 */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 수정 시간 */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /* ==============================
           비즈니스 로직
    ============================== */

    /** 발화 텍스트 수정 */
    public void updateText(String newText) {
        this.text = newText;
    }

    /** 발화자 변경 */
    public void updateSpeaker(String newSpeakerId, String newSpeakerName) {
        this.speakerId = newSpeakerId;
        this.speakerName = newSpeakerName;
    }
    
    public void updateSequenceOrder(Integer sequenceOrder) {
        this.sequenceOrder = sequenceOrder;
    }

    /** 삭제 */
    public void delete() {
        this.isDeleted = true;
    }

    /** 복구 */
    public void restore() {
        this.isDeleted = false;
    }

    /** 프론트 표시용 시간 라벨 생성 (00:01:30 형식) */
    public String getTimeLabel() {
        long seconds = this.startTime / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, secs);
        } else {
            return String.format("%02d:%02d", minutes, secs);
        }
    }
}