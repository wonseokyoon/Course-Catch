package com.Catch_Course.domain.course.entity;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.entity.BaseTime;
import com.Catch_Course.global.exception.ServiceException;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Course extends BaseTime {

    @ManyToOne(fetch = FetchType.LAZY)
    private Member instructor;      // 강사
    private String title;           // 강의 명
    private String content;         // 강의 내용
    private long capacity;           // 정원


    public void canModify(Member member) {
        if (member == null) {
            throw new ServiceException("401-1", "인증 정보가 없습니다.");
        }

        if (member.isAdmin()) return;

        if (member.equals(this.instructor)) return;

        throw new ServiceException("403-1", "자신이 생성한 강의만 수정 가능합니다.");
    }

    public void canDelete(Member member) {
        if (member == null) {
            throw new ServiceException("401-1", "인증 정보가 없습니다.");
        }

        if (member.isAdmin()) return;

        if (member.equals(this.instructor)) return;

        throw new ServiceException("403-1", "자신이 생성한 강의만 삭제 가능합니다.");
    }
}
