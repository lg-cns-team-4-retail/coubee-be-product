package com.coubee.coubeebeproduct.common.type;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ActionLog {
    private String service = "coubee-be-user";      // 예: "coubee-be-user"
    private String actor;        // 사용자명 "username"
    private String action;       // 예: "Create", "Update" ,"READ"
    private String entity;       // 예: "CoubeeUser"
    private Long entityId;       // 해당 엔티티 ID
    private String result;       // 예: "SUCCESS", "FAIL"
    private LocalDateTime timestamp;
    private Long elapsedMillis;  // 처리 시간(ms)
    private String detail;       // 추가 메시지 (선택)
}