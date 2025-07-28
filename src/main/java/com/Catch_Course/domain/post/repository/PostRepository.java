package com.Catch_Course.domain.post.repository;

import com.Catch_Course.domain.post.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
