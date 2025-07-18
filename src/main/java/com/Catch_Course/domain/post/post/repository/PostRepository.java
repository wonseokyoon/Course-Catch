package com.Catch_Course.domain.post.post.repository;

import com.Catch_Course.domain.post.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
