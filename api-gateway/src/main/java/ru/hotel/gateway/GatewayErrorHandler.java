package ru.hotel.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

@Component
@Order(-2)
public class GatewayErrorHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper mapper;

    public GatewayErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable error) {
        if (exchange.getResponse().isCommitted()) return Mono.error(error);
        int status =
                error instanceof ErrorResponse webError ? webError.getStatusCode().value() : 503;
        var resolved = org.springframework.http.HttpStatus.resolve(status);
        return GatewayConfiguration.error(
                exchange,
                mapper,
                status,
                resolved == null ? "Gateway error" : resolved.getReasonPhrase());
    }
}
