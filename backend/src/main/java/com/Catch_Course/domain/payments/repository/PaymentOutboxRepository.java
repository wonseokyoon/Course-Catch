package com.Catch_Course.domain.payments.repository;

import com.Catch_Course.domain.payments.entity.PaymentOutbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutbox, Long> {
    List<PaymentOutbox> findTop100ByOrderByCreatedAtAsc();
}