package com.example.finance.repository;

import com.example.finance.domain.Transaction;
import java.util.List;

public interface TransactionRepository<T extends Transaction> {
  T save(T t);
  List<T> listByUser(String userId);
  List<T> listByUserAndMonth(String userId, int year, int month);
  boolean deleteById(String id);
}

