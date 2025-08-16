package com.Catch_Course.domain.payments.repository;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.payments.entity.PaymentStatus;
import com.Catch_Course.domain.reservation.entity.Reservation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository  extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservation(Reservation reservation);

    Optional<Payment> findByMerchantUid(String orderId);

    List<Payment> findByMemberAndStatus(Member member, PaymentStatus paymentStatus);

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // 비관적 Lock, FOR UPDATE 전부 차단
    @Query("select r from Reservation r where r.id= :id")
    Optional<Payment> findByIdWithPessimisticLock(@Param("id") Long id);
}
