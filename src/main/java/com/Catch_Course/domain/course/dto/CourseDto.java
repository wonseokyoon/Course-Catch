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
    private long authorId;
    private String authorName;

    public CourseDto(Course course) {
        this.id = course.getId();
        this.createdDate = course.getCreatedDate();
        this.modifiedDate = course.getModifiedDate();
        this.title = course.getTitle();
        this.content = course.getContent();
        this.authorId = course.getAuthor().getId();
        this.authorName = course.getAuthor().getNickname();
    }
}
