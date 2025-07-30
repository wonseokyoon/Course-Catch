package com.Catch_Course.domain.course.controller;

import com.Catch_Course.domain.course.dto.CourseDto;
import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Transactional
class CourseControllerTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private MemberService memberService;

    @Autowired
    private CourseService courseService;

    private String token;
    private Member loginedMember;

    @BeforeEach
    @DisplayName("user1로 로그인 셋업")
    void setUp() {
        loginedMember = memberService.findByUsername("user1").get();
        token = memberService.getAuthToken(loginedMember);
    }

    private void checkCourses(List<CourseDto> courses, ResultActions resultActions) throws Exception {
        for (int i = 0; i < courses.size(); i++) {

            CourseDto course = courses.get(i);

            resultActions
                    .andExpect(jsonPath("$.data[%d]".formatted(i)).exists())
                    .andExpect(jsonPath("$.data[%d].id".formatted(i)).value(course.getId()))
                    .andExpect(jsonPath("$.data[%d].title".formatted(i)).value(course.getTitle()))
                    .andExpect(jsonPath("$.data[%d].instructorId".formatted(i)).value(course.getInstructorId()))
                    .andExpect(jsonPath("$.data[%d].instructorName".formatted(i)).value(course.getInstructorName()))
                    .andExpect(jsonPath("$.data[%d].capacity".formatted(i)).value(course.getCapacity()))
            ;
        }
    }

    @Test
    @DisplayName("강의 목록 조회")
    void getItems() throws Exception {
        ResultActions resultActions = mvc.perform(
                        get("/api/courses")
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 목록 조회가 완료되었습니다."));

        // DB 에서 실제 강의 목록 가져옴
        List<CourseDto> courseDtos = courseService.getItems()
                .stream()
                .map(CourseDto::new)
                .toList();

        checkCourses(courseDtos, resultActions);
    }

    private void checkCourse(CourseDto course, ResultActions resultActions) throws Exception {

        resultActions
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.id").value(course.getId()))
                .andExpect(jsonPath("$.data.title").value(course.getTitle()))
                .andExpect(jsonPath("$.data.instructorId").value(course.getInstructorId()))
                .andExpect(jsonPath("$.data.instructorName").value(course.getInstructorName()))
                .andExpect(jsonPath("$.data.capacity").value(course.getCapacity()))
        ;
    }

    @Test
    @DisplayName("강의 상세 조회")
    void getItem() throws Exception {
        long courseId = 1L;

        ResultActions resultActions = mvc.perform(
                        get("/api/courses/%d".formatted(courseId))
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());


        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 상세 조회가 완료되었습니다."));

        // DB 에서 실제 강의 목록 가져옴
        CourseDto courseDto = new CourseDto(courseService.getItem(courseId).get());
        checkCourse(courseDto, resultActions);
    }

    @Test
    @DisplayName("강의 삭제 - 작성자만 삭제 가능")
    void delete() {
    }

    @Test
    @DisplayName("강의 수정 - 작성자만 수정 가능")
    void modify() {
    }

    @Test
    @DisplayName("강의 생성")
    void write() {
    }

    String adminLogin() {
        loginedMember = memberService.findByUsername("admin").get();
        return memberService.getAccessToken(loginedMember);
    }

    @Test
    @DisplayName("통계 - 관리자 기능 - 관리자 접근")
    @WithUserDetails("admin")
        // CustomUserDetailService 의 loadUserByUsername 메서드를 통해 유저 정보 가져옴
    void getStatistics() throws Exception {
//        accessToken = adminLogin();
        ResultActions resultActions = mvc.perform(
                        get("/api/courses/statistics")
                        // @WithUserDetails("admin") 으로 로그인 한 것처럼 되기때문에, 헤더를 굳이 입력할 필요 x
//                                .header("Authorization", "Bearer " + accessToken)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("통계 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.courseCount").value(20))
                .andExpect(jsonPath("$.data.publishedCount").value(10));
    }

    @Test
    @DisplayName("통계 - 관리자 기능 - user1 접근")
    void statisticsUser() throws Exception {
        ResultActions resultActions = mvc.perform(
                        get("/api/courses/statistics")
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("접근 권한이 없습니다."));

    }

}
