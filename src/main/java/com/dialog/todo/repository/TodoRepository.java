package com.dialog.todo.repository;
import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dialog.todo.domain.Todo;
import com.dialog.todo.domain.TodoStatus;
import com.dialog.user.domain.MeetUser;

public interface TodoRepository extends JpaRepository<Todo, Long> {

	// 미결 아이템 총 개수 (상태가 COMPLETED가 아닌 것)
    long countByUserAndStatusNot(MeetUser user, TodoStatus status);

  // 특정 기간에 생성된 미결 아이템 개수
  long countByUserAndStatusNotAndCreatedAtBetween(MeetUser user, TodoStatus status, LocalDateTime start, LocalDateTime end);
}
