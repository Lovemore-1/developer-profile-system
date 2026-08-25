package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByProfileIdOrderById(Long profileId);
}
