package com.Catch_Course.domain.payments.repository;

import com.Catch_Course.domain.payments.entity.Payment;
import com.Catch_Course.domain.reservation.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository  extends JpaRepository<Payment, Long> {
    Optional<Payment> findByReservation(Reservation reservation);

    List<Payment> findByReservationIn(List<Reservation> reservationList);

    Optional<Payment> findByMerchantUid(String orderId);
}
