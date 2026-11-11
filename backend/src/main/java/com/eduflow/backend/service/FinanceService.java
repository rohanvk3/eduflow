package com.eduflow.backend.service;

import com.eduflow.backend.dto.PageResponse;
import com.eduflow.backend.dto.FinanceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.exception.*;
import com.eduflow.backend.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import jakarta.persistence.criteria.Predicate;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
@Transactional(readOnly=true)
@PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
public class FinanceService {
    @org.springframework.beans.factory.annotation.Autowired private AuditRecorder adminAudit;
    private final FeeStructureRepository structures;
    private final StudentFeeAccountRepository accounts;
    private final FeeInstallmentRepository installments;
    private final FeePaymentRepository payments;
    private final PaymentAllocationRepository allocations;
    private final PaymentVoidAuditRepository audits;
    private final StudentRepository students;
    private final UserRepository users;
    private final AcademicAccess access;
    private final EntityManager em;
    private final ObjectMapper mapper;
    private final ParentAccess parentAccess;
    private final NotificationOutbox notifications;
    public FinanceService(FeeStructureRepository structures,StudentFeeAccountRepository accounts,
            FeeInstallmentRepository installments,FeePaymentRepository payments,PaymentAllocationRepository allocations,
            PaymentVoidAuditRepository audits,StudentRepository students,UserRepository users,
            AcademicAccess access,EntityManager em,ObjectMapper mapper,ParentAccess parentAccess,NotificationOutbox notifications) {
        this.structures=structures;this.accounts=accounts;this.installments=installments;this.payments=payments;
        this.allocations=allocations;this.audits=audits;this.students=students;this.users=users;this.access=access;this.em=em;this.mapper=mapper;this.parentAccess=parentAccess;this.notifications=notifications;
    }
    @PreAuthorize("hasRole('PARENT')")
    public AccountDetails parentAccount(Long studentId,Long accountId) {
        parentAccess.child(studentId);var account=find(StudentFeeAccount.class,accountId,"Fee account",false);
        if(!account.getStudent().getId().equals(studentId))throw new AccessDeniedException("Payment/account does not belong to this child");
        return details(account);
    }
    @PreAuthorize("hasRole('PARENT')")
    public PaymentResponse parentPayment(Long studentId,Long paymentId) {
        parentAccess.child(studentId);var p=find(FeePayment.class,paymentId,"Payment",false);
        if(!p.getFeeAccount().getStudent().getId().equals(studentId))throw new AccessDeniedException("Payment does not belong to this child");
        return paymentResponse(p);
    }
    public static BigDecimal money(BigDecimal value) {
        if(value==null || value.signum()<0 || value.compareTo(new BigDecimal("999999999999.99"))>0)
            throw new IllegalArgumentException("Amount must be between 0 and 999999999999.99");
        try {return value.setScale(2,RoundingMode.UNNECESSARY);}
        catch(ArithmeticException ex) {throw new IllegalArgumentException("Amounts may have at most two decimal places");}
    }
    static BigDecimal zero() {return new BigDecimal("0.00");}
    private <T> T find(Class<T> type,Long id,String label,boolean lock) {
        T value=lock?em.find(type,id,LockModeType.PESSIMISTIC_WRITE):em.find(type,id);
        if(value==null) throw new ResourceNotFoundException(label+" not found");
        return value;
    }
    public static Pageable paging(int page,int size,String sort,Set<String> allowed) {
        if(page<0 || size<1 || size>100) throw new IllegalArgumentException("Page must be non-negative and size must be between 1 and 100");
        String[] parts=sort.split(",");
        if(parts.length!=2 || !allowed.contains(parts[0]) || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc")))
            throw new IllegalArgumentException("Unsupported sort; use an allowed field followed by ,asc or ,desc");
        return PageRequest.of(page,size,Sort.by(Sort.Direction.fromString(parts[1]),parts[0]).and(Sort.by("id")));
    }
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<StructureResponse> structures(int page,int size,String search,Long courseId,StudentStatus status,String sort) {
        CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
        return PageResponse.from(structures.findAll((r,q,cb)->{
            var p=cb.or(cb.like(cb.lower(r.get("name")),pattern,'\\'),cb.like(cb.lower(r.get("course").get("name")),pattern,'\\'));
            if(courseId!=null)p=cb.and(p,cb.equal(r.get("course").get("id"),courseId));
            if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));return p;
        },paging(page,size,sort,Set.of("id","name","effectiveFrom","totalFee"))).map(this::structureResponse));
    }
    @PreAuthorize("hasRole('ADMIN')") public StructureResponse structure(Long id) {return structureResponse(find(FeeStructure.class,id,"Fee structure",false));}
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public StructureResponse saveStructure(Long id,StructureRequest r) {
        Course c=find(Course.class,r.courseId(),"Course",true);
        FeeStructure f=id==null?new FeeStructure():find(FeeStructure.class,id,"Fee structure",true);
        if(id!=null && !f.getCourse().getId().equals(c.getId()))throw new FinanceConflictException("A fee structure's course cannot be changed");
        if(r.effectiveTo()!=null && r.effectiveTo().isBefore(r.effectiveFrom()))throw new IllegalArgumentException("Effective end must not precede the start");
        BigDecimal total=money(r.tuitionFee().add(r.admissionFee()).add(r.materialFee()).add(r.otherFee()));
        if(id!=null && accounts.existsByFeeStructureId(id) &&
            (!f.getTuitionFee().equals(money(r.tuitionFee())) || !f.getAdmissionFee().equals(money(r.admissionFee())) ||
             !f.getMaterialFee().equals(money(r.materialFee())) || !f.getOtherFee().equals(money(r.otherFee())) ||
             !f.getEffectiveFrom().equals(r.effectiveFrom()) || !Objects.equals(f.getEffectiveTo(),r.effectiveTo())))
            throw new FinanceConflictException("This structure has fee agreements. Preserve it and create a new structure for revised fees or dates");
        if(r.status()==StudentStatus.ACTIVE && structures.overlapping(c.getId(),id==null?0L:id,r.effectiveFrom(),r.effectiveTo())>0)
            throw new FinanceConflictException("An active fee structure already applies to this course during these dates");
        f.setCourse(c);f.setName(r.name().strip());f.setTuitionFee(money(r.tuitionFee()));f.setAdmissionFee(money(r.admissionFee()));
        f.setMaterialFee(money(r.materialFee()));f.setOtherFee(money(r.otherFee()));f.setTotalFee(total);
        f.setEffectiveFrom(r.effectiveFrom());f.setEffectiveTo(r.effectiveTo());f.setStatus(r.status());
        structures.saveAndFlush(f);return structureResponse(f);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public StructureResponse deactivateStructure(Long id) {
        FeeStructure f=find(FeeStructure.class,id,"Fee structure",false);
        find(Course.class,f.getCourse().getId(),"Course",true);
        em.refresh(f,LockModeType.PESSIMISTIC_WRITE);f.setStatus(StudentStatus.INACTIVE);structures.flush();return structureResponse(f);
    }
    private StructureResponse structureResponse(FeeStructure f) {
        return new StructureResponse(f.getId(),f.getCourse().getId(),f.getCourse().getName(),f.getName(),f.getTuitionFee(),
            f.getAdmissionFee(),f.getMaterialFee(),f.getOtherFee(),f.getTotalFee(),f.getEffectiveFrom(),f.getEffectiveTo(),f.getStatus());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public AccountDetails createAccount(AccountRequest r) {
        var enrollment=find(BatchEnrollment.class,r.enrollmentId(),"Enrollment",true);
        if(accounts.existsByEnrollmentId(enrollment.getId()))throw new FinanceConflictException("This enrollment already has a fee account");
        if(enrollment.getStatus()!=EnrollmentStatus.ACTIVE)throw new FinanceConflictException("New fee agreements require an active enrollment");
        var f=find(FeeStructure.class,r.feeStructureId(),"Fee structure",true);
        if(f.getStatus()!=StudentStatus.ACTIVE || !f.getCourse().getId().equals(enrollment.getBatch().getCourse().getId()))
            throw new IllegalArgumentException("Choose an active fee structure for the enrolled course");
        if(r.agreementDate().isAfter(BatchService.today()) || r.agreementDate().isBefore(f.getEffectiveFrom()) ||
            (f.getEffectiveTo()!=null && r.agreementDate().isAfter(f.getEffectiveTo())))
            throw new IllegalArgumentException("Agreement date must be within the structure's applicability period and cannot be in the future");
        BigDecimal discount=money(r.discountAmount());
        if(discount.compareTo(f.getTotalFee())>0)throw new IllegalArgumentException("Discount cannot exceed the agreed original fee");
        StudentFeeAccount a=new StudentFeeAccount();a.setStudent(enrollment.getStudent());a.setEnrollment(enrollment);a.setFeeStructure(f);
        a.setStructureName(f.getName());a.setAgreementDate(r.agreementDate());a.setTuitionFee(f.getTuitionFee());
        a.setAdmissionFee(f.getAdmissionFee());a.setMaterialFee(f.getMaterialFee());a.setOtherFee(f.getOtherFee());
        a.setOriginalAmount(f.getTotalFee());a.setDiscountAmount(discount);a.setFinalPayableAmount(money(f.getTotalFee().subtract(discount)));
        accounts.saveAndFlush(a);return details(a);
    }
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<AccountResponse> accounts(int page,int size,String search,Long courseId,Long batchId,Long studentId,FeeAccountStatus status,String sort) {
        return accountPage(page,size,search,courseId,batchId,studentId,status,sort);
    }
    private PageResponse<AccountResponse> accountPage(int page,int size,String search,Long courseId,Long batchId,Long studentId,FeeAccountStatus status,String sort) {
        CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
        var result=accounts.findAll((r,q,cb)->{
            Predicate p=cb.or(cb.like(cb.lower(r.get("student").get("fullName")),pattern,'\\'),
                cb.like(cb.lower(r.get("student").get("admissionNumber")),pattern,'\\'));
            if(courseId!=null)p=cb.and(p,cb.equal(r.get("enrollment").get("batch").get("course").get("id"),courseId));
            if(batchId!=null)p=cb.and(p,cb.equal(r.get("enrollment").get("batch").get("id"),batchId));
            if(studentId!=null)p=cb.and(p,cb.equal(r.get("student").get("id"),studentId));
            if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));return p;
        },paging(page,size,sort,Set.of("id","createdAt","finalPayableAmount","agreementDate")));
        var ids=result.getContent().stream().map(StudentFeeAccount::getId).toList();
        if(ids.isEmpty())return PageResponse.from(result.map(this::accountResponse));
        var paid=totals(payments.successfulTotals(ids));
        var overdue=totals(installments.overdueTotals(ids,BatchService.today()));
        return PageResponse.from(result.map(a->accountResponse(a,paid.getOrDefault(a.getId(),zero()),overdue.getOrDefault(a.getId(),zero()))));
    }
    private Map<Long,BigDecimal> totals(List<Object[]> rows) {
        var values=new HashMap<Long,BigDecimal>();
        for(var row:rows)values.put(((Number)row[0]).longValue(),money((BigDecimal)row[1]));
        return values;
    }
    public AccountDetails account(Long id) {var a=find(StudentFeeAccount.class,id,"Fee account",false);authorize(a);return details(a);}
    public PageResponse<AccountResponse> studentAccounts(Long id,int page,int size) {
        if(!access.hasRole("ADMIN")) {
            var own=students.findByUserEmail(access.email()).orElseThrow(()->new AccessDeniedException("Student fee access denied"));
            if(!own.getId().equals(id))throw new AccessDeniedException("Student fee access denied");
        }
        return accountPage(page,size,"",null,null,id,null,"id,desc");
    }
    @PreAuthorize("hasRole('STUDENT')") public PersonalFees mine(int page,int size) {
        paging(page,size,"id,desc",Set.of("id"));
        var own=students.findByUserEmail(access.email());
        return new PersonalFees(own.isPresent(),own.map(s->accountPage(page,size,"",null,null,s.getId(),null,"id,desc"))
            .orElseGet(()->new PageResponse<>(List.of(),page,size,0,0)));
    }
    private void authorize(StudentFeeAccount a) {
        if(access.hasRole("ADMIN"))return;
        if(access.hasRole("STUDENT") && a.getStudent().getUser()!=null && a.getStudent().getUser().getEmail().equals(access.email()))return;
        throw new AccessDeniedException("Fee account access denied");
    }
    private AccountDetails details(StudentFeeAccount a) {
        return new AccountDetails(accountResponse(a),installments.findByFeeAccountIdOrderByInstallmentNumber(a.getId()).stream().map(this::installmentResponse).toList(),
            payments.findByFeeAccountIdOrderByCreatedAtDesc(a.getId()).stream().map(this::paymentResponse).toList());
    }
    private AccountResponse accountResponse(StudentFeeAccount a) {
        BigDecimal paid=money(payments.successfulTotal(a.getId())),overdue=zero();
        for(var i:installments.findByFeeAccountIdOrderByInstallmentNumber(a.getId()))
            if(i.getDueDate().isBefore(BatchService.today()))overdue=overdue.add(i.getAmount().subtract(allocations.successfulTotal(i.getId())));
        return accountResponse(a,paid,overdue);
    }
    private AccountResponse accountResponse(StudentFeeAccount a,BigDecimal paid,BigDecimal overdue) {
        BigDecimal due=money(a.getFinalPayableAmount().subtract(paid));
        var e=a.getEnrollment();var b=e.getBatch();var s=a.getStudent();
        return new AccountResponse(a.getId(),s.getId(),s.getFullName(),s.getAdmissionNumber(),e.getId(),e.getStatus(),b.getCourse().getId(),
            b.getCourse().getName(),b.getId(),b.getName(),a.getFeeStructure()==null?null:a.getFeeStructure().getId(),a.getStructureName(),a.getAgreementDate(),"INR",
            a.getTuitionFee(),a.getAdmissionFee(),a.getMaterialFee(),a.getOtherFee(),a.getOriginalAmount(),a.getDiscountAmount(),a.getFinalPayableAmount(),
            paid,due,money(overdue),due.signum()==0?"PAID":overdue.signum()>0?"OVERDUE":paid.signum()>0?"PARTIALLY_PAID":"PENDING",a.getStatus(),a.getVersion());
    }
    public List<InstallmentResponse> installments(Long id) {var a=find(StudentFeeAccount.class,id,"Fee account",false);authorize(a);
        return installments.findByFeeAccountIdOrderByInstallmentNumber(id).stream().map(this::installmentResponse).toList();}
    private InstallmentResponse installmentResponse(FeeInstallment i) {
        BigDecimal paid=money(allocations.successfulTotal(i.getId())),due=money(i.getAmount().subtract(paid));
        String status=due.signum()==0?"PAID":i.getDueDate().isBefore(BatchService.today())?"OVERDUE":paid.signum()>0?"PARTIALLY_PAID":"PENDING";
        return new InstallmentResponse(i.getId(),i.getInstallmentNumber(),i.getAmount(),i.getDueDate(),paid,due,status);
    }
    private StudentFeeAccount editableAccount(Long id,Long version) {
        var a=find(StudentFeeAccount.class,id,"Fee account",true);
        if(a.getStatus()!=FeeAccountStatus.ACTIVE)throw new FinanceConflictException("This fee account is not active");
        if(version!=null && a.getVersion()!=version)throw new FinanceConflictException("Fee account changed; reload before retrying");
        return a;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public AccountDetails createSchedule(Long id,ScheduleRequest r) {
        var a=editableAccount(id,r.version());
        if(installments.existsByFeeAccountId(id))throw new FinanceConflictException("An installment schedule already exists");
        BigDecimal sum=zero();Set<Integer> numbers=new HashSet<>();
        for(var input:r.installments()) {
            if(!numbers.add(input.installmentNumber()))throw new IllegalArgumentException("Duplicate installment number");
            if(input.dueDate().isBefore(a.getAgreementDate()))throw new IllegalArgumentException("Installment due date cannot precede the agreement date");
            sum=sum.add(money(input.amount()));
        }
        if(sum.compareTo(a.getFinalPayableAmount())!=0)throw new IllegalArgumentException("Installment schedule total must equal the final payable amount");
        for(var input:r.installments()) {
            var i=new FeeInstallment();i.setFeeAccount(a);i.setInstallmentNumber(input.installmentNumber());i.setAmount(money(input.amount()));i.setDueDate(input.dueDate());installments.save(i);
        }
        em.lock(a,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();return details(a);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public AccountDetails updateInstallment(Long id,InstallmentUpdate r) {
        var i=find(FeeInstallment.class,id,"Installment",false);var a=editableAccount(i.getFeeAccount().getId(),r.version());
        em.refresh(i);
        if(payments.existsByFeeAccountId(a.getId()))throw new FinanceConflictException("The schedule is frozen after any payment has been recorded, including voided payments");
        if(money(r.amount()).compareTo(i.getAmount())!=0)throw new FinanceConflictException("Installment amounts are fixed to preserve the agreed schedule total");
        if(r.dueDate().isBefore(a.getAgreementDate()))throw new IllegalArgumentException("Due date cannot precede the agreement");
        i.setDueDate(r.dueDate());em.lock(a,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();return details(a);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public PaymentResult recordPayment(PaymentRequest r) {
        var a=editableAccount(r.feeAccountId(),null);
        String reference=clean(r.transactionReference()),key=r.idempotencyKey().strip();
        if(key.isEmpty())throw new IllegalArgumentException("Idempotency key is required");
        Set<Long> ids=new HashSet<>();
        for(var x:r.allocations())if(!ids.add(x.installmentId()))throw new IllegalArgumentException("Duplicate installment allocations");
        String hash=fingerprint(r,reference);
        var existing=payments.findByIdempotencyKey(key);
        if(existing.isPresent()) {
            var p=existing.get();
            if(!p.getRequestHash().equals(hash) || p.getStatus()!=FeePaymentStatus.SUCCESS)
                throw new FinanceConflictException("Idempotency key was already used for a different or voided payment");
            return new PaymentResult(paymentResponse(p),true);
        }
        if(r.paymentDate().isAfter(BatchService.today()) || r.paymentDate().isBefore(a.getAgreementDate()))
            throw new IllegalArgumentException("Payment date must be on or after the agreement and cannot be in the future");
        if(r.paymentMethod()!=PaymentMethod.CASH && reference.isEmpty())throw new IllegalArgumentException("Transaction reference is required for non-cash payments");
        String external=r.paymentMethod()==PaymentMethod.CASH || reference.isEmpty()?null:r.paymentMethod().name()+":"+reference.toUpperCase(Locale.ROOT);
        if(external!=null && payments.existsByExternalReferenceKey(external))throw new FinanceConflictException("This transaction reference has already been recorded");
        BigDecimal amount=money(r.amount());
        if(amount.signum()<=0)throw new IllegalArgumentException("Payment must be positive");
        if(amount.compareTo(a.getFinalPayableAmount().subtract(payments.successfulTotal(a.getId())))>0)
            throw new FinanceConflictException("Payment exceeds the outstanding fee account balance");
        Map<Long,FeeInstallment> byId=new HashMap<>();
        for(var i:installments.findByFeeAccountIdOrderByInstallmentNumber(a.getId()))byId.put(i.getId(),i);
        BigDecimal sum=zero();
        for(var input:r.allocations()) {
            var i=byId.get(input.installmentId());if(i==null)throw new IllegalArgumentException("All allocations must belong to installments of this fee account");
            BigDecimal value=money(input.amount());if(value.signum()<=0)throw new IllegalArgumentException("Allocation must be positive");
            if(value.compareTo(i.getAmount().subtract(allocations.successfulTotal(i.getId())))>0)throw new FinanceConflictException("Allocation exceeds installment balance");
            sum=sum.add(value);
        }
        if(sum.compareTo(amount)!=0)throw new IllegalArgumentException("Allocations must sum to the payment amount");
        FeePayment p=new FeePayment();p.setFeeAccount(a);p.setAmount(amount);p.setPaymentDate(r.paymentDate());p.setPaymentMethod(r.paymentMethod());
        p.setTransactionReference(reference);p.setExternalReferenceKey(external);p.setReceiptNumber("EF-"+BatchService.today().getYear()+"-"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
        p.setIdempotencyKey(key);p.setRequestHash(hash);p.setRecordedBy(currentUser());p.setRemarks(clean(r.remarks()));payments.saveAndFlush(p);
        for(var input:r.allocations()) {var x=new PaymentAllocation();x.setPayment(p);x.setInstallment(byId.get(input.installmentId()));x.setAmount(money(input.amount()));allocations.save(x);}
        em.lock(a,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();notifications.capture("payment-recorded:"+p.getId(),NotificationType.PAYMENT_RECORDED,"PAYMENT",p.getId(),null);return new PaymentResult(paymentResponse(p),false);
    }
    private String fingerprint(PaymentRequest r,String reference) {
        try {
            var sorted=r.allocations().stream().sorted(Comparator.comparing(AllocationInput::installmentId))
                .map(x->List.of(x.installmentId().toString(),money(x.amount()).toPlainString())).toList();
            String canonical=mapper.writeValueAsString(List.of(r.feeAccountId(),money(r.amount()).toPlainString(),r.paymentDate().toString(),
                r.paymentMethod().name(),reference,clean(r.remarks()),sorted));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        }catch(java.security.NoSuchAlgorithmException|com.fasterxml.jackson.core.JsonProcessingException ex){throw new IllegalStateException("Cannot fingerprint payment",ex);}
    }
    static String clean(String s) {return s==null?"":s.strip();}
    private User currentUser() {return users.findByEmail(access.email()).orElseThrow(()->new AccessDeniedException("User account unavailable"));}
    @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('ADMIN')")
    public PaymentResponse voidPayment(Long id,VoidRequest r) {
        var p=find(FeePayment.class,id,"Payment",false);var a=editableAccount(p.getFeeAccount().getId(),null);
        em.refresh(p);
        if(p.getStatus()!=FeePaymentStatus.SUCCESS)throw new FinanceConflictException("Payment is already voided");
        if(clean(r.reason()).isEmpty())throw new IllegalArgumentException("A void reason is required");
        var audit=new PaymentVoidAudit();audit.setPayment(p);audit.setChangedBy(currentUser());audit.setReason(r.reason().strip());audit.setPreviousAmount(p.getAmount());
        audits.save(audit);adminAudit.record("PAYMENT_VOIDED","PAYMENT",id,"Payment voided with mandatory domain audit reason");p.setStatus(FeePaymentStatus.VOIDED);em.lock(a,LockModeType.PESSIMISTIC_FORCE_INCREMENT);em.flush();return paymentResponse(p);
    }
    public PaymentResponse payment(Long id) {var p=find(FeePayment.class,id,"Payment",false);authorize(p.getFeeAccount());return paymentResponse(p);}
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<PaymentResponse> paymentPage(int page,int size,String search,Long courseId,Long batchId,Long studentId,Long accountId,
            LocalDate fromDate,LocalDate toDate,FeePaymentStatus status,String sort) {
        CourseService.validatePage(page,size,search);dateRange(fromDate,toDate);
        return PageResponse.from(payments.findAll(paymentFilter(search,courseId,batchId,studentId,accountId,fromDate,toDate,status),
            paging(page,size,sort,Set.of("id","paymentDate","createdAt","amount"))).map(this::paymentResponse));
    }
    public static void dateRange(LocalDate from,LocalDate to) {if(from!=null && to!=null && from.isAfter(to))throw new IllegalArgumentException("From date must not follow to date");}
    private Specification<FeePayment> paymentFilter(String search,Long course,Long batch,Long student,Long account,LocalDate from,LocalDate to,FeePaymentStatus status) {
        String pattern=CourseService.pattern(search);
        return (r,q,cb)->{
            var a=r.get("feeAccount");var b=a.get("enrollment").get("batch");List<Predicate> p=new ArrayList<>();
            p.add(cb.or(cb.like(cb.lower(r.get("receiptNumber")),pattern,'\\'),cb.like(cb.lower(a.get("student").get("fullName")),pattern,'\\'),
                cb.like(cb.lower(a.get("student").get("admissionNumber")),pattern,'\\')));
            if(course!=null)p.add(cb.equal(b.get("course").get("id"),course));if(batch!=null)p.add(cb.equal(b.get("id"),batch));
            if(student!=null)p.add(cb.equal(a.get("student").get("id"),student));if(account!=null)p.add(cb.equal(a.get("id"),account));
            if(from!=null)p.add(cb.greaterThanOrEqualTo(r.get("paymentDate"),from));if(to!=null)p.add(cb.lessThanOrEqualTo(r.get("paymentDate"),to));
            if(status!=null)p.add(cb.equal(r.get("status"),status));return cb.and(p.toArray(Predicate[]::new));
        };
    }
    public PaymentResponse paymentResponse(FeePayment p) {
        var a=p.getFeeAccount();var s=a.getStudent();var b=a.getEnrollment().getBatch();
        var audit=audits.findByPaymentId(p.getId()).map(x->new VoidResponse(x.getReason(),x.getChangedBy().getFullName(),x.getCreatedAt(),x.getPreviousStatus(),x.getNewStatus(),x.getPreviousAmount())).orElse(null);
        return new PaymentResponse(p.getId(),a.getId(),p.getReceiptNumber(),s.getId(),s.getFullName(),s.getAdmissionNumber(),b.getCourse().getName(),
            b.getName(),p.getAmount(),p.getPaymentDate(),p.getPaymentMethod(),p.getTransactionReference(),p.getStatus(),p.getRecordedBy().getFullName(),p.getRemarks(),
            allocations.findByPaymentIdOrderByInstallmentInstallmentNumber(p.getId()).stream().map(x->new AllocationResponse(x.getInstallment().getId(),x.getInstallment().getInstallmentNumber(),x.getAmount())).toList(),audit,p.getCreatedAt());
    }
}
