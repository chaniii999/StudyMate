package studyMate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import studyMate.config.OpenAiProperties;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimiterService {
    private final OpenAiProperties openAiProperties;
    private final ConcurrentLinkedQueue<LocalDateTime> requestTimes = new ConcurrentLinkedQueue<>();
    private final AtomicInteger currentMinuteRequests = new AtomicInteger(0);

    public boolean canMakeRequest() {
        LocalDateTime now = LocalDateTime.now();
        
        // Clean up old requests (older than 1 minute)
        while (!requestTimes.isEmpty()) {
            LocalDateTime oldestRequest = requestTimes.peek();
            if (oldestRequest != null && oldestRequest.isBefore(now.minusMinutes(1))) {
                requestTimes.poll();
                currentMinuteRequests.decrementAndGet();
            } else {
                break;
            }
        }

        // Check if we're under the rate limit
        int currentRequests = currentMinuteRequests.get();
        int maxRequests = openAiProperties.getRateLimit().getRequestsPerMinute();
        
        if (currentRequests < maxRequests) {
            requestTimes.offer(now);
            currentMinuteRequests.incrementAndGet();
            return true;
        }

        log.warn("Rate limit exceeded. Current requests in last minute: {}, Max allowed: {}", 
                currentRequests, maxRequests);
        return false;
    }

    public void waitForRateLimit() {
        if (!canMakeRequest()) {
            int delaySeconds = openAiProperties.getRateLimit().getRetryDelaySeconds();
            log.info("Rate limit reached. Waiting {} seconds before next request.", delaySeconds);
            try {
                Thread.sleep(delaySeconds * 1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Rate limit wait interrupted", e);
            }
        }
    }

    public int getCurrentRequestCount() {
        return currentMinuteRequests.get();
    }

    public int getMaxRequestsPerMinute() {
        return openAiProperties.getRateLimit().getRequestsPerMinute();
    }
} 