package com.Catch_Course.domain.course.controller;

import com.Catch_Course.domain.course.dto.CourseDto;
import com.Catch_Course.domain.course.entity.Course;
import com.Catch_Course.domain.course.service.CourseService;
import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.member.service.MemberService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static com.Catch_Course.domain.course.controller.KeywordType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    @DisplayName("user2로 로그인 셋업")
    void setUp() {
        loginedMember = memberService.findByUsername("user2").get();
        token = memberService.getAuthToken(loginedMember);
    }

    private void checkCourses(List<Course> courses, ResultActions resultActions) throws Exception {
        for (int i = 0; i < courses.size(); i++) {

            Course course = courses.get(i);

            resultActions
                    .andExpect(jsonPath("$.data.items[%d]".formatted(i)).exists())
                    .andExpect(jsonPath("$.data.items[%d].id".formatted(i)).value(course.getId()))
                    .andExpect(jsonPath("$.data.items[%d].title".formatted(i)).value(course.getTitle()))
                    .andExpect(jsonPath("$.data.items[%d].content".formatted(i)).value(course.getContent()))
                    .andExpect(jsonPath("$.data.items[%d].instructorId".formatted(i)).value(course.getInstructor().getId()))
                    .andExpect(jsonPath("$.data.items[%d].instructorName".formatted(i)).value(course.getInstructor().getNickname()))
                    .andExpect(jsonPath("$.data.items[%d].capacity".formatted(i)).value(course.getCapacity()))
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
                .andExpect(jsonPath("$.msg").value("강의 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(10))
                .andExpect(jsonPath("$.data.currentPage").value(1));

        // DB 에서 실제 강의 목록 가져옴
        Page<Course> coursePage = courseService.getItems(1, 10, title, "");
        List<Course> courses = coursePage.getContent();
        checkCourses(courses, resultActions);
    }

    @Test
    @DisplayName("강의 목록 조회 - 페이징 처리 확인")
    void getItems2() throws Exception {
        int page = 3;
        int pageSize = 8;
        ResultActions resultActions = mvc.perform(
                        get("/api/courses?page=" + page + "&pageSize=" + pageSize)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items.length()").value(pageSize))
                .andExpect(jsonPath("$.data.currentPage").value(page));

        // DB 에서 실제 강의 목록 가져옴
        Page<Course> coursePage = courseService.getItems(page, pageSize, title, "");
        List<Course> courses = coursePage.getContent();
        checkCourses(courses, resultActions);
    }

    @Test
    @DisplayName("강의 목록 조회 - 검색 by 제목")
    void getItems3() throws Exception {
        String keyword = "국어";
        ResultActions resultActions = mvc.perform(
                        get("/api/courses?&keyword=" + keyword)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items[*].title").value(everyItem(containsString(keyword))))
        ;
    }

    @Test
    @DisplayName("강의 목록 조회 - 검색 by 내용")
    void getItems4() throws Exception {
        KeywordType keywordType = content;
        String keyword = "비문학";
        ResultActions resultActions = mvc.perform(
                        get("/api/courses?keywordType=" + keywordType + "&keyword=" + keyword)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items[*].content").value(everyItem(containsString(keyword))))
        ;
    }

    @Test
    @DisplayName("강의 목록 조회 - 검색 by 작성자")
    void getItems5() throws Exception {
        KeywordType keywordType = instructor;
        String keyword = "유저2";
        ResultActions resultActions = mvc.perform(
                        get("/api/courses?keywordType=" + keywordType + "&keyword=" + keyword)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 목록 조회가 완료되었습니다."))
                .andExpect(jsonPath("$.data.items[*].instructorName").value(everyItem(equalTo(keyword))))   // 작성자는 일치해야함
        ;
    }

    @Test
    @DisplayName("강의 목록 조회 - 검색 실패")
    void getItems6() throws Exception {
        KeywordType keywordType = instructor;
        String keyword = "유저";
        ResultActions resultActions = mvc.perform(
                        get("/api/courses?keywordType=" + keywordType + "&keyword=" + keyword)
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-2"))
                .andExpect(jsonPath("$.msg").value("일치하는 강의가 없습니다."));
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
    @DisplayName("강의 상세 조회 실패 - 존재하지 않는 강의")
    void getItem2() throws Exception {
        long courseId = 999L;

        ResultActions resultActions = mvc.perform(
                        get("/api/courses/%d".formatted(courseId))
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
    }

    @Test
    @DisplayName("강의 삭제 성공 - 작성자만 삭제 가능")
    void deleteItem1() throws Exception {
        long courseId = 1L;

        ResultActions resultActions = mvc.perform(
                        delete("/api/courses/%d".formatted(courseId))
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 강의 삭제가 완료되었습니다.".formatted(courseId)));

    }

    @Test
    @DisplayName("강의 삭제 실패 - 작성자만 삭제 가능")
    void deleteItem2() throws Exception {
        long courseId = 5L;

        ResultActions resultActions = mvc.perform(
                        delete("/api/courses/%d".formatted(courseId))
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("자신이 생성한 강의만 삭제 가능합니다."));
    }

    @Test
    @DisplayName("강의 삭제 실패 - 존재하지 않는 강의 삭제")
    void deleteItem3() throws Exception {
        long courseId = 999L;

        ResultActions resultActions = mvc.perform(
                        delete("/api/courses/%d".formatted(courseId))
                                .header("Authorization", "Bearer " + token)
                )
                .andDo(print());

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
    }

    private ResultActions modifyRequest(long id, String title, String content, long capacity) throws Exception {
        Map<String, Object> requestBody = Map.of("title", title, "content", content, "capacity", capacity);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        put("/api/courses/%d".formatted(id))
                                .header("Authorization", "Bearer " + token)
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    @Test
    @DisplayName("강의 수정")
    void modifyItem() throws Exception {
        long courseId = 1L;
        String title = "수정된 제목";
        String content = "수정된 내용";
        long capacity = 50;

        ResultActions resultActions = modifyRequest(courseId, title, content, capacity);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("%d번 글 수정이 완료되었습니다.".formatted(courseId)));

        Course course = courseService.getItem(courseId).get();
        assertThat(course.getTitle()).isEqualTo(title);
        assertThat(course.getContent()).isEqualTo(content);
        assertThat(course.getCapacity()).isEqualTo(capacity);
    }

    @Test
    @DisplayName("강의 수정 실패 - 작성자만 수정 가능")
    void modifyItem2() throws Exception {
        long courseId = 4L;
        String title = "수정된 제목";
        String content = "수정된 내용";
        long capacity = 50;

        ResultActions resultActions = modifyRequest(courseId, title, content, capacity);

        resultActions
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("403-1"))
                .andExpect(jsonPath("$.msg").value("자신이 작성한 글만 수정 가능합니다."));
    }

    @Test
    @DisplayName("강의 수정 실패 - 존재하지 않는 강의")
    void modifyItem3() throws Exception {
        long courseId = 3333L;
        String title = "수정된 제목";
        String content = "수정된 내용";
        long capacity = 50;

        ResultActions resultActions = modifyRequest(courseId, title, content, capacity);

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("404-1"))
                .andExpect(jsonPath("$.msg").value("존재하지 않는 강의입니다."));
    }

    private ResultActions writeRequest(String title, String content, long capacity) throws Exception {
        Map<String, Object> requestBody = Map.of("title", title, "content", content, "capacity", capacity);

        // Map -> Json 변환
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(requestBody);

        return mvc
                .perform(
                        post("/api/courses")
                                .header("Authorization", "Bearer " + token)
                                .content(json)
                                .contentType(new MediaType(MediaType.APPLICATION_JSON, StandardCharsets.UTF_8))
                ).andDo(print());
    }

    @Test
    @DisplayName("강의 생성")
    void write() throws Exception {
        String title = "테스트 제목";
        String content = "테스트 내용";
        long capacity = 100;

        ResultActions resultActions = writeRequest(title, content, capacity);

        resultActions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("200-1"))
                .andExpect(jsonPath("$.msg").value("강의 생성이 완료되었습니다."));

        ObjectMapper mapper = new ObjectMapper();
        String jsonContent = resultActions.andReturn().getResponse().getContentAsString();      // 문자열 응답
        JsonNode json = mapper.readTree(jsonContent);   // 파싱
        long courseId = json.get("data").get("id").asLong();  // data 필드의 id 필드

        // DB 에서 실제 강의 목록 가져옴
        CourseDto courseDto = new CourseDto(courseService.getItem(courseId).get());
        checkCourse(courseDto, resultActions);
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
