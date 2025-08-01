package com.Catch_Course.domain.course.repository;

import com.Catch_Course.domain.course.entity.Course;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // 강사로 검색
    Page<Course> findAllByInstructor_Nickname(String keyword, Pageable pageable);
    // 내용으로 검색
    Page<Course> findAllByContentContaining(String keyword, Pageable pageable);
    // 제목으로 검색
    Page<Course> findAllByTitleContaining(String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)   // 비관적 Lock, FOR UPDATE 전부 차단
    @Query("select c from Course c where c.id= :id")
    Optional<Course> findByIdWithPessimisticLock(@Param("id") Long id);
}
