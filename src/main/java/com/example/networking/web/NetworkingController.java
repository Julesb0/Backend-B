package com.example.networking.web;

import com.example.auth.SupabaseProperties;
import com.example.auth.security.AuthGuard;
import com.example.auth.security.JwtProperties;
import com.example.networking.domain.Connection;
import com.example.networking.domain.Profile;
import com.example.networking.service.NetworkingFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/networking")
public class NetworkingController {
  private final NetworkingFacade facade;
  private final AuthGuard guard;

  public NetworkingController(SupabaseProperties props, JwtProperties jwtProps) {
    this.facade = new NetworkingFacade(props);
    this.guard = new AuthGuard(jwtProps, props);
  }

  @PostMapping("/profile")
  public ResponseEntity<Profile> createOrUpdateProfile(@RequestHeader(value = "Authorization", required = false) String auth,
                                                       @Valid @RequestBody ProfileRequestDto dto) {
    String subject = guard.require(auth);
    String userId = subject != null ? subject : dto.getUserEmail();
    Profile profile = facade.createOrUpdateProfile(userId, dto.getDisplayName(), dto.getBio(), 
                                                  dto.getLocation(), dto.getCompany(), dto.getRole(), 
                                                  dto.getLinkedinUrl(), dto.getWebsite(), dto.isPublic(), dto.getSkills());
    return ResponseEntity.ok(profile);
  }

  @GetMapping("/profile")
  public ResponseEntity<Profile> getProfile(@RequestHeader(value = "Authorization", required = false) String auth,
                                            @RequestParam(required = false) String userEmail) {
    String subject = guard.require(auth);
    String userId = subject != null ? subject : userEmail;
    Profile profile = facade.getProfile(userId);
    return ResponseEntity.ok(profile);
  }

  @GetMapping("/profiles")
  public ResponseEntity<List<Profile>> getAllPublicProfiles(@RequestHeader(value = "Authorization", required = false) String auth) {
    guard.require(auth);
    List<Profile> profiles = facade.getAllPublicProfiles();
    return ResponseEntity.ok(profiles);
  }

  @PostMapping("/connections")
  public ResponseEntity<Connection> sendConnectionRequest(@RequestHeader(value = "Authorization", required = false) String auth,
                                                          @Valid @RequestBody ConnectionRequestDto dto) {
    String subject = guard.require(auth);
    String requesterId = subject != null ? subject : dto.getRequesterEmail();
    Connection connection = facade.sendConnectionRequest(requesterId, dto.getAddresseeId(), dto.getMessage());
    return ResponseEntity.status(201).body(connection);
  }

  @PutMapping("/connections/{connectionId}/accept")
  public ResponseEntity<Connection> acceptConnection(@RequestHeader(value = "Authorization", required = false) String auth,
                                                   @PathVariable String connectionId) {
    guard.require(auth);
    Connection connection = facade.acceptConnectionRequest(connectionId);
    return ResponseEntity.ok(connection);
  }

  @PutMapping("/connections/{connectionId}/reject")
  public ResponseEntity<Connection> rejectConnection(@RequestHeader(value = "Authorization", required = false) String auth,
                                                   @PathVariable String connectionId) {
    guard.require(auth);
    Connection connection = facade.rejectConnectionRequest(connectionId);
    return ResponseEntity.ok(connection);
  }

  @GetMapping("/connections")
  public ResponseEntity<List<Connection>> getUserConnections(@RequestHeader(value = "Authorization", required = false) String auth,
                                                             @RequestParam(required = false) String userEmail) {
    String subject = guard.require(auth);
    String userId = subject != null ? subject : userEmail;
    List<Connection> connections = facade.getUserConnections(userId);
    return ResponseEntity.ok(connections);
  }

  @GetMapping("/connections/pending")
  public ResponseEntity<List<Connection>> getPendingRequests(@RequestHeader(value = "Authorization", required = false) String auth,
                                                             @RequestParam(required = false) String userEmail) {
    String subject = guard.require(auth);
    String userId = subject != null ? subject : userEmail;
    List<Connection> pending = facade.getPendingRequestsReceived(userId);
    return ResponseEntity.ok(pending);
  }

  @GetMapping("/connections/accepted")
  public ResponseEntity<List<Connection>> getAcceptedConnections(@RequestHeader(value = "Authorization", required = false) String auth,
                                                                 @RequestParam(required = false) String userEmail) {
    String subject = guard.require(auth);
    String userId = subject != null ? subject : userEmail;
    List<Connection> accepted = facade.getAcceptedConnections(userId);
    return ResponseEntity.ok(accepted);
  }

  @GetMapping("/network")
  public ResponseEntity<Map<String, Object>> getUserNetwork(@RequestHeader(value = "Authorization", required = false) String auth,
                                                            @RequestParam(required = false) String userEmail) {
    String subject = guard.require(auth);
    String userId = subject != null ? subject : userEmail;
    Map<String, Object> network = facade.getUserNetwork(userId);
    return ResponseEntity.ok(network);
  }

  @GetMapping("/connections/{userId1}/{userId2}")
  public ResponseEntity<Connection> getConnectionBetweenUsers(@RequestHeader(value = "Authorization", required = false) String auth,
                                                             @PathVariable String userId1,
                                                             @PathVariable String userId2) {
    guard.require(auth);
    Connection connection = facade.getConnectionBetweenUsers(userId1, userId2);
    return ResponseEntity.ok(connection);
  }

  public static class ProfileRequestDto {
    private String displayName;
    private String bio;
    private String location;
    private String company;
    private String role;
    private String linkedinUrl;
    private String website;
    private boolean isPublic = true;
    private String userEmail;
    private java.util.List<String> skills;

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
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public java.util.List<String> getSkills() { return skills; }
    public void setSkills(java.util.List<String> skills) { this.skills = skills; }
  }

  public static class ConnectionRequestDto {
    private String addresseeId;
    private String message;
    private String requesterEmail;

    public String getAddresseeId() { return addresseeId; }
    public void setAddresseeId(String addresseeId) { this.addresseeId = addresseeId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
  }
}
