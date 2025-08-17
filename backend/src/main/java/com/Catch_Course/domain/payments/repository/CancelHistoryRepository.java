package com.Catch_Course.domain.payments.repository;

import com.Catch_Course.domain.payments.entity.CancelHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CancelHistoryRepository extends JpaRepository<CancelHistory, Long> {

}
