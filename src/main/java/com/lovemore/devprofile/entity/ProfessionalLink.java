package com.lovemore.devprofile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
public class ProfessionalLink {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "profile_id", nullable = false) private Profile profile;

    @NotBlank(message = "Label is required, e.g. LinkedIn, GitHub, Portfolio") private String label;
    @NotBlank(message = "URL is required") private String url;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
