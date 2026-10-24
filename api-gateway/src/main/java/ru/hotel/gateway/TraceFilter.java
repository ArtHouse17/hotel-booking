package ru.hotel.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.slf4j.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

@Component
@Order(-200)
public class TraceFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(TraceFilter.class);
    private final ObjectMapper mapper;

    public TraceFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        String trace =
                incoming != null && incoming.matches("[a-zA-Z0-9._-]{1,80}")
                        ? incoming
                        : UUID.randomUUID().toString();
        exchange.getAttributes().put("traceId", trace);
        exchange.getResponse().getHeaders().set("X-Request-Id", trace);
        String path = exchange.getRequest().getPath().value();
        if (path.matches("/api/rooms/[^/]+/(confirm-availability|release)/?")) {
            return GatewayConfiguration.error(exchange, mapper, 404, "Route not found");
        }
        var forwarded =
                exchange.mutate()
                        .request(r -> r.headers(h -> h.set("X-Request-Id", trace)))
                        .build();
        return chain.filter(forwarded)
                .doFinally(
                        signal ->
                                log.info(
                                        "event=gateway_request traceId={} method={} path={} status={}",
                                        trace,
                                        exchange.getRequest().getMethod(),
                                        path,
                                        exchange.getResponse().getStatusCode()));
    }
}
