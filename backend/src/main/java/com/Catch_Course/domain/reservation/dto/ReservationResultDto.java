package com.Catch_Course.domain.reservation.dto;

import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import lombok.Getter;

@Getter
public class ReservationResultDto {
    private long courseId;              // 강의 id
    private String courseTitle;         // 강의 제목
    private long studentId;             // 학생 id
    private String studentName;         // 학생 이름
    private ReservationStatus status;
    private String message;

    public ReservationResultDto(Reservation reservation, String message) {
        this.courseId = reservation.getCourse().getId();
        this.courseTitle = reservation.getCourse().getTitle();
        this.studentId = reservation.getStudent().getId();
        this.studentName = reservation.getStudent().getNickname();
        this.status = reservation.getStatus();
        this.message = message;
    }

    public ReservationResultDto(ReservationStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
