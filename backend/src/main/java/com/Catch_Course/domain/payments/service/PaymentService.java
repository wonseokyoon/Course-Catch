package com.Catch_Course.domain.payments.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.repository.MemberRepository;
import com.Catch_Course.domain.payments.dto.PaymentDto;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.repository.PaymentRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;

    public PaymentDto getPayment(Member member, Long reservationId) {
        // reservation 이력 조회
        Reservation reservation = reservationRepository.findByIdAndStudent(reservationId,member)
                .orElseThrow(() -> new ServiceException("404-3","수강신청 이력이 없습니다."));

        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new ServiceException("404-5","결제 정보가 없습니다."));

        return new PaymentDto(payment);
    }
}
