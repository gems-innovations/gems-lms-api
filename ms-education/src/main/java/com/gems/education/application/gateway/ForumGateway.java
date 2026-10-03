package com.gems.education.application.gateway;

import com.gems.education.domain.entities.ForumPost;
import com.gems.education.domain.entities.ForumThread;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ForumGateway {
  /** Pinned first, then by latest activity; search matches title or body. */
  Flux<ForumThread> threads(Long courseId, String search, int offset, int limit);

  Mono<Long> countThreads(Long courseId, String search);

  Mono<ForumThread> findThread(Long id);

  Mono<ForumThread> saveThread(ForumThread thread);

  Mono<Void> deleteThread(Long id);

  /** Oldest first. */
  Flux<ForumPost> posts(Long threadId);

  Mono<ForumPost> findPost(Long id);

  /** Inserting a reply also bumps the thread's reply count and last activity. */
  Mono<ForumPost> savePost(ForumPost post);

  Mono<Void> deletePost(ForumPost post);
}
