package com.example.networking.repository;

import com.example.auth.SupabaseProperties;
import com.example.networking.domain.Connection;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ConnectionRepository {
  private final SupabaseProperties props;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper mapper = new ObjectMapper();
  private final String table = "connections";

  public ConnectionRepository(SupabaseProperties props) {
    this.props = props;
  }

  public Connection save(Connection connection) {
    try {
      String json = toSupabaseJson(connection);
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getUrl() + "/rest/v1/" + table))
        .header("Content-Type", "application/json")
        .header("apikey", apiKey())
        .header("Authorization", "Bearer " + apiKey())
        .header("Prefer", "return=representation")
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
      HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() >= 200 && res.statusCode() < 300) {
        JsonNode arr = mapper.readTree(res.body());
        if (arr.isArray() && arr.size() > 0) {
          return fromJson(arr.get(0));
        }
        return connection;
      }
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Supabase save failed: " + res.body());
    } catch (Exception e) {
      if (e instanceof org.springframework.web.server.ResponseStatusException) throw (org.springframework.web.server.ResponseStatusException) e;
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  public List<Connection> findByUserId(String userId) {
    return query("or=(requester_user_id.eq." + encode(userId) + ",target_user_id.eq." + encode(userId) + ")");
  }

  public List<Connection> findByRequesterId(String requesterId) {
    return query("requester_user_id=eq." + encode(requesterId));
  }

  public List<Connection> findByAddresseeId(String addresseeId) {
    return query("target_user_id=eq." + encode(addresseeId));
  }

  public Connection findByUsers(String requesterId, String addresseeId) {
    List<Connection> connections = query("requester_user_id=eq." + encode(requesterId) + "&target_user_id=eq." + encode(addresseeId));
    return connections.isEmpty() ? null : connections.get(0);
  }

  public Connection updateStatus(String connectionId, String status) {
    try {
      String json = "{\"status\":\"" + escape(status) + "\",\"updated_at\":\"" + java.time.Instant.now().toString() + "\"}";
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getUrl() + "/rest/v1/" + table + "?id=eq." + encode(connectionId)))
        .header("Content-Type", "application/json")
        .header("apikey", apiKey())
        .header("Authorization", "Bearer " + apiKey())
        .header("Prefer", "return=representation")
        .method("PATCH", HttpRequest.BodyPublishers.ofString(json))
        .build();
      HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() >= 200 && res.statusCode() < 300) {
        JsonNode arr = mapper.readTree(res.body());
        if (arr.isArray() && arr.size() > 0) {
          return fromJson(arr.get(0));
        }
        return null;
      }
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Supabase update failed: " + res.body());
    } catch (Exception e) {
      if (e instanceof org.springframework.web.server.ResponseStatusException) throw (org.springframework.web.server.ResponseStatusException) e;
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private List<Connection> query(String filter) {
    try {
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getUrl() + "/rest/v1/" + table + "?" + filter))
        .header("apikey", apiKey())
        .header("Authorization", "Bearer " + apiKey())
        .build();
      HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() >= 200 && res.statusCode() < 300) {
        List<Connection> list = new ArrayList<>();
        JsonNode arr = mapper.readTree(res.body());
        if (arr.isArray()) {
          for (JsonNode n : arr) {
            list.add(fromJson(n));
          }
        }
        return list;
      }
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Supabase query failed: " + res.body());
    } catch (Exception e) {
      if (e instanceof org.springframework.web.server.ResponseStatusException) throw (org.springframework.web.server.ResponseStatusException) e;
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  private String toSupabaseJson(Connection c) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (c.getId() == null || c.getId().isEmpty()) {
      c.setId(java.util.UUID.randomUUID().toString());
    }
    sb.append("\"id\":\"").append(c.getId()).append("\",");
    sb.append("\"requester_user_id\":\"").append(escape(c.getRequesterId())).append("\",");
    sb.append("\"target_user_id\":\"").append(escape(c.getAddresseeId())).append("\",");
    sb.append("\"status\":\"").append(escape(c.getStatus())).append("\",");
    if (c.getMessage() != null) sb.append("\"message\":\"").append(escape(c.getMessage())).append("\",");
    sb.append("\"created_at\":\"").append(java.time.Instant.now().toString()).append("\",");
    sb.append("\"updated_at\":\"").append(java.time.Instant.now().toString()).append("\"");
    sb.append("}");
    return sb.toString();
  }


  private Connection fromJson(JsonNode n) {
    Connection c = new Connection();
    if (n.get("id") != null) c.setId(n.get("id").asText());
    if (n.get("requester_user_id") != null) c.setRequesterId(n.get("requester_user_id").asText());
    if (n.get("target_user_id") != null) c.setAddresseeId(n.get("target_user_id").asText());
    if (n.get("status") != null) c.setStatus(n.get("status").asText());
    if (n.get("message") != null) c.setMessage(n.get("message").asText());
    if (n.get("created_at") != null) c.setCreatedAt(n.get("created_at").asText());
    if (n.get("updated_at") != null) c.setUpdatedAt(n.get("updated_at").asText());
    return c;
  }

  private String escape(String s) { return s == null ? "" : s.replace("\"","\\\""); }
  private String encode(String s) { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
  private String apiKey() { return (props.getServiceRoleKey() != null && !props.getServiceRoleKey().isBlank()) ? props.getServiceRoleKey() : props.getAnonKey(); }
}
