package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.Education;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {
    List<Education> findByProfileIdOrderById(Long profileId);
}
