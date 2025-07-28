package com.Catch_Course.domain.course.repository;

import com.Catch_Course.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
