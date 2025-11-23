package com.example.networking.domain;

public class Profile {
  private String id;
  private String userId;
  private String displayName;
  private String bio;
  private String location;
  private String company;
  private String role;
  private String linkedinUrl;
  private String website;
  private boolean isPublic;
  private String createdAt;
  private String updatedAt;
  private java.util.List<String> skills;

  public Profile() {}

  public Profile(String userId, String displayName) {
    this.userId = userId;
    this.displayName = displayName;
    this.isPublic = true;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getUserId() { return userId; }
  public void setUserId(String userId) { this.userId = userId; }
  public String getDisplayName() { return displayName; }
  public void setDisplayName(String displayName) { this.displayName = displayName; }
  public String getBio() { return bio; }
  public void setBio(String bio) { this.bio = bio; }
  public String getLocation() { return location; }
  public void setLocation(String location) { this.location = location; }
  public String getCompany() { return company; }
  public void setCompany(String company) { this.company = company; }
  public String getRole() { return role; }
  public void setRole(String role) { this.role = role; }
  public String getLinkedinUrl() { return linkedinUrl; }
  public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
  public String getWebsite() { return website; }
  public void setWebsite(String website) { this.website = website; }
  public boolean isPublic() { return isPublic; }
  public void setPublic(boolean isPublic) { this.isPublic = isPublic; }
  public String getCreatedAt() { return createdAt; }
  public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
  public String getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
  public java.util.List<String> getSkills() { return skills; }
  public void setSkills(java.util.List<String> skills) { this.skills = skills; }
}
