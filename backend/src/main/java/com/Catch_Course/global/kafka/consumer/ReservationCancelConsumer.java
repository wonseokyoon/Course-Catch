package com.Catch_Course.global.kafka.consumer;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.reservation.entity.DeletedHistory;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.DeleteHistoryRepository;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.dto.ReservationCancelRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationCancelConsumer {

    private final DeleteHistoryRepository deleteHistoryRepository;
    private final ReservationRepository reservationRepository;
    private final CourseRepository courseRepository;

    // 구독
    @KafkaListener(topics = "course-reservation-deleted", groupId = "course")
    @Transactional
    public void consume(ReservationCancelRequest reservationCancelRequest) {

        log.info("수강 취소 처리 시작: {}", reservationCancelRequest);
        try {
            // 실제 로직 호출
            Reservation reservation = reservationRepository.findById(reservationCancelRequest.getReservationId())
                    .orElseThrow(() -> new ServiceException("404-3", "수강 신청 이력이 없습니다."));

            cancelProcess(reservation, reservation.getCourse());
            saveDeleteHistory(reservationCancelRequest.getMemberId(), reservationCancelRequest.getCourseId());
            log.info("{}의 수강 취소 이력이 성공적으로 처리되었습니다.", reservationCancelRequest.getCourseId());
        } catch (Exception e){
            log.error("Error Request: {}", reservationCancelRequest, e);
        }
    }

    private void cancelProcess(Reservation reservation, Course course) {
        // 현재 인원 감소
        course.decreaseReservation();
        courseRepository.save(course);

        // 상태 변경
        reservation.setStatus(ReservationStatus.CANCELLED);
    }

    public void saveDeleteHistory(Long memberId, Long courseId) {
        // 기록으로 남김
        deleteHistoryRepository.save(DeletedHistory.builder()
                .memberId(memberId)
                .courseId(courseId)
                .build()
        );
    }
}