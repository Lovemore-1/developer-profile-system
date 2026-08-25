package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.Reference;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReferenceRepository extends JpaRepository<Reference, Long> {
    List<Reference> findByProfileIdOrderById(Long profileId);
}
