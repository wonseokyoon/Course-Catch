package com.Catch_Course.domain.course.repository;

import com.Catch_Course.domain.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
    // 강사로 검색
    Page<Course> findAllByInstructor_Nickname(String keyword, Pageable pageable);
    // 내용으로 검색
    Page<Course> findAllByContentContaining(String keyword, Pageable pageable);
    // 제목으로 검색
    Page<Course> findAllByTitleContaining(String keyword, Pageable pageable);
}
