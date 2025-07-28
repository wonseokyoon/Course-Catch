package com.Catch_Course.domain.post.controller;

import com.Catch_Course.domain.member.entity.Member;
import com.Catch_Course.domain.post.dto.PostDto;
import com.Catch_Course.domain.post.entity.Post;
import com.Catch_Course.domain.post.service.PostService;
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
public class ApiV1PostController {

    private final PostService postService;
    private final Rq rq;

    @GetMapping
    public RsData<List<PostDto>> getItems() {

        List<Post> posts = postService.getItems();
        List<PostDto> postDtos = posts.stream()
                .map(PostDto::new)
                .toList();

        return new RsData<>(
                "200-1",
                "글 목록 조회가 완료되었습니다.",
                postDtos
        );
    }


    @GetMapping("{id}")
    public RsData<PostDto> getItem(@PathVariable long id) {

        Post post = postService.getItem(id).get();

        return new RsData<>(
                "200-1",
                "글 조회가 완료되었습니다.",
                new PostDto(post)
        );
    }

    @DeleteMapping("/{id}")
    public RsData<Void> delete(@PathVariable long id) {

        Member actor = rq.getAuthenticatedActor();
        Post post = postService.getItem(id).get();

        post.canDelete(actor);
        postService.delete(post);

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
        Post post = postService.getItem(id).get();

        if (post.getAuthor().getId() != actor.getId()) {
            throw new ServiceException("403-1", "자신이 작성한 글만 수정 가능합니다.");
        }

        post.canModify(actor);
        postService.modify(post, body.title(), body.content());
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
    public RsData<PostDto> write(@RequestBody @Valid WriteReqBody body) {

        Member actor = rq.getAuthenticatedActor();
        Post post = postService.write(actor, body.title(), body.content());

        return new RsData<>(
                "200-1",
                "글 작성이 완료되었습니다.",
                new PostDto(post)
        );
    }
}
