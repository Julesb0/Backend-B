package com.example.finance.web;

import com.example.auth.SupabaseProperties;
import com.example.auth.security.AuthGuard;
import com.example.auth.security.JwtProperties;
import com.example.finance.config.FinanceTablesProperties;
import com.example.finance.config.AiProperties;
import com.example.finance.domain.Expense;
import com.example.finance.domain.Income;
import com.example.finance.domain.MicroExpense;
import com.example.finance.service.FinanceFacade;
import com.example.finance.web.dto.ExpenseCreateDto;
import com.example.finance.web.dto.IncomeCreateDto;
import com.example.finance.web.dto.MicroExpenseCreateDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
  private final FinanceFacade facade;
  private final AuthGuard guard;

  public FinanceController(SupabaseProperties props, JwtProperties jwtProps, FinanceTablesProperties tables, AiProperties aiProps) {
    this.facade = new FinanceFacade(props, tables, aiProps);
    this.guard = new AuthGuard(jwtProps, props);
  }

  @PostMapping("/incomes")
  public ResponseEntity<Income> addIncome(@RequestHeader(value = "Authorization", required = false) String auth,
                                          @Valid @RequestBody IncomeCreateDto dto) {
    String subject = guard.require(auth);
    String user = subject != null ? subject : dto.getUserEmail();
    var res = facade.addIncome(user, dto.getAmount(), dto.getDate(), dto.getDescription());
    return ResponseEntity.status(201).body(res);
  }

  @PostMapping("/expenses")
  public ResponseEntity<Expense> addExpense(@RequestHeader(value = "Authorization", required = false) String auth,
                                            @Valid @RequestBody ExpenseCreateDto dto) {
    String subject = guard.require(auth);
    String user = subject != null ? subject : dto.getUserEmail();
    var res = facade.addExpense(user, dto.getAmount(), dto.getType(), dto.getDate(), dto.getDescription(), dto.isRecurring());
    return ResponseEntity.status(201).body(res);
  }

  @PostMapping("/microexpenses")
  public ResponseEntity<MicroExpense> addMicro(@RequestHeader(value = "Authorization", required = false) String auth,
                                               @Valid @RequestBody MicroExpenseCreateDto dto) {
    String subject = guard.require(auth);
    String user = subject != null ? subject : dto.getUserEmail();
    var res = facade.addMicroExpense(user, dto.getAmount(), dto.getDate(), dto.getDescription());
    return ResponseEntity.status(201).body(res);
  }

  @GetMapping("/dashboard")
  public ResponseEntity<Map<String, Object>> dashboard(@RequestHeader(value = "Authorization", required = false) String auth,
                                                       @RequestParam String userEmail,
                                                       @RequestParam int year,
                                                       @RequestParam int month) {
    guard.require(auth);
    var res = facade.dashboard(userEmail, year, month);
    return ResponseEntity.ok(res);
  }

  @GetMapping("/transactions")
  public ResponseEntity<java.util.List<com.example.finance.web.dto.TransactionView>> history(@RequestHeader(value = "Authorization", required = false) String auth,
                                                                                             @RequestParam String userEmail) {
    guard.require(auth);
    var res = facade.history(userEmail);
    return ResponseEntity.ok(res);
  }

  @DeleteMapping("/transactions/{id}")
  public ResponseEntity<Void> delete(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @PathVariable String id,
                                     @RequestParam(required = false) String kind) {
    guard.require(auth);
    boolean ok = (kind == null || kind.isBlank()) ? facade.deleteAnyTransaction(id) : facade.deleteTransaction(kind, id);
    if (!ok) ok = facade.deleteAnyTransaction(id);
    if (ok) return ResponseEntity.noContent().build();
    return ResponseEntity.notFound().build();
  }

  @GetMapping("/nimai")
  public ResponseEntity<java.util.List<String>> nimAi(@RequestHeader(value = "Authorization", required = false) String auth,
                                                      @RequestParam String userEmail,
                                                      @RequestParam int year,
                                                      @RequestParam int month) {
    guard.require(auth);
    var res = facade.nimAi(userEmail, year, month);
    return ResponseEntity.ok(res);
  }

  @GetMapping("/nimai6")
  public ResponseEntity<java.util.Map<String, Object>> nimAi6(@RequestHeader(value = "Authorization", required = false) String auth,
                                                              @RequestParam String userEmail,
                                                              @RequestParam int year,
                                                              @RequestParam int month) {
    guard.require(auth);
    var res = facade.nimAi6(userEmail, year, month);
    return ResponseEntity.ok(res);
  }
}
