package org.zhaobo.ping.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhaobo.client.core.ApiInvoker;
import org.zhaobo.client.core.ApiProperties;
import org.zhaobo.client.core.ApiProtocol;
import org.zhaobo.client.core.ApiService;

import java.nio.ByteBuffer;

@Slf4j
@RestController
@ApiService(serviceId = "backend-http-server", protocol = ApiProtocol.HTTP, patternPath = "/http-server/**")
public class PingController {

    @Autowired
    private ApiProperties apiProperties;

    @ApiInvoker(path = "/http-server/ping")
    @GetMapping("/http-server/ping")
    public String ping() throws InterruptedException {
        log.info("{}", apiProperties);
//        Thread.sleep(5100);
//        Thread.sleep(3000);
        Thread.sleep(20);
        StringBuilder response = new StringBuilder(2048);
        for (int i = 0; i < 2048; i++) {
            response.append("a");
        }
        log.info("pong");
        return response.toString();
//        return "pong";
    }
}
