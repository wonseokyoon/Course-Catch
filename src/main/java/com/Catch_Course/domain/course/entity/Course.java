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
    private Member author;
    private String title;
    private String content;


    public void canModify(Member actor) {
        if (actor == null) {
            throw new ServiceException("401-1", "인증 정보가 없습니다.");
        }

        if (actor.isAdmin()) return;

        if (actor.equals(this.author)) return;

        throw new ServiceException("403-1", "자신이 작성한 글만 수정 가능합니다.");
    }

    public void canDelete(Member actor) {
        if (actor == null) {
            throw new ServiceException("401-1", "인증 정보가 없습니다.");
        }

        if (actor.isAdmin()) return;

        if (actor.equals(this.author)) return;

        throw new ServiceException("403-1", "자신이 작성한 글만 삭제 가능합니다.");
    }
}
