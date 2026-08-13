package com.tasktracker.repository;

import com.tasktracker.domain.Task;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @Override
    @EntityGraph(attributePaths = "assignee")
    Optional<Task> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "assignee")
    Page<Task> findAll(Pageable pageable);
}
