package org.zhaobo.core.filter.monitor;

import com.sun.net.httpserver.HttpServer;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.zhaobo.core.ConfigLoader;
import org.zhaobo.core.context.GatewayContext;
import org.zhaobo.core.filter.Filter;
import org.zhaobo.core.filter.FilterInfo;
import org.zhaobo.common.constants.FilterConst;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * @Auther: bo
 * @Date: 2023/12/9 14:53
 * @Description:
 */
@Slf4j
@FilterInfo(id = FilterConst.MONITOR_END_FILTER_ID,
        name = FilterConst.MONITOR_END_FILTER_NAME,
        order = FilterConst.MONITOR_END_FILTER_ORDER)
public class MonitorEndFilter implements Filter {

    // 普罗米修斯的注册表
    private final PrometheusMeterRegistry registry;

    public MonitorEndFilter() {
        this.registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        // 暴露一个端口提供给普罗米修斯主动访问拉取指标数据
        int prometheusPort = ConfigLoader.getInstance().getConfig().getPrometheusPort();
        // 构建一个简单的http服务器
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(prometheusPort), 0);
            server.createContext("/prometheus", exchange -> {
                // 获取指标数据的文本内容
                String scraped = registry.scrape();

                // 指标数据写回
                exchange.sendResponseHeaders(200, scraped.getBytes().length);
                try(OutputStream os = exchange.getResponseBody()) {
                    os.write(scraped.getBytes());
                }
            });

            new Thread(server::start).start();

        } catch (IOException e){
            log.error("Prometheus http server start error: {}",e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("Prometheus http server started! port:{}", prometheusPort);
    }

    @Override
    public void doFilter(GatewayContext ctx) throws Exception {
        Timer timer = registry.timer("gateway-request",
                "uniqueId", ctx.getUniqueId(),
                "protocol", ctx.getProtocol(),
                "path", ctx.getRequest().getPath());
        ctx.getTimerSample().stop(timer);
    }
}
