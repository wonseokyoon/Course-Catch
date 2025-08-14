package com.Catch_Course.domain.reservation.dto;

import com.Catch_Course.domain.reservation.entity.Reservation;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReservationDto {
    private long courseId;              // 강의 id
    private String courseTitle;         // 강의 제목
    private long studentId;             // 학생 id
    private String studentName;         // 학생 이름
    private String status;              // 예약 상태
    private Long price;
    @JsonProperty("createdDatetime")
    private LocalDateTime createdDate;

    public ReservationDto(Reservation reservation) {
        this.courseId = reservation.getCourse().getId();
        this.courseTitle = reservation.getCourse().getTitle();
        this.studentId = reservation.getStudent().getId();
        this.studentName = reservation.getStudent().getNickname();
        this.status = reservation.getStatus().toString();
        this.price = reservation.getPrice();
        this.createdDate = reservation.getCreatedDate();
    }

}
