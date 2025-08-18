package com.coubee.coubeebeproduct.remote.store;

import com.coubee.coubeebeproduct.common.dto.ApiResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "coubee-be-store", path = "/backend/store")
//@FeignClient(
//        name = "store-service",
//        url = "http://coubee-be-store-service.default.svc.cluster.local:8080",
//        path = "/backend/store"
//)
public interface RemoteStoreService {
    @GetMapping(value = "/near")
    public ApiResponseDto<List<Long>> getNearStoreIds(@RequestParam double latitude, @RequestParam double longitude);
}
