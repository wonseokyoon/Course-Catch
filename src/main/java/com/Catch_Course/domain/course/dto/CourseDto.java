package com.Catch_Course.domain.course.dto;

import com.Catch_Course.domain.course.entity.Course;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CourseDto {

    private long id;
    @JsonProperty("createdDatetime")
    private LocalDateTime createdDate;
    @JsonProperty("modifiedDatetime")
    private LocalDateTime modifiedDate;
    private String title;
    private String content;
    private long instructorId;
    private String instructorName;
    private long capacity;

    public CourseDto(Course course) {
        this.id = course.getId();
        this.createdDate = course.getCreatedDate();
        this.modifiedDate = course.getModifiedDate();
        this.title = course.getTitle();
        this.content = course.getContent();
        this.instructorId = course.getInstructor().getId();
        this.instructorName = course.getInstructor().getNickname();
        this.capacity = course.getCapacity();
    }
}
