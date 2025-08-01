package com.Catch_Course.domain.course.controller;

import com.Catch_Course.domain.course.dto.CourseDto;
import com.Catch_Course.domain.course.dto.PageDto;
import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.exception.ServiceException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "CourseController", description = "강의 관련 API")
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final Rq rq;

    @Operation(
            summary = "강의 목록 조회"
    )
    @GetMapping
    public RsData<PageDto> getItems(@RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "10") int pageSize,
                                    @RequestParam(defaultValue = "title") KeywordType keywordType,
                                    @RequestParam(defaultValue = "") String keyword
    ) {

        Page<Course> coursePage = courseService.getItems(page,pageSize,keywordType,keyword);

        if(coursePage.isEmpty()) throw new ServiceException("404-2","일치하는 강의가 없습니다.");

        return new RsData<>(
                "200-1",
                "강의 목록 조회가 완료되었습니다.",
                new PageDto(coursePage)
        );
    }


    @Operation(
            summary = "강의 상세 조회"
    )
    @GetMapping("{id}")
    public RsData<CourseDto> getItem(@PathVariable long id) {

        Course course = courseService.getItem(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        return new RsData<>(
                "200-1",
                "강의 상세 조회가 완료되었습니다.",
                new CourseDto(course)
        );
    }

    @Operation(
            summary = "강의 삭제"
    )
    @DeleteMapping("/{id}")
    public RsData<Void> delete(@PathVariable long id) {

        Member dummyMember = rq.getDummyMember();        // 더미 유저 객체(id,username,authorities 만 있음, 필요하면 DB에서 꺼내씀)
        Course course = courseService.getItem(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        course.canDelete(dummyMember);
        courseService.delete(course);

        return new RsData<>(
                "200-1",
                "%d번 강의 삭제가 완료되었습니다.".formatted(id)
        );
    }


    record ModifyReqBody(@NotBlank @Length(min = 3) String title,
                         @NotBlank @Length(min = 3) String content,
                         @NotNull @Min(1) long capacity
    ) {
    }

    @Operation(
            summary = "강의 수정"
    )
    @PutMapping("{id}")
    public RsData<Void> modify(@PathVariable long id, @RequestBody @Valid ModifyReqBody body
    ) {

        Member dummyMember = rq.getDummyMember();
        Course course = courseService.getItem(id)
                .orElseThrow(() -> new ServiceException("404-1", "존재하지 않는 강의입니다."));

        if (!course.getInstructor().getId().equals(dummyMember.getId())) {
            throw new ServiceException("403-1", "자신이 작성한 글만 수정 가능합니다.");
        }

        course.canModify(dummyMember);
        courseService.modify(course, body.title(), body.content(), body.capacity());
        return new RsData<>(
                "200-1",
                "%d번 글 수정이 완료되었습니다.".formatted(id),
                null
        );
    }

    record WriteReqBody(
            @NotBlank @Length(min = 3) String title,
            @NotBlank @Length(min = 3) String content,
            @NotNull @Min(1) long capacity
    ) {
    }

    @Operation(
            summary = "강의 생성"
    )
    @PostMapping
    public RsData<CourseDto> write(@RequestBody @Valid WriteReqBody body) {

        Member dummyMember = rq.getDummyMember();
        Course course = courseService.write(dummyMember, body.title(), body.content(), body.capacity());

        return new RsData<>(
                "200-1",
                "강의 생성이 완료되었습니다.",
                new CourseDto(course)
        );
    }

    record StatisticsResBody(long courseCount, long publishedCount) {
    }

    @Operation(
            summary = "집계 페이지",
            description = "관리자만 접근 가능한 집계 페이지"
    )
    @GetMapping("statistics")
    public RsData<StatisticsResBody> getStatistics() {

        return new RsData<>(
                "200-1",
                "통계 조회가 완료되었습니다.",
                new StatisticsResBody(20, 10)
        );
    }
}
