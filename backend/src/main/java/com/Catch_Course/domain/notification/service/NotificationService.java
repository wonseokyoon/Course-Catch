package com.Catch_Course.domain.notification.service;

import com.Catch_Course.domain.notification.dto.NotificationDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private static final String KEY_PREFIX = "notifications:";
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    // 알림 저장
    public void saveNotification(Long memberId, Object data) {

        String key = KEY_PREFIX + memberId;

        try {
            // data -> Json 문자열
            String json = objectMapper.writeValueAsString(data);
            Map<String, String> streamMessage = Map.of("data", json);
            redisTemplate.opsForStream().add(key, streamMessage);
        } catch (Exception e) {
            log.error("Failed to serialize NotificationDto for memberId: {}", memberId, e);
        }
    }

    public List<Object> getNotifications(Long memberId) {

        String key = KEY_PREFIX + memberId;
        List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().range(key, Range.unbounded());

        if (records == null || records.isEmpty()) {
            return List.of();
        }

        return records.stream()
                .map(record -> {
                    Map<Object,Object> values = record.getValue();
                    String jsonData = (String) values.get("data");
                    try{
                        return objectMapper.readValue(jsonData, NotificationDto.class);
                    }catch (Exception e){
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


}
