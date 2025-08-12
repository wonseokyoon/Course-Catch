package com.Catch_Course.domain.notification.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.notification.service.NotificationService;
import com.Catch_Course.global.Rq;
import com.Catch_Course.global.dto.RsData;
import com.Catch_Course.global.sse.service.SseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@Tag(name = "SSE", description = "이벤트 전송")
public class NotificationController {

    private final SseService sseService;
    private final Rq rq;
    private final NotificationService notificationService;

    @Operation(summary = "SSE 연결 생성", description = "타임 아웃 1시간")
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe() {
        Member member = rq.getDummyMember();
        Member realMember = rq.getMember(member);
        return sseService.subscribe(realMember.getId());
    }

    @Operation(summary = "이벤트 조회")
    @GetMapping("event")
    public RsData<?> getEvent() {
        Member member = rq.getDummyMember();
        List<Object> events = notificationService.getNotifications(member.getId());

        return new RsData<>(
                "200-1",
                "이벤트 조회가 완료되었습니다.",
                events
        );
    }
}