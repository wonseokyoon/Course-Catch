package com.Catch_Course.domain.reservation.service;

import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.domain.notification.dto.NotificationDto;
import com.Catch_Course.domain.notification.service.NotificationService;
import com.Catch_Course.domain.payments.service.PaymentService;
import com.Catch_Course.domain.reservation.dto.ReservationDto;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.kafka.dto.ReservationCancelRequest;
import com.Catch_Course.global.kafka.dto.ReservationRequest;
import com.Catch_Course.global.kafka.producer.RedisExpirationProducer;
import com.Catch_Course.global.kafka.producer.ReservationCancelProducer;
import com.Catch_Course.global.kafka.producer.ReservationProducer;
import com.Catch_Course.global.sse.service.SseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ReservationService {

    private final CourseRepository courseRepository;
    private final ReservationRepository reservationRepository;
    private final MemberRepository memberRepository;
    private final ReservationProducer reservationProducer;
    private final ReservationCancelProducer reservationCancelProducer;
    private final NotificationService notificationService;
    private final SseService sseService;
    private final PaymentService paymentService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisExpirationProducer redisExpirationProducer;
    private final int TIME_LIMIT = 1;

    public Reservation addToQueue(Member member, Long courseId) {
        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 비관적 Lock 을 걸고 조회
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Optional<Reservation> optionalReservation = reservationRepository.findByStudentAndCourse(member, course);
        if (optionalReservation.isPresent()) {
            handleDuplicateReservation(optionalReservation.get(), member, courseId);
            return optionalReservation.get();
        }

        // 대기열 등록
        Reservation reservation = Reservation.builder()
                .student(member)
                .course(course)
                .status(ReservationStatus.WAITING)
                .price(course.getPrice())
                .build();

        // 메세지 전송
        reservationProducer.send(String.valueOf(courseId), new ReservationRequest(member.getId(), courseId));
        return reservationRepository.save(reservation);
    }

    private void handleDuplicateReservation(Reservation reservation, Member member, Long courseId) {
        ReservationStatus status = reservation.getStatus();

        if (status.equals(ReservationStatus.COMPLETED) || status.equals(ReservationStatus.PENDING)) {
            throw new ServiceException("409-1", "이미 신청한 강의입니다.");
        } else if (status.equals(ReservationStatus.WAITING)) {
            throw new ServiceException("409-3", "이미 대기열에 등록된 신청입니다.");
        } else {
            // 대기열 등록
            reservation.setStatus(ReservationStatus.WAITING);
            reservationProducer.send(String.valueOf(courseId), new ReservationRequest(member.getId(), courseId));
            reservationRepository.save(reservation);
        }
    }

    public ReservationDto cancelReserveRequest(Member member, Long courseId) {

        Course course = courseRepository.findByIdWithPessimisticLock(courseId)  // 잠금
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                .orElseThrow(() -> new ServiceException("404-3", "수강 신청 이력이 없습니다."));

        // 결제 취소
        if (reservation.getStatus().equals(ReservationStatus.COMPLETED)) {
            log.info("결제 완료 건에 대한 취소 요청 접수. paymentId: {}", reservation.getPayment().getId());
            paymentService.deletePaymentRequest(reservation.getStudent(), reservation.getId());
        }else if (reservation.getStatus().equals(ReservationStatus.CANCELLED)) {
            throw new ServiceException("409-5", "이미 취소된 신청입니다.");
        } else if (reservation.getStatus().equals(ReservationStatus.PENDING)) {
            // 수강 취소는 메세지를 남겨서 저장
            reservationCancelProducer.send(new ReservationCancelRequest(reservation.getId(), member.getId(), courseId));
        } else {
            throw new ServiceException("409-6", "취소할 수 없는 신청입니다.");
        }

        return new ReservationDto(reservation);
    }

    @Transactional(readOnly = true)
    public Page<Reservation> getReservations(Member member, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<Reservation> reservations = reservationRepository.findAllByStudentAndStatus(member, ReservationStatus.COMPLETED, pageable);

        if (reservations.isEmpty()) {
            throw new ServiceException("404-3", "수강신청 이력이 없습니다.");
        }

        return reservations;
    }

    @Transactional(readOnly = true)
    public Page<Reservation> getReservationsPending(Member member, int page, int pageSize) {
        Pageable pageable = PageRequest.of(page - 1, pageSize);

        Page<Reservation> reservations = reservationRepository.findAllByStudentAndStatus(member, ReservationStatus.PENDING, pageable);

        if (reservations.isEmpty()) {
            throw new ServiceException("404-3", "수강신청 이력이 없습니다.");
        }

        return reservations;
    }

    /**
     * Kafka Consumer에 의해 호출 될 실제 수강 신청 처리 메서드
     */
    public void processReservation(Long courseId, Long memberId) {
        try {
            // 락을 걸어 강의 정보 조회(동시성 제어)
            Course course = courseRepository.findByIdWithPessimisticLock(courseId)
                    .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new ServiceException("404-4", "회원을 찾을 수 없습니다."));

            Reservation reservation = reservationRepository.findByStudentAndCourse(member, course)
                    .orElseThrow(() -> new ServiceException("404-3", "수강신청 이력이 없습니다."));

            if (course.isFull()) {
                reservation.setStatus(ReservationStatus.FAILED);
                reservationRepository.save(reservation);
                NotificationDto notificationDto = new NotificationDto(reservation, "수강 신청 실패: 정원이 마감되었습니다.");
                notificationService.saveNotification(memberId, notificationDto);
                sseService.sendToClient(memberId, "ReservationResult", notificationDto);
                return;
            }

            // 수강 신청 처리
            course.increaseReservation();
            courseRepository.save(course);

            reservation.setStatus(ReservationStatus.PENDING);

            // Redis에 만료 타이머 등록
            String expirationKey = "reservation:expire:" + reservation.getId();
            redisTemplate.opsForValue().set(expirationKey, "", TIME_LIMIT, TimeUnit.MINUTES);
            log.info("Redis 만료 타이머 등록. Key: {}, 만료 시간: {} 분", expirationKey, TIME_LIMIT);

            NotificationDto notificationDto = new NotificationDto(reservation, "수강 신청이 성공하였습니다.");
            sseService.sendToClient(memberId, "ReservationResult", notificationDto);

            notificationService.saveNotification(memberId, notificationDto);
            reservationRepository.save(reservation);
        } catch (ServiceException e) {
            NotificationDto notificationDto = new NotificationDto(ReservationStatus.FAILED, "수강 신청 처리 중 오류가 발생했습니다. " + e.getMessage());
            sseService.sendToClient(memberId, "ReservationResult", notificationDto);
        }
    }

    public void expireReservation(Long reservationId) {
        log.info("만료 처리 서비스 시작. 예약 ID: {}", reservationId);
        try {
            Optional<Reservation> reservation = reservationRepository.findByIdWithPessimisticLock(reservationId);

            if(reservation.isPresent()) {
                Long memberId = reservation.get().getStudent().getId();
                Long courseId = reservation.get().getCourse().getId();
                redisExpirationProducer.send(new ReservationCancelRequest(reservationId, memberId, courseId));
                log.info("만료 처리 Kafka 메시지 전송 성공. 예약 ID: {}", reservationId);
            }
            log.warn("만료 처리 시점에 예약을 찾을 수 없음. ID: {}", reservationId);

        } catch (Exception e) {
            log.error("만료 처리 중 예외 발생. 예약 ID: {}", reservationId, e);
        }
    }

}
