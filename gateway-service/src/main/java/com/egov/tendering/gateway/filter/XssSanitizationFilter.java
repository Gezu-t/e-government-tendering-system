package com.egov.tendering.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Global gateway filter that sanitizes JSON request bodies to remove XSS payloads.
 *
 * <p>Applies to POST, PUT, and PATCH requests with {@code Content-Type: application/json}.
 * The sanitized body replaces the original in the forwarded request, and the
 * {@code Content-Length} header is updated to match the new byte length.
 *
 * <p>Runs at order {@code -2}, before the {@link RequestLoggingFilter} at order {@code -1}.
 */
@Component
@Slf4j
public class XssSanitizationFilter implements GlobalFilter, Ordered {

    private static final Set<HttpMethod> BODY_METHODS =
            Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        MediaType contentType = request.getHeaders().getContentType();

        if (!BODY_METHODS.contains(method)
                || contentType == null
                || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return chain.filter(exchange);
        }

        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    String original = new String(bytes, StandardCharsets.UTF_8);
                    String sanitized = XssUtils.sanitize(original);

                    if (!sanitized.equals(original)) {
                        log.warn("XSS payload detected and stripped on {} {}", method, request.getPath());
                    }

                    byte[] sanitizedBytes = sanitized.getBytes(StandardCharsets.UTF_8);
                    DataBuffer sanitizedBuffer =
                            exchange.getResponse().bufferFactory().wrap(sanitizedBytes);

                    ServerHttpRequest mutatedRequest = request.mutate()
                            .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(sanitizedBytes.length))
                            .build();

                    ServerHttpRequestDecorator decorated = new ServerHttpRequestDecorator(mutatedRequest) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            return Flux.just(sanitizedBuffer);
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
