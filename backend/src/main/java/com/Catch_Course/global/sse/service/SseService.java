package com.Catch_Course.global.sse.service;

import com.Catch_Course.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseService {
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final NotificationService notificationService;
    private static final String NOTIFICATION_STREAM_PREFIX = "notification_stream:";

    public SseEmitter subscribe(Long memberId) {

        SseEmitter emitter = new SseEmitter(60 * 10 * 1000L);    // 10분
        this.emitters.put(memberId, emitter);

        // Emitter 가 완료되거나 타임아웃되면 맵에서 제거
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for memberId: {}", memberId);
            this.emitters.remove(memberId);
        });
        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for memberId: {}", memberId);
            this.emitters.remove(memberId);
        });
        emitter.onError((e) -> {
            log.error("SSE connection error for memberId: {}", memberId, e);
            this.emitters.remove(memberId);
        });

        // 연결이 생성되었을 때, 503 Service Unavailable 방지를 위해 더미 데이터 전송
        sendDummyEvent(memberId, "connected", "SSE connection established.");

        return emitter;
    }

    // 이벤트 전송
    public void sendToClient(Long memberId, String eventName, Object data) {
        // 메세지 저장
        notificationService.saveNotification(memberId, data);

        // 채널이 열려있다면, 이벤트 전송
        SseEmitter emitter = this.emitters.get(memberId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName) // 클라이언트에서 이 이름으로 이벤트를 수신
                        .data(data));
            } catch (IOException e) {
                log.error("Failed to send SSE event to memberId: {}", memberId, e);
                this.emitters.remove(memberId);
            }
        } else {
            log.warn("No SseEmitter found for memberId: {}", memberId);
        }
    }

    // 실제 저장하지 않는 더미 이벤트
    private void sendDummyEvent(Long memberId, String eventName, Object data) {
        SseEmitter emitter = this.emitters.get(memberId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.error("Failed to send dummy event to memberId: {}", memberId, e);
                this.emitters.remove(memberId);
            }
        }
    }
}
