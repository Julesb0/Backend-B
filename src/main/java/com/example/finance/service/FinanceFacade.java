package com.example.finance.service;

import com.example.auth.SupabaseProperties;
import com.example.finance.config.FinanceTablesProperties;
import com.example.finance.config.AiProperties;
import com.example.finance.domain.Expense;
import com.example.finance.domain.Income;
import com.example.finance.domain.MicroExpense;
import com.example.finance.domain.Recommendation;
import com.example.finance.patterns.composite.TransactionGroup;
import com.example.finance.patterns.composite.TransactionLeaf;
import com.example.finance.patterns.observer.ExpenseNotifier;
import com.example.finance.patterns.strategy.DailyLimitStrategy;
import com.example.finance.patterns.strategy.FixedDailyLimit;
import com.example.finance.repository.InMemoryRepository;
import com.example.finance.repository.SupabaseRepository;
import com.example.finance.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public class FinanceFacade {
  private final TransactionRepository<Income> incomeRepo;
  private final TransactionRepository<Expense> expenseRepo;
  private final TransactionRepository<MicroExpense> microRepo;
  private final ExpenseNotifier notifier = new ExpenseNotifier();
  private final DailyLimitStrategy limitStrategy = new FixedDailyLimit(new BigDecimal("20"));
  private final AiAdvisor aiAdvisor;

  public FinanceFacade(SupabaseProperties props, FinanceTablesProperties tables, AiProperties aiProps) {
    boolean useSupabase = props != null && props.getUrl() != null && !props.getUrl().isBlank() && props.getServiceRoleKey() != null && !props.getServiceRoleKey().isBlank();
    if (useSupabase) {
      String userCol = tables.getUserColumn();
      this.incomeRepo = new SupabaseRepository<>(props, tables.getIncome(), userCol, Income.class);
      this.expenseRepo = new SupabaseRepository<>(props, tables.getExpenses(), userCol, Expense.class);
      this.microRepo = new SupabaseRepository<>(props, tables.getMicro(), userCol, MicroExpense.class);
    } else {
      this.incomeRepo = new InMemoryRepository<>();
      this.expenseRepo = new InMemoryRepository<>();
      this.microRepo = new InMemoryRepository<>();
    }
    this.aiAdvisor = new AiAdvisor(aiProps);
  }

  public Income addIncome(String userId, BigDecimal amount, LocalDate date, String description) {
    Income i = new Income();
    i.setUserId(userId);
    i.setAmount(amount);
    i.setDate(date);
    i.setDescription(description);
    return incomeRepo.save(i);
  }

  public Expense addExpense(String userId, BigDecimal amount, String type, LocalDate date, String description, boolean recurring) {
    Expense e = new Expense();
    e.setUserId(userId);
    e.setAmount(amount);
    e.setType(type);
    e.setDate(date);
    e.setDescription(description);
    e.setRecurring(recurring);
    var saved = expenseRepo.save(e);
    notifier.notifyAdded(userId, amount);
    return saved;
  }

  public MicroExpense addMicroExpense(String userId, BigDecimal amount, LocalDate date, String description) {
    MicroExpense m = new MicroExpense();
    m.setUserId(userId);
    m.setAmount(amount);
    m.setDate(date);
    m.setDescription(description);
    m.setDailyLimit(limitStrategy.limitFor(userId).intValue());
    return microRepo.save(m);
  }

  public Map<String, Object> dashboard(String userId, int year, int month) {
    var incomes = incomeRepo.listByUserAndMonth(userId, year, month);
    var expenses = expenseRepo.listByUserAndMonth(userId, year, month);
    BigDecimal totalInc = incomes.stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalExp = expenses.stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal balance = totalInc.subtract(totalExp);
    BigDecimal ratio = totalInc.compareTo(BigDecimal.ZERO) > 0 ? totalExp.divide(totalInc, 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

    List<Map<String, Object>> last6 = new ArrayList<>();
    LocalDate now = LocalDate.of(year, month, 1);
    for (int i = 5; i >= 0; i--) {
      LocalDate d = now.minusMonths(i);
      var incM = incomeRepo.listByUserAndMonth(userId, d.getYear(), d.getMonthValue()).stream().map(Income::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
      var expM = expenseRepo.listByUserAndMonth(userId, d.getYear(), d.getMonthValue()).stream().map(Expense::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
      Map<String, Object> row = new HashMap<>();
      row.put("month", d.getMonthValue());
      row.put("income", incM);
      row.put("expense", expM);
      last6.add(row);
    }

    List<Recommendation> recs = recommendations(userId, totalInc, totalExp);

    Map<String, Object> out = new HashMap<>();
    out.put("income", totalInc);
    out.put("expense", totalExp);
    out.put("balance", balance);
    out.put("ratio", ratio);
    out.put("series", last6);
    out.put("recommendations", recs);
    return out;
  }

  public List<com.example.finance.web.dto.TransactionView> history(String userId) {
    List<com.example.finance.web.dto.TransactionView> out = new ArrayList<>();
    for (var i : incomeRepo.listByUser(userId)) {
      var v = new com.example.finance.web.dto.TransactionView();
      v.setId(i.getId()); v.setAmount(i.getAmount()); v.setDate(i.getDate()); v.setDescription(i.getDescription()); v.setUserId(i.getUserId()); v.setKind("ingreso");
      out.add(v);
    }
    for (var e : expenseRepo.listByUser(userId)) {
      var v = new com.example.finance.web.dto.TransactionView();
      v.setId(e.getId()); v.setAmount(e.getAmount()); v.setDate(e.getDate()); v.setDescription(e.getDescription()); v.setUserId(e.getUserId()); v.setKind("gasto"); v.setType(e.getType()); v.setRecurring(e.isRecurring());
      out.add(v);
    }
    for (var m : microRepo.listByUser(userId)) {
      var v = new com.example.finance.web.dto.TransactionView();
      v.setId(m.getId()); v.setAmount(m.getAmount()); v.setDate(m.getDate()); v.setDescription(m.getDescription()); v.setUserId(m.getUserId()); v.setKind("microgasto");
      out.add(v);
    }
    return out;
  }

  public java.util.List<String> nimAi(String userId, int year, int month) {
    var incomes = incomeRepo.listByUserAndMonth(userId, year, month);
    var expenses = expenseRepo.listByUserAndMonth(userId, year, month);
    var micros = microRepo.listByUserAndMonth(userId, year, month);
    java.math.BigDecimal inc = incomes.stream().map(Income::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    java.math.BigDecimal exp = expenses.stream().map(Expense::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    java.math.BigDecimal mic = micros.stream().map(MicroExpense::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    java.util.List<String> tips = new java.util.ArrayList<>();
    if (inc.compareTo(java.math.BigDecimal.ZERO) > 0) {
      java.math.BigDecimal ratio = exp.divide(inc, 2, java.math.RoundingMode.HALF_UP);
      if (ratio.compareTo(new java.math.BigDecimal("0.7")) >= 0) tips.add("Gastos altos respecto a ingresos; considera recortar 10% en categorías mayores");
      if (ratio.compareTo(new java.math.BigDecimal("0.4")) <= 0) tips.add("Buen control de gastos; evalúa aumentar ahorro");
    }
    if (mic.compareTo(new java.math.BigDecimal("50")) > 0) tips.add("Microgastos elevados; fija un límite diario más bajo");
    java.math.BigDecimal balance = inc.subtract(exp);
    if (balance.compareTo(new java.math.BigDecimal("100")) > 0) tips.add("Tienes balance positivo; podrías destinar parte a inversión básica");
    java.util.Map<String, java.math.BigDecimal> byType = new java.util.HashMap<>();
    for (var e : expenses) byType.merge(e.getType() == null ? "Otros" : e.getType(), e.getAmount(), java.math.BigDecimal::add);
    if (!byType.isEmpty()) {
      var top = byType.entrySet().stream().sorted(java.util.Map.Entry.<String, java.math.BigDecimal>comparingByValue().reversed()).findFirst();
      top.ifPresent(t -> tips.add("Mayor gasto en " + t.getKey() + "; busca reducirlo"));
    }
    return tips;
  }

  public java.util.Map<String, Object> nimAi6(String userId, int year, int month) {
    java.util.List<java.util.Map<String, Object>> months = new java.util.ArrayList<>();
    java.util.Map<String, java.util.List<java.math.BigDecimal>> byCatSeries = new java.util.HashMap<>();
    for (int i = 5; i >= 0; i--) {
      java.time.LocalDate d = java.time.LocalDate.of(year, month, 1).minusMonths(i);
      var incM = incomeRepo.listByUserAndMonth(userId, d.getYear(), d.getMonthValue()).stream().map(Income::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
      var expList = expenseRepo.listByUserAndMonth(userId, d.getYear(), d.getMonthValue());
      var expM = expList.stream().map(Expense::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
      var micM = microRepo.listByUserAndMonth(userId, d.getYear(), d.getMonthValue()).stream().map(MicroExpense::getAmount).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
      java.util.Map<String, Object> row = new java.util.HashMap<>();
      row.put("year", d.getYear()); row.put("month", d.getMonthValue());
      row.put("income", incM); row.put("expense", expM); row.put("micro", micM);
      months.add(row);
      for (var e : expList) {
        String k = e.getType() == null ? "Otros" : e.getType();
        byCatSeries.computeIfAbsent(k, kk -> new java.util.ArrayList<>()).add(e.getAmount());
      }
      for (var k : byCatSeries.keySet()) {
        var series = byCatSeries.get(k);
        while (series.size() < months.size()) series.add(java.math.BigDecimal.ZERO);
      }
    }
    java.util.List<java.util.Map<String, Object>> trends = new java.util.ArrayList<>();
    for (var entry : byCatSeries.entrySet()) {
      var s = entry.getValue();
      java.math.BigDecimal prev = s.subList(0, Math.min(3, s.size())).stream().reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
      java.math.BigDecimal last = s.subList(Math.max(0, s.size()-3), s.size()).stream().reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
      String dir = last.compareTo(prev) > 0 ? "sube" : (last.compareTo(prev) < 0 ? "baja" : "igual");
      java.util.Map<String, Object> t = new java.util.HashMap<>();
      t.put("category", entry.getKey()); t.put("prev3", prev); t.put("last3", last); t.put("trend", dir);
      trends.add(t);
    }
    var tips = nimAi(userId, year, month);
    StringBuilder summary = new StringBuilder();
    summary.append("Meses:");
    for (var m : months) summary.append(m.get("year")).append("-").append(m.get("month")).append(" inc ").append(m.get("income")).append(" exp ").append(m.get("expense")).append("; ");
    summary.append("Trends:");
    for (var t : trends) summary.append(t.get("category")).append(" ").append(t.get("trend")).append("; ");
    var ai = aiAdvisor.advise(summary.toString());
    if (!ai.isEmpty()) tips.addAll(ai);
    java.util.Map<String, Object> out = new java.util.HashMap<>();
    out.put("months", months); out.put("trends", trends); out.put("tips", tips);
    return out;
  }

  private List<Recommendation> recommendations(String userId, BigDecimal totalInc, BigDecimal totalExp) {
    List<Recommendation> recs = new ArrayList<>();
    TransactionGroup g = new TransactionGroup().add(new TransactionLeaf(totalInc)).add(new TransactionLeaf(totalExp.negate()));
    BigDecimal net = g.total();
    Recommendation r = new Recommendation();
    r.setId(UUID.randomUUID().toString());
    r.setUserId(userId);
    r.setType("balance");
    r.setDescription("Balance neto " + net);
    r.setPotentialSavings(totalExp.compareTo(BigDecimal.ZERO) > 0 ? totalExp.multiply(new BigDecimal("0.10")).toPlainString() : "0");
    recs.add(new com.example.finance.patterns.decorator.PotentialSavingsDecorator(r).build());
    return recs;
  }

  public boolean deleteTransaction(String kind, String id) {
    if (kind == null || id == null || id.isBlank()) return false;
    String k = kind.toLowerCase();
    if (k.equals("ingreso")) return incomeRepo.deleteById(id);
    if (k.equals("gasto")) return expenseRepo.deleteById(id);
    if (k.equals("microgasto")) return microRepo.deleteById(id);
    return false;
  }

  public boolean deleteAnyTransaction(String id) {
    if (id == null || id.isBlank()) return false;
    if (incomeRepo.deleteById(id)) return true;
    if (expenseRepo.deleteById(id)) return true;
    return microRepo.deleteById(id);
  }
}
