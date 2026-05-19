package com.secretlab.exercise.repository;

import com.secretlab.exercise.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
