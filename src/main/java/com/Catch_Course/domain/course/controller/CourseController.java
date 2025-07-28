package com.Catch_Course.domain.course.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.course.dto.CourseDto;
import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.exception.ServiceException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/posts")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final Rq rq;

    @GetMapping
    public RsData<List<CourseDto>> getItems() {

        List<Course> courses = courseService.getItems();
        List<CourseDto> courseDtos = courses.stream()
                .map(CourseDto::new)
                .toList();

        return new RsData<>(
                "200-1",
                "글 목록 조회가 완료되었습니다.",
                courseDtos
        );
    }


    @GetMapping("{id}")
    public RsData<CourseDto> getItem(@PathVariable long id) {

        Course course = courseService.getItem(id).get();

        return new RsData<>(
                "200-1",
                "글 조회가 완료되었습니다.",
                new CourseDto(course)
        );
    }

    @DeleteMapping("/{id}")
    public RsData<Void> delete(@PathVariable long id) {

        Member actor = rq.getAuthenticatedActor();
        Course course = courseService.getItem(id).get();

        course.canDelete(actor);
        courseService.delete(course);

        return new RsData<>(
                "200-1",
                "%d번 글 삭제가 완료되었습니다.".formatted(id)
        );
    }


    record ModifyReqBody(@NotBlank @Length(min = 3) String title,
                         @NotBlank @Length(min = 3) String content
    ) {
    }

    @PutMapping("{id}")
    public RsData<Void> modify(@PathVariable long id, @RequestBody @Valid ModifyReqBody body
    ) {

        Member actor = rq.getAuthenticatedActor();
        Course course = courseService.getItem(id).get();

        if (course.getAuthor().getId() != actor.getId()) {
            throw new ServiceException("403-1", "자신이 작성한 글만 수정 가능합니다.");
        }

        course.canModify(actor);
        courseService.modify(course, body.title(), body.content());
        return new RsData<>(
                "200-1",
                "%d번 글 수정이 완료되었습니다.".formatted(id),
                null
        );
    }

    record WriteReqBody(
            @NotBlank @Length(min = 3) String title,
            @NotBlank @Length(min = 3) String content
    ) {
    }

    @PostMapping
    public RsData<CourseDto> write(@RequestBody @Valid WriteReqBody body) {

        Member actor = rq.getAuthenticatedActor();
        Course course = courseService.write(actor, body.title(), body.content());

        return new RsData<>(
                "200-1",
                "글 작성이 완료되었습니다.",
                new CourseDto(course)
        );
    }
}
