package com.Catch_Course.domain.reservation.dto;

import com.Catch_Course.domain.reservation.entity.Reservation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@AllArgsConstructor
public class PageDto {

    List<ReservationDto> items;

    int totalPages;     // 전체 페이지
    int totalItems;     // 전체 갯수
    int currentPage;    // 현재 페이지
    int pageSize;       // 페이지 사이즈


    public PageDto(Page<Reservation> reservationPage) {

        this.items =reservationPage.stream()
                .map(ReservationDto::new)
                .toList();

        this.totalPages = reservationPage.getTotalPages();
        this.totalItems = (int) reservationPage.getTotalElements();
        this.currentPage = reservationPage.getNumber() + 1;
        this.pageSize = reservationPage.getSize();
    }
}
