package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    List<Experience> findByProfileIdOrderById(Long profileId);
}
