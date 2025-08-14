package com.Catch_Course.domain.payments.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.dto.PaymentDto;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.entity.PaymentStatus;
import com.Catch_Course.domain.payments.repository.PaymentRepository;
import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import com.Catch_Course.domain.reservation.repository.ReservationRepository;
import com.Catch_Course.domain.reservation.service.ReservationService;
import com.Catch_Course.global.exception.ServiceException;
import com.Catch_Course.global.payment.TossPaymentsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationService reservationService;
    private final TossPaymentsService tossPaymentsService;

    public PaymentDto getPayment(Member member, Long reservationId) {
        // reservation 이력 조회
        Reservation reservation = reservationRepository.findByIdAndStudent(reservationId,member)
                .orElseThrow(() -> new ServiceException("404-3","수강신청 이력이 없습니다."));

        Payment payment = paymentRepository.findByReservation(reservation)
                .orElseThrow(() -> new ServiceException("404-5","결제 정보가 없습니다."));

        return new PaymentDto(payment);
    }

    public List<PaymentDto> getPayments(Member member) {
        List<Reservation> reservationList = reservationRepository.findByStudent(member);

        List<Payment> payments = paymentRepository.findByReservationIn(reservationList);

        if(payments.isEmpty()) {
            throw new ServiceException("404-5","결제 정보가 없습니다.");
        }

        return payments.stream()
                .map(PaymentDto::new)
                .toList();
    }

    @Transactional
    public PaymentDto requestPayment(Member member, Long reservationId) {
        Reservation reservation = reservationService.findByIdAndStudent(reservationId,member);

        String merchantUid = UUID.randomUUID().toString();
        long amount = reservation.getPrice();

        Payment payment = Payment.builder()
                .reservation(reservation)
                .member(member)
                .merchantUid(merchantUid)
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        return new PaymentDto(savedPayment);
    }

    @Transactional
    public PaymentDto confirmPayment(String paymentKey, String orderId, Long amount) {
        Payment payment = paymentRepository.findByMerchantUid(orderId)
                .orElseThrow(() -> new ServiceException("404-5","결제 정보가 없습니다."));

        if(payment.getStatus() != PaymentStatus.PENDING) {
            throw new ServiceException("409-2","이미 처리된 결제입니다.");
        }
        if(payment.getAmount() != amount){
            throw new ServiceException("400-4","결제 금액이 일치하지 않습니다.");
        }

        tossPaymentsService.confirm(paymentKey,orderId,amount);

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaymentKey(paymentKey);
        payment.getReservation().setStatus(ReservationStatus.COMPLETED);

        return new PaymentDto(paymentRepository.save(payment));
    }
}
