package com.Catch_Course.domain.home;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "HomeController", description = "API 서버 홈")
@RestController
@RequestMapping("/home")
public class homeController {

    @Operation(summary = "홈 화면 뷰 반환", description = "서버의 기본 홈 화면(랜딩 페이지)을 반환합니다.")
    @GetMapping()
    public String home() {
        // "home"이라는 이름의 뷰(html 파일)를 찾아서 반환합니다.
        return "welcome";
    }
}