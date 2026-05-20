package com.secretlab.exercise.config.aspect;

import com.secretlab.exercise.common.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Global logging aspect for all controller methods.
 *
 * Execution order relative to GlobalExceptionHandler:
 *   request → LoggingAspect(@Around) → controller method → (exception re-thrown) → GlobalExceptionHandler
 *
 * The @Around advice wraps the controller invocation entirely, so any exception
 * is observed and logged here before it propagates to @RestControllerAdvice.
 */
@Aspect
@Component
@Slf4j
public class LoggingAspect {
    private static final int LOG_OBJECT_STRING_MAX_LEN = 500;
    
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    /**
     * Intercepts all public methods in the controller package.
     * Emits a single log line per request containing: HTTP method, URI, request time,
     * args, response/exception and elapsed ms.
     */
    @Around("execution(public * com.secretlab.exercise.controller.*.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpRequest = attrs != null ? attrs.getRequest() : null;

        String httpMethod = httpRequest != null ? httpRequest.getMethod() : "-";
        String uri = httpRequest != null ? httpRequest.getRequestURI() : pjp.getSignature().toShortString();
        String requestTime = FORMATTER.format(Instant.now());
        String args = JsonUtils.toJsonStringWithoutEx(pjp.getArgs(), LOG_OBJECT_STRING_MAX_LEN);
        long startMs = System.currentTimeMillis();

        try {
            Object result = pjp.proceed();
            log.info("[API_LOG] {} {} | time={} | elapsed={}ms | args={} | response={}", httpMethod, uri, requestTime,
                    System.currentTimeMillis() - startMs, args, JsonUtils.toJsonStringWithoutEx(result, LOG_OBJECT_STRING_MAX_LEN));
            return result;
        } catch (Throwable ex) {
            log.error("[API_LOG] {} {} | time={} | elapsed={}ms | args={} | exception={}",
                    httpMethod, uri, requestTime, System.currentTimeMillis() - startMs, args, ex.getMessage());
            throw ex;
        }
    }
}
