package com.example.finance.repository;

import com.example.finance.domain.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class InMemoryRepository<T extends Transaction> implements TransactionRepository<T> {
  private final List<T> store = new ArrayList<>();

  @Override
  public synchronized T save(T t) {
    if (t.getId() == null) t.setId(UUID.randomUUID().toString());
    store.add(t);
    return t;
  }

  @Override
  public synchronized List<T> listByUser(String userId) {
    return store.stream().filter(x -> userId.equals(x.getUserId())).collect(Collectors.toList());
  }

  @Override
  public synchronized List<T> listByUserAndMonth(String userId, int year, int month) {
    return store.stream().filter(x -> userId.equals(x.getUserId()) && x.getDate().getYear() == year && x.getDate().getMonthValue() == month).collect(Collectors.toList());
  }

  @Override
  public synchronized boolean deleteById(String id) {
    return store.removeIf(x -> id.equals(x.getId()));
  }
}

