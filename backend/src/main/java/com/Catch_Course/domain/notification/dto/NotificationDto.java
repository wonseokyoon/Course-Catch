package com.Catch_Course.domain.notification.dto;

import com.Catch_Course.domain.reservation.entity.Reservation;
import com.Catch_Course.domain.reservation.entity.ReservationStatus;
import lombok.Getter;

@Getter
public class NotificationDto {
    private ReservationStatus status;
    private String message;
    private String courseTitle;

    public NotificationDto(Reservation reservation, String message) {
        this.courseTitle = reservation.getCourse().getTitle();
        this.status = reservation.getStatus();
        this.message = message;
    }

    public NotificationDto(ReservationStatus status, String message) {
        this.status = status;
        this.message = message;
    }

}
