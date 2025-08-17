package com.Catch_Course.global.redis;

import com.Catch_Course.domain.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisExpirationListener implements MessageListener {

    private final ReservationService reservationService;

    @Override
    public void onMessage(Message message, byte[] pattern) {

        String expiredKey = message.toString();
        log.info("Redis Key 만료 감지: {}", expiredKey);

        if(expiredKey.startsWith("reservation:expire:")) {
            String[] params = expiredKey.split(":");
            if(params.length == 3) {
                try {
                    Long reservationId = Long.parseLong(params[2]);
                    reservationService.expireReservation(reservationId);
                } catch (NumberFormatException e){
                    log.error("만료된 Redis 키에서 예약 ID를 파싱하는 데 실패했습니다: {}", expiredKey, e);
                }
            }
        }
    }
}
