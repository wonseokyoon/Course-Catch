package com.Catch_Course.global.kafka.consumer;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.kafka.dto.ReservationCancelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisExpirationConsumer {

    private final ReservationRepository reservationRepository;
    private final CourseRepository courseRepository;

    // 구독
    @KafkaListener(topics = "course-reservation-expired", groupId = "course")
    @Transactional
    public void consume(ReservationCancelRequest reservationCancelRequest) {

        log.info("결제 기한 만료 처리 시작: {}", reservationCancelRequest);
        try {
            // 실제 로직 호출
            Optional<Reservation> optionalReservation = reservationRepository.findByIdWithPessimisticLock(reservationCancelRequest.getReservationId());

            if (optionalReservation.isEmpty()) {
                log.warn("취소할 예약을 찾을 수 없습니다. ID: {}", reservationCancelRequest.getReservationId());
                return;
            }

            Reservation reservation = optionalReservation.get();
            if (!reservation.getStatus().equals(ReservationStatus.PENDING)) {
                log.info("예약 ID {}는 PENDING 상태가 아니므로 취소하지 않습니다. 현재 상태: {}", reservation.getId(), reservation.getStatus());
                return;
            }

            cancelProcess(reservation, reservation.getCourse());
            log.info("{}의 수강 취소가 성공적으로 처리되었습니다.", reservationCancelRequest.getCourseId());
        } catch (Exception e) {
            log.error("Error Request: {}", reservationCancelRequest, e);
        }
    }

    private void cancelProcess(Reservation reservation, Course course) {
        // 현재 인원 감소
        course.decreaseReservation();
        courseRepository.save(course);

        // 상태 변경
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }
}
