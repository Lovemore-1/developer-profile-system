package com.lovemore.devprofile.repository;

import com.lovemore.devprofile.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByOwnerUsername(String username);
}
