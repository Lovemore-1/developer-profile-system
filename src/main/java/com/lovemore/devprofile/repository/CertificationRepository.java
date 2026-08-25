package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CertificationRepository extends JpaRepository<Certification, Long> {
    List<Certification> findByProfileIdOrderById(Long profileId);
}
