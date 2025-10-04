package com.smartrent.infra.connector;

import com.smartrent.infra.connector.model.VNPayQueryRequest;
import com.smartrent.infra.connector.model.VNPayQueryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "vnpay", url = "${vnpay.query-url}")
public interface VNPayConnector {

    @PostMapping(value = "/querydr", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    VNPayQueryResponse queryTransaction(@RequestBody VNPayQueryRequest request);
}
