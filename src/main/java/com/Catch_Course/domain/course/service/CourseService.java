package com.Catch_Course.domain.course.service;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    public Course write(Member author, String title, String content) {

        return courseRepository.save(
                Course
                        .builder()
                        .author(author)
                        .title(title)
                        .content(content)
                        .build()
        );
    }

    public List<Course> getItems() {
        return courseRepository.findAll();
    }

    public Optional<Course> getItem(long id) {
        return courseRepository.findById(id);
    }

    public long count() {
        return courseRepository.count();
    }

    public void delete(Course course) {
        courseRepository.delete(course);
    }

    @Transactional
    public void modify(Course course, String title, String content) {
        course.setTitle(title);
        course.setContent(content);
    }

    public void flush() {
        courseRepository.flush();
    }
}
