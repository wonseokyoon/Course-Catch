package com.Catch_Course.domain.course.service;

import com.Catch_Course.domain.course.controller.KeywordType;
import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.repository.CourseRepository;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.exception.ServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public Course write(Member member, String title, String content, long capacity) {

        return courseRepository.save(
                Course
                        .builder()
                        .instructor(member)
                        .title(title)
                        .content(content)
                        .capacity(capacity)
                        .currentRegistration(0)
                        .build()
        );
    }

    public Page<Course> getItems(int page, int pageSize, KeywordType keywordType, String keyword) {

        Pageable pageable = PageRequest.of(page - 1, pageSize);

        switch (keywordType) {
            case instructor -> {
                // 이름이 정확히 일치
                return courseRepository.findAllByInstructor_Nickname(keyword, pageable);
            }
            case content -> {
                return courseRepository.findAllByContentContaining(keyword, pageable);
            }
            default -> {    // 기본값: title
                return courseRepository.findAllByTitleContaining(keyword, pageable);
            }
        }
    }

    public Optional<Course> getItem(long id) {
        return courseRepository.findById(id);
    }

    public long count() {
        return courseRepository.count();
    }

    @Transactional
    public void delete(Course course) {
        courseRepository.delete(course);
    }

    @Transactional
    public void modify(Course course, String title, String content, long capacity) {
        course.setTitle(title);
        course.setContent(content);
        course.setCapacity(capacity);
    }

    public Course findById(long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ServiceException("404-1","존재하지 않는 강의입니다."));
    }
}
