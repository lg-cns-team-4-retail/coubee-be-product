package com.coubee.coubeebeproduct.common.type;

import lombok.Getter;
import lombok.Setter;

/// 카프카 이벤트 발행에 쓰일 클래스
@Getter
@Setter
public class ActionAndId {
    private String action;
    private Long id;

    /*
    * "entity": "CoubeeUser",
  "timestamp": "2025-07-18T09:40:00Z",
  "service": "coubee-be-user",
  "actor": "admin@coubee.com"*/

    public static ActionAndId of(String action, Long id) {
        ActionAndId actionAndId = new ActionAndId();
        actionAndId.action = action;
        actionAndId.id = id;
        return actionAndId;
    }
}
