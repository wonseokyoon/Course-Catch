package com.Catch_Course.domain.reservation.repository;

import com.Catch_Course.domain.reservation.entity.DeletedHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface DeleteHistoryRepository extends JpaRepository<DeletedHistory, Long> {

}

