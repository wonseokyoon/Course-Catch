package com.Catch_Course.domain.reservation.entity;

import com.Catch_Course.global.entity.BaseTime;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@SuperBuilder
public class DeletedHistory extends BaseTime {

    private Long memberId;
    private Long courseId;
}
