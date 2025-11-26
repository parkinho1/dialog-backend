package com.dialog.user.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomeStatsDto {
    // 이번 달 회의
    private long thisMonthMeetingCount;
    // 예: "+12%" 또는 "-2"
    private String meetingCountDiff; 

    // 총 참여 시간 (시간 단위)
    private String totalMeetingTime;
    private String meetingHoursDiff;

    // 미결 액션아이템 (Todo 중 완료되지 않은 것)
    private long openActionItems;
    // 전월 대비 혹은 어제 대비
    private String actionItemsDiff;

    // 종료된 회의 수
    private long confirmedMeetings;
    private String meetingsDiff;
}
