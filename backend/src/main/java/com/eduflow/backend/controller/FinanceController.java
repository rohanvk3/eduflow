package com.eduflow.backend.controller;

import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.FinanceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.*;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/fees")
public class FinanceController {
    private final FinanceService finance;private final FinanceReportService reports;private final FinanceReceiptService receipts;
    public FinanceController(FinanceService finance,FinanceReportService reports,FinanceReceiptService receipts) {this.finance=finance;this.reports=reports;this.receipts=receipts;}
    @PostMapping("/structures") public ResponseEntity<StructureResponse> createStructure(@Valid @RequestBody StructureRequest r) {
        var result=finance.saveStructure(null,r);return ResponseEntity.created(URI.create("/api/fees/structures/"+result.id())).body(result);
    }
    @GetMapping("/structures") public PageResponse<StructureResponse> structures(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,
        @RequestParam(defaultValue="") String search,@RequestParam(required=false) Long courseId,@RequestParam(required=false) StudentStatus status,@RequestParam(defaultValue="effectiveFrom,desc") String sort) {
        return finance.structures(page,size,search,courseId,status,sort);
    }
    @GetMapping("/structures/{id}") public StructureResponse structure(@PathVariable Long id) {return finance.structure(id);}
    @PutMapping("/structures/{id}") public StructureResponse updateStructure(@PathVariable Long id,@Valid @RequestBody StructureRequest r) {return finance.saveStructure(id,r);}
    @PatchMapping("/structures/{id}/deactivate") public StructureResponse deactivate(@PathVariable Long id) {return finance.deactivateStructure(id);}
    @PostMapping("/accounts") public ResponseEntity<AccountDetails> createAccount(@Valid @RequestBody AccountRequest r) {
        var result=finance.createAccount(r);return ResponseEntity.created(URI.create("/api/fees/accounts/"+result.account().id())).body(result);
    }
    @GetMapping("/accounts") public PageResponse<AccountResponse> accounts(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,
        @RequestParam(defaultValue="") String search,@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long batchId,@RequestParam(required=false) Long studentId,
        @RequestParam(required=false) FeeAccountStatus status,@RequestParam(defaultValue="id,desc") String sort) {
        return finance.accounts(page,size,search,courseId,batchId,studentId,status,sort);
    }
    @GetMapping("/accounts/{id}") public AccountDetails account(@PathVariable Long id) {return finance.account(id);}
    @GetMapping("/students/{studentId}") public PageResponse<AccountResponse> student(@PathVariable Long studentId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {return finance.studentAccounts(studentId,page,size);}
    @GetMapping("/me") public PersonalFees mine(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {return finance.mine(page,size);}
    @PostMapping("/accounts/{id}/installments") public AccountDetails schedule(@PathVariable Long id,@Valid @RequestBody ScheduleRequest r) {return finance.createSchedule(id,r);}
    @GetMapping("/accounts/{id}/installments") public List<InstallmentResponse> installments(@PathVariable Long id) {return finance.installments(id);}
    @PutMapping("/installments/{id}") public AccountDetails updateInstallment(@PathVariable Long id,@Valid @RequestBody InstallmentUpdate r) {return finance.updateInstallment(id,r);}
    @PostMapping("/payments") public ResponseEntity<PaymentResponse> payment(@Valid @RequestBody PaymentRequest r) {
        var result=finance.recordPayment(r);return ResponseEntity.status(result.replayed()?200:201)
            .location(URI.create("/api/fees/payments/"+result.payment().id())).body(result.payment());
    }
    @GetMapping("/payments") public PageResponse<PaymentResponse> payments(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size,
        @RequestParam(defaultValue="") String search,@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long batchId,@RequestParam(required=false) Long studentId,@RequestParam(required=false) Long feeAccountId,
        @RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(required=false) FeePaymentStatus status,@RequestParam(defaultValue="paymentDate,desc") String sort) {
        return finance.paymentPage(page,size,search,courseId,batchId,studentId,feeAccountId,fromDate,toDate,status,sort);
    }
    @GetMapping("/payments/{id}") public PaymentResponse payment(@PathVariable Long id) {return finance.payment(id);}
    @PostMapping("/payments/{id}/void") public PaymentResponse voidPayment(@PathVariable Long id,@Valid @RequestBody VoidRequest r) {return finance.voidPayment(id,r);}
    @GetMapping("/payments/{id}/receipt") public ResponseEntity<byte[]> receipt(@PathVariable Long id) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"eduflow-receipt-"+id+".pdf\"").body(receipts.receipt(id));
    }
    @GetMapping({"/dashboard","/reports"}) public FinanceReport report(@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long batchId,
        @RequestParam(required=false) Long studentId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return reports.report(courseId,batchId,studentId,fromDate,toDate,page,size);
    }
    @GetMapping("/reports/export") public ResponseEntity<byte[]> export(@RequestParam(required=false) Long courseId,@RequestParam(required=false) Long batchId,
        @RequestParam(required=false) Long studentId,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"eduflow-finance.csv\"").body(reports.export(courseId,batchId,studentId,fromDate,toDate));
    }
}
