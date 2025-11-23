package com.example.networking.service;

import com.example.auth.SupabaseProperties;
import com.example.networking.domain.Connection;
import com.example.networking.domain.Profile;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NetworkingFacade {
  private final ProfileService profileService;
  private final ConnectionService connectionService;

  public NetworkingFacade(SupabaseProperties props) {
    this.profileService = new ProfileService(props);
    this.connectionService = new ConnectionService(props);
  }

  public Profile createOrUpdateProfile(String userId, String displayName, String bio, String location, String company, String role, String linkedinUrl, String website, boolean isPublic, java.util.List<String> skills) {
    Profile profile = profileService.getProfileByUserId(userId);
    if (profile == null) {
      profile = new Profile(userId, displayName);
    }
    profile.setDisplayName(displayName);
    profile.setBio(bio);
    profile.setLocation(location);
    profile.setCompany(company);
    profile.setRole(role);
    profile.setLinkedinUrl(linkedinUrl);
    profile.setWebsite(website);
    profile.setPublic(isPublic);
    profile.setSkills(skills);
    
    if (profile.getId() == null) {
      return profileService.createProfile(userId, displayName);
    } else {
      return profileService.updateProfile(profile);
    }
  }

  public Profile getProfile(String userId) {
    return profileService.getProfileByUserId(userId);
  }

  public List<Profile> getAllPublicProfiles() {
    return profileService.getAllPublicProfiles();
  }

  public Connection sendConnectionRequest(String requesterId, String addresseeId, String message) {
    return connectionService.sendConnectionRequest(requesterId, addresseeId, message);
  }

  public Connection acceptConnectionRequest(String connectionId) {
    return connectionService.acceptConnection(connectionId);
  }

  public Connection rejectConnectionRequest(String connectionId) {
    return connectionService.rejectConnection(connectionId);
  }

  public List<Connection> getUserConnections(String userId) {
    return connectionService.getConnectionsByUserId(userId);
  }

  public List<Connection> getPendingRequestsReceived(String userId) {
    return connectionService.getPendingRequestsReceived(userId);
  }

  public List<Connection> getAcceptedConnections(String userId) {
    return connectionService.getAcceptedConnections(userId);
  }

  public Connection getConnectionBetweenUsers(String userId1, String userId2) {
    return connectionService.getConnectionByUsers(userId1, userId2);
  }

  public Map<String, Object> getUserNetwork(String userId) {
    Map<String, Object> network = new HashMap<>();
    
    Profile profile = profileService.getProfileByUserId(userId);
    network.put("profile", profile);
    
    List<Connection> allConnections = connectionService.getConnectionsByUserId(userId);
    List<Connection> pendingReceived = connectionService.getPendingRequestsReceived(userId);
    List<Connection> acceptedConnections = connectionService.getAcceptedConnections(userId);
    
    network.put("allConnections", allConnections);
    network.put("pendingReceived", pendingReceived);
    network.put("acceptedConnections", acceptedConnections);
    network.put("connectionCount", acceptedConnections.size());
    
    return network;
  }
}
