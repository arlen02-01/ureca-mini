package com.example.ureka02.user;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>{
	Optional<User> findByName(String name);
	Optional<User> findById(long id);
	Optional<User> findBySocialId(String socialId);
	Optional<User> findByEmail(String email);
	boolean existsByName(String name);
}
