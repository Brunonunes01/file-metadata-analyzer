package com.metascan.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.metascan.dto.ErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_MILLIS = 60_000L;

    private final ObjectMapper objectMapper;
    private final Map<String, Deque<Long>> requestLogByIp = new ConcurrentHashMap<>();

    public RateLimitFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) {
            return true;
        }
        return !path.startsWith("/metadata/") && !path.startsWith("/osint/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        long now = System.currentTimeMillis();

        Deque<Long> requestTimes = requestLogByIp.computeIfAbsent(clientIp, ignored -> new ArrayDeque<>());
        synchronized (requestTimes) {
            while (!requestTimes.isEmpty() && now - requestTimes.peekFirst() > WINDOW_MILLIS) {
                requestTimes.pollFirst();
            }

            if (requestTimes.size() >= MAX_REQUESTS_PER_MINUTE) {
                writeRateLimitResponse(response);
                return;
            }

            requestTimes.addLast(now);
        }

        filterChain.doFilter(request, response);
    }

    private void writeRateLimitResponse(HttpServletResponse response) throws IOException {
        int statusCode = HttpStatus.TOO_MANY_REQUESTS.value();
        response.setStatus(statusCode);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                statusCode,
                "Erro ao processar arquivo",
                "Limite de requisicoes excedido. Tente novamente em instantes.",
                Instant.now()
        );

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "unknown" : remoteAddr;
    }
}
