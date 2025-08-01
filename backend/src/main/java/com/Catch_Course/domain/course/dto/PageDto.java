package com.Catch_Course.domain.course.dto;

import com.Catch_Course.domain.course.entity.Course;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageDto {

    List<CourseDto> items;

    int totalPages;     // 전체 페이지
    int totalItems;     // 전체 갯수
    int currentPage;    // 현재 페이지
    int pageSize;       // 페이지 사이즈


    public PageDto(Page<Course> coursePage) {

        this.items =coursePage.stream()
                .map(CourseDto::new)
                .toList();

        this.totalPages = coursePage.getTotalPages();
        this.totalItems = (int) coursePage.getTotalElements();
        this.currentPage = coursePage.getNumber() + 1;
        this.pageSize = coursePage.getSize();
    }
}
