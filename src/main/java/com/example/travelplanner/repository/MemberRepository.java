package com.example.travelplanner.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.travelplanner.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
}