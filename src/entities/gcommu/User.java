package entities.gcommu;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {
    private int id;
    private String username;
    private String firstName;
    private String lastName;
    private String email;
    private String passwordHash;
    private String roles;
    private boolean isActive;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private String type;
    private String headline;
    private String bio;
    private String location;
    private String visibility;
    private String niveau;
    private Double scoreGlobal;
    private String orgName;
    private String description;
    private String websiteUrl;
    private String logoUrl;
    private String profilePic;
    private boolean online;
    private boolean favorite;

    // Constructeurs
    public User() {
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    public User(int id, String username) {
        this.id = id;
        this.username = username;
        this.favorite = false;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
    }

    public User(String firstName, String lastName, String email) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = firstName + " " + lastName;
        this.email = email;
        this.createdAt = LocalDateTime.now();
        this.isActive = true;
        this.favorite = false;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getUsername() {
        if (username == null && firstName != null) {
            return firstName + (lastName != null ? " " + lastName : "");
        }
        return username;
    }
    public void setUsername(String username) { this.username = username; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
        updateUsername();
    }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) {
        this.lastName = lastName;
        updateUsername();
    }

    private void updateUsername() {
        if (firstName != null) {
            this.username = firstName + (lastName != null ? " " + lastName : "");
        }
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRoles() { return roles; }
    public void setRoles(String roles) { this.roles = roles; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getVisibility() { return visibility; }
    public void setVisibility(String visibility) { this.visibility = visibility; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public Double getScoreGlobal() { return scoreGlobal; }
    public void setScoreGlobal(Double scoreGlobal) { this.scoreGlobal = scoreGlobal; }

    public String getOrgName() { return orgName; }
    public void setOrgName(String orgName) { this.orgName = orgName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getProfilePic() { return profilePic; }
    public void setProfilePic(String profilePic) { this.profilePic = profilePic; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public boolean isFavorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    @Override
    public String toString() {
        return getUsername() != null ? getUsername() : "User " + id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
