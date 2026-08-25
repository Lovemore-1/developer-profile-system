package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.ProfessionalLink;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProfessionalLinkRepository extends JpaRepository<ProfessionalLink, Long> {
    List<ProfessionalLink> findByProfileIdOrderById(Long profileId);
}
