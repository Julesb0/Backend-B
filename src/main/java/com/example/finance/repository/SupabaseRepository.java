package com.example.finance.repository;

import com.example.auth.SupabaseProperties;
import com.example.finance.domain.Expense;
import com.example.finance.domain.MicroExpense;
import com.example.finance.domain.Transaction;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class SupabaseRepository<T extends Transaction> implements TransactionRepository<T> {
  private final SupabaseProperties props;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final String table;
  private final String userColumn;
  private final Class<T> clazz;
  private final ObjectMapper mapper = new ObjectMapper();

  public SupabaseRepository(SupabaseProperties props, String table, String userColumn, Class<T> clazz) {
    this.props = props;
    this.table = table;
    this.userColumn = userColumn;
    this.clazz = clazz;
  }

  @Override
  public T save(T t) {
    try {
      String json = toJson(t);
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
          JsonNode n = arr.get(0);
          fillFromJson(t, n);
        }
        return t;
      }
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Supabase save failed: " + res.body());
    } catch (Exception e) {
      if (e instanceof org.springframework.web.server.ResponseStatusException) throw (org.springframework.web.server.ResponseStatusException) e;
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @Override
  public List<T> listByUser(String userId) {
    return query(userColumn + "=eq." + encode(userId));
  }

  @Override
  public List<T> listByUserAndMonth(String userId, int year, int month) {
    String from = String.format("%04d-%02d-01", year, month);
    java.time.YearMonth ym = java.time.YearMonth.of(year, month);
    String to = String.format("%04d-%02d-%02d", year, month, ym.lengthOfMonth());
    return query(userColumn + "=eq." + encode(userId) + "&date=gte." + from + "&date=lte." + to);
  }

  private List<T> query(String filter) {
    try {
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getUrl() + "/rest/v1/" + table + "?" + filter))
        .header("apikey", apiKey())
        .header("Authorization", "Bearer " + apiKey())
        .build();
      HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      if (res.statusCode() >= 200 && res.statusCode() < 300) {
        List<T> list = new ArrayList<>();
        JsonNode arr = mapper.readTree(res.body());
        if (arr.isArray()) {
          for (JsonNode n : arr) {
            T obj = newInstance();
            fillFromJson(obj, n);
            list.add(obj);
          }
        }
        return list;
      }
      throw new RuntimeException("Supabase query failed: " + res.body());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String toJson(T t) {
    String date = t.getDate().format(DateTimeFormatter.ISO_DATE);
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    if (t.getId() == null || t.getId().isEmpty()) {
      String gen = java.util.UUID.randomUUID().toString();
      t.setId(gen);
    }
    sb.append("\"id\":\"").append(t.getId()).append("\",");
    sb.append("\"amount\":").append(t.getAmount()).append(",");
    sb.append("\"date\":\"").append(date).append("\",");
    sb.append("\"description\":\"").append(escape(t.getDescription())).append("\",");
    sb.append("\"user_id\":\"").append(escape(t.getUserId())).append("\"");
    if (t instanceof Expense e) {
      sb.append(",\"type\":\"").append(escape(e.getType())).append("\"");
      sb.append(",\"recurring\":").append(e.isRecurring());
    }
    if (t instanceof MicroExpense m) {
      if (m.getDailyLimit() != null) {
        sb.append(",\"daily_limit\":").append(m.getDailyLimit());
      }
    }
    sb.append("}");
    return sb.toString();
  }

  private String escape(String s) { return s == null ? "" : s.replace("\"","\\\""); }
  private String encode(String s) { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
  private String apiKey() { return (props.getServiceRoleKey() != null && !props.getServiceRoleKey().isBlank()) ? props.getServiceRoleKey() : props.getAnonKey(); }

  private T newInstance() {
    try { return clazz.getDeclaredConstructor().newInstance(); } catch (Exception e) { throw new RuntimeException(e); }
  }

  private void fillFromJson(T t, JsonNode n) {
    if (n.get("id") != null) t.setId(n.get("id").asText());
    if (n.get("amount") != null) t.setAmount(new java.math.BigDecimal(n.get("amount").asText()));
    if (n.get("date") != null) t.setDate(LocalDate.parse(n.get("date").asText()));
    if (n.get("description") != null) t.setDescription(n.get("description").asText());
    if (n.get("user_id") != null) t.setUserId(n.get("user_id").asText());
    if (t instanceof Expense e) {
      if (n.get("type") != null) e.setType(n.get("type").asText());
      if (n.get("recurring") != null) e.setRecurring(n.get("recurring").asBoolean());
    }
    if (t instanceof MicroExpense m) {
      if (n.get("daily_limit") != null) m.setDailyLimit(n.get("daily_limit").asInt());
    }
  }

  @Override
  public boolean deleteById(String id) {
    try {
      HttpRequest req = HttpRequest.newBuilder()
        .uri(URI.create(props.getUrl() + "/rest/v1/" + table + "?id=eq." + encode(id)))
        .header("apikey", apiKey())
        .header("Authorization", "Bearer " + apiKey())
        .header("Prefer", "return=minimal")
        .DELETE()
        .build();
      HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
      return res.statusCode() >= 200 && res.statusCode() < 300;
    } catch (Exception e) {
      return false;
    }
  }
}
