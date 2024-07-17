package org.example.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingInterceptor extends HandlerInterceptorAdapter {

    private static final int MAX_REQUESTS = 5;
    private static final long TIME_INTERVAL_MS = 60000;

    private final Map<String, Queue<Long>> requestMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String clientId = getClientId(request);

        if (!requestMap.containsKey(clientId)) {
            requestMap.put(clientId, new LinkedList<>());
        }

        Queue<Long> requests = requestMap.get(clientId);
        long now = System.currentTimeMillis();

        while (!requests.isEmpty() && now - requests.peek() > TIME_INTERVAL_MS) {
            requests.poll();
        }

        if (requests.size() >= MAX_REQUESTS) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            return false;
        }

        requests.offer(now);
        return true;
    }

    private String getClientId(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
