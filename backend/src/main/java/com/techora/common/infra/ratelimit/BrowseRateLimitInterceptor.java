package com.techora.common.infra.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techora.common.application.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.techora.common.application.constant.ResponseCode.RATE_LIMIT_EXCEEDED;

@Component
@RequiredArgsConstructor
public class BrowseRateLimitInterceptor implements HandlerInterceptor {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    private final BrowseRateLimiter browseRateLimiter;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {

        String clientKey = clientKey(request);
        if (browseRateLimiter.isAllowed(clientKey)) {
            return true;
        }

        response.setStatus(RATE_LIMIT_EXCEEDED.getStatus().value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(browseRateLimiter.retryAfterSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ErrorResponse.fromErrorCode(RATE_LIMIT_EXCEEDED));
        return false;
    }

    private String clientKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "user:" + authentication.getName();
        }
        return "ip:" + clientIp(request);
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader(X_REAL_IP);
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}
