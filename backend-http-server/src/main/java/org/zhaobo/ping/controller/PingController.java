package org.zhaobo.ping.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zhaobo.client.core.ApiInvoker;
import org.zhaobo.client.core.ApiProperties;
import org.zhaobo.client.core.ApiProtocol;
import org.zhaobo.client.core.ApiService;

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
//        throw new RuntimeException();
        return "pong";
    }
}
