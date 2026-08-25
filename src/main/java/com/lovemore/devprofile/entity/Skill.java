package com.lovemore.devprofile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Skill {
    public enum Level { BEGINNER, INTERMEDIATE, ADVANCED, EXPERT }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "profile_id", nullable = false) private Profile profile;

    @NotBlank(message = "Skill name is required") private String name;
    @NotBlank(message = "Category is required") private String category;
    @Enumerated(EnumType.STRING) private Level level;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Level getLevel() { return level; }
    public void setLevel(Level level) { this.level = level; }
}
