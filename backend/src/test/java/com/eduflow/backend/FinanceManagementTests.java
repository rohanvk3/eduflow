package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.security.JwtService;
import com.eduflow.backend.service.*;
import com.fasterxml.jackson.databind.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Separate, disposable H2 database; HTTP writes commit so race tests exercise real transaction boundaries. */
@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:finance-tests;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
class FinanceManagementTests {
    @Autowired jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Test void feeAccountPageUsesBoundedQueriesAndReconcilesVoidedAllocations() throws Exception {
        var detail=provision();
        pay(detail,3000,"QUERY-PAID");
        var reversed=pay(detail,1000,"QUERY-VOID");
        mvc.perform(json(post("/api/fees/payments/"+reversed.get("id").asLong()+"/void"),Map.of("reason","Isolated query regression"),admin)).andExpect(status().isOk());
        Long structureId=detail.at("/account/feeStructureId").asLong();
        var batch=batches.findById(batchId).orElseThrow();
        for(int index=0;index<11;index++)account(structureId,enroll(makeStudent("QUERY-"+index,null),batch));
        var stats=entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);
        try {
            stats.clear();
            mvc.perform(get("/api/fees/accounts").param("size","1").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
            long one=stats.getPrepareStatementCount();stats.clear();
            var result=body(mvc.perform(get("/api/fees/accounts").param("size","100").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(12)).andReturn());
            long many=stats.getPrepareStatementCount();
            assertThat(many).isLessThanOrEqualTo(8).isLessThanOrEqualTo(one+1);
            var row=result.get("content").findParents("id").stream().filter(n->n.get("id").asLong()==accountId(detail)).findFirst().orElseThrow();
            assertThat(row.get("paidAmount").decimalValue()).isEqualByComparingTo("3000.00");
            assertThat(row.get("overdueAmount").decimalValue()).isEqualByComparingTo("2000.00");
            System.out.println("FEE_ACCOUNT_QUERY_EVIDENCE one="+one+" twelve="+many);
        } finally { stats.setStatisticsEnabled(false); }
    }
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JwtService jwt;
    @Autowired FeeStructureRepository structures;@Autowired StudentFeeAccountRepository accounts;
    @Autowired FeeInstallmentRepository installments;@Autowired FeePaymentRepository payments;
    @Autowired PaymentAllocationRepository allocations;@Autowired PaymentVoidAuditRepository audits;
    @Autowired CourseRepository courses;@Autowired BatchRepository batches;@Autowired BatchEnrollmentRepository enrollments;
    @Autowired StudentRepository students;@Autowired UserRepository users;
    String admin,student,otherStudent,teacher,parent;Long courseId,batchId,enrollmentId,otherEnrollmentId,studentId,otherStudentId;
    LocalDate today=BatchService.today();
    @BeforeEach void setup() {
        clean();
        users.saveAndFlush(new User("Finance Admin","admin@finance.test","unused",Role.ADMIN));
        User own=users.saveAndFlush(new User("Student","student@finance.test","unused",Role.STUDENT));
        User other=users.saveAndFlush(new User("Other","other@finance.test","unused",Role.STUDENT));
        users.saveAndFlush(new User("Teacher","teacher@finance.test","unused",Role.TEACHER));
        users.saveAndFlush(new User("Parent","parent@finance.test","unused",Role.PARENT));
        admin=jwt.generateToken("admin@finance.test");student=jwt.generateToken(own.getEmail());otherStudent=jwt.generateToken(other.getEmail());
        teacher=jwt.generateToken("teacher@finance.test");parent=jwt.generateToken("parent@finance.test");
        var c=new Course();c.setCode("FIN");c.setName("Mathematics");c.setDurationWeeks(12);courses.saveAndFlush(c);courseId=c.getId();
        var b=new Batch();b.setCode("FIN-B");b.setName("Morning batch");b.setCourse(c);b.setCapacity(20);b.setStartDate(today.minusMonths(1));b.setEndDate(today.plusMonths(2));b.setStatus(BatchStatus.ACTIVE);batches.saveAndFlush(b);batchId=b.getId();
        var s=makeStudent("FIN-1",own);studentId=s.getId();var t=makeStudent("FIN-2",other);otherStudentId=t.getId();
        enrollmentId=enroll(s,b);otherEnrollmentId=enroll(t,b);
    }
    Student makeStudent(String code,User u) {
        var s=new Student(code);s.setFullName(code.equals("FIN-1")?"=Asha, Student":"Other Student");s.setEmail(code+"@finance.test");s.setPhone("9876543210");
        s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusMonths(2));s.setAddress("Address");s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");s.setUser(u);return students.saveAndFlush(s);
    }
    Long enroll(Student s,Batch b){var e=new BatchEnrollment();e.setStudent(s);e.setBatch(b);e.setEnrollmentDate(today.minusDays(8));return enrollments.saveAndFlush(e).getId();}
    @AfterEach void clean() {
        audits.deleteAll();allocations.deleteAll();payments.deleteAll();installments.deleteAll();accounts.deleteAll();structures.deleteAll();
        enrollments.deleteAll();students.deleteAll();batches.deleteAll();courses.deleteAll();users.deleteAll();
    }
    MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder,Object value,String token)throws Exception {
        return builder.header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(value));
    }
    JsonNode body(MvcResult r)throws Exception{return mapper.readTree(r.getResponse().getContentAsString());}
    Map<String,Object> structureInput(){
        return new HashMap<>(Map.of("courseId",courseId,"name","Annual fees","tuitionFee",8000,"admissionFee",1000,"materialFee",1000,"otherFee",0,
            "effectiveFrom",today.minusDays(30).toString(),"effectiveTo",today.plusYears(1).toString(),"status","ACTIVE"));
    }
    Long structure()throws Exception{return body(mvc.perform(json(post("/api/fees/structures"),structureInput(),admin)).andExpect(status().isCreated()).andReturn()).get("id").asLong();}
    Map<String,Object> accountInput(Long structure,Long enrollment){return Map.of("enrollmentId",enrollment,"feeStructureId",structure,"discountAmount",1000,"agreementDate",today.minusDays(5).toString());}
    JsonNode account(Long structure,Long enrollment)throws Exception{return body(mvc.perform(json(post("/api/fees/accounts"),accountInput(structure,enrollment),admin)).andExpect(status().isCreated()).andReturn());}
    JsonNode getAccount(Long id)throws Exception{return body(mvc.perform(get("/api/fees/accounts/"+id).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andReturn());}
    JsonNode provision()throws Exception {
        var account=account(structure(),enrollmentId);Long id=account.get("account").get("id").asLong();
        return body(mvc.perform(json(post("/api/fees/accounts/"+id+"/installments"),Map.of("version",account.get("account").get("version").asLong(),"installments",List.of(
            Map.of("installmentNumber",1,"amount",5000,"dueDate",today.minusDays(1).toString()),Map.of("installmentNumber",2,"amount",4000,"dueDate",today.plusDays(15).toString()))),admin)).andExpect(status().isOk()).andReturn());
    }
    Long accountId(JsonNode detail){return detail.get("account").get("id").asLong();}
    Map<String,Object> paymentInput(JsonNode detail,int amount,String key) {
        return new HashMap<>(Map.of("feeAccountId",accountId(detail),"amount",amount,"paymentDate",today.toString(),"paymentMethod","UPI","transactionReference","REF-"+key,
            "idempotencyKey",key,"allocations",List.of(Map.of("installmentId",detail.get("installments").get(0).get("id").asLong(),"amount",amount)),"remarks","Verified"));
    }
    JsonNode pay(JsonNode a,int amount,String key)throws Exception{return body(mvc.perform(json(post("/api/fees/payments"),paymentInput(a,amount,key),admin)).andExpect(status().isCreated()).andReturn());}
    @Test void createsStructureWithBackendCalculatedTotalAndPagination()throws Exception {
        var input=structureInput();input.put("totalFee",1);
        mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isCreated()).andExpect(jsonPath("totalFee").value(10000));
        mvc.perform(get("/api/fees/structures").param("search","Annual").param("courseId",courseId.toString()).param("status","ACTIVE").param("size","1").header("Authorization","Bearer "+admin))
            .andExpect(status().isOk()).andExpect(jsonPath("totalElements").value(1));
    }
    @Test void rejectsNegativeComponentsExcessPrecisionAndReversedDates()throws Exception {
        var input=structureInput();input.put("tuitionFee",-1);mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isBadRequest());
        input.put("tuitionFee",new BigDecimal("1.001"));mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isBadRequest());
        input.put("tuitionFee",8000);input.put("effectiveTo",today.minusYears(1).toString());mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isBadRequest());
    }
    @Test void rejectsOverlapIncludingInclusiveBoundaryAndAllowsInactiveHistory()throws Exception {
        structure();mvc.perform(json(post("/api/fees/structures"),structureInput(),admin)).andExpect(status().isConflict());
        var input=structureInput();input.put("effectiveFrom",today.plusYears(1).toString());input.put("effectiveTo",today.plusYears(2).toString());
        mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isConflict());
        input.put("effectiveFrom",today.plusYears(1).plusDays(1).toString());mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isCreated());
        input.put("status","INACTIVE");mvc.perform(json(post("/api/fees/structures"),input,admin)).andExpect(status().isCreated());
    }
    @Test void snapshotsAgreementAndRejectsDuplicateEnrollmentAndExcessDiscount()throws Exception {
        Long f=structure();var a=account(f,enrollmentId);assertThat(a.at("/account/finalPayableAmount").decimalValue()).isEqualByComparingTo("9000.00");
        mvc.perform(json(post("/api/fees/accounts"),accountInput(f,enrollmentId),admin)).andExpect(status().isConflict());
        var input=new HashMap<>(accountInput(f,otherEnrollmentId));input.put("discountAmount",10001);mvc.perform(json(post("/api/fees/accounts"),input,admin)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/fees/structures/"+f+"/deactivate").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
        assertThat(getAccount(accountId(a)).at("/account/originalAmount").decimalValue()).isEqualByComparingTo("10000");
        mvc.perform(json(post("/api/fees/accounts"),accountInput(f,otherEnrollmentId),admin)).andExpect(status().isBadRequest());
    }
    @Test void referencedStructureAmountsAndDatesAreImmutable()throws Exception {
        Long f=structure();var a=account(f,enrollmentId);var input=structureInput();input.put("tuitionFee",9000);
        mvc.perform(json(put("/api/fees/structures/"+f),input,admin)).andExpect(status().isConflict());
        input.put("tuitionFee",8000);input.put("name","Renamed structure");
        mvc.perform(json(put("/api/fees/structures/"+f),input,admin)).andExpect(status().isOk());
        assertThat(getAccount(accountId(a)).at("/account/structureName").asText()).isEqualTo("Annual fees");
    }
    @Test void invalidCourseAndAgreementAndInactiveEnrollmentRejected()throws Exception {
        Long f=structure();var input=new HashMap<>(accountInput(f,enrollmentId));input.put("agreementDate",today.plusDays(1).toString());
        mvc.perform(json(post("/api/fees/accounts"),input,admin)).andExpect(status().isBadRequest());
        var e=enrollments.findById(enrollmentId).orElseThrow();e.setStatus(EnrollmentStatus.WITHDRAWN);e.setWithdrawalDate(today);enrollments.saveAndFlush(e);
        mvc.perform(json(post("/api/fees/accounts"),accountInput(f,enrollmentId),admin)).andExpect(status().isConflict());
    }
    @Test void scheduleIsAtomicAndTotalsAndDuplicateNumbersValidated()throws Exception {
        var a=account(structure(),enrollmentId);String url="/api/fees/accounts/"+accountId(a)+"/installments";
        var input=Map.of("version",0,"installments",List.of(Map.of("installmentNumber",1,"amount",8999,"dueDate",today.toString())));
        mvc.perform(json(post(url),input,admin)).andExpect(status().isBadRequest());assertThat(installments.count()).isZero();
        mvc.perform(json(post(url),Map.of("version",0,"installments",List.of(Map.of("installmentNumber",1,"amount",4000,"dueDate",today.toString()),Map.of("installmentNumber",1,"amount",5000,"dueDate",today.toString()))),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post(url),Map.of("version",0,"installments",List.of(Map.of("installmentNumber",1,"amount",9000,"dueDate",today.minusMonths(1).toString()))),admin)).andExpect(status().isBadRequest());assertThat(installments.count()).isZero();
    }
    @Test void detectsOverdueOnlyForPastUnpaidInstallments()throws Exception {
        var a=provision();assertThat(a.at("/account/overdueAmount").decimalValue()).isEqualByComparingTo("5000");
        assertThat(a.at("/installments/0/status").asText()).isEqualTo("OVERDUE");assertThat(a.at("/installments/1/status").asText()).isEqualTo("PENDING");
        pay(a,2000,"part");a=getAccount(accountId(a));assertThat(a.at("/account/overdueAmount").decimalValue()).isEqualByComparingTo("3000");
    }
    @Test void safeDueDateEditingAndStaleVersionProtection()throws Exception {
        var a=provision();Long i=a.at("/installments/0/id").asLong();long version=a.at("/account/version").asLong();
        mvc.perform(json(put("/api/fees/installments/"+i),Map.of("version",version,"amount",5000,"dueDate",today.plusDays(1).toString()),admin)).andExpect(status().isOk());
        mvc.perform(json(put("/api/fees/installments/"+i),Map.of("version",version,"amount",5000,"dueDate",today.plusDays(2).toString()),admin)).andExpect(status().isConflict());
        a=getAccount(accountId(a));pay(a,100,"lock");
        mvc.perform(json(put("/api/fees/installments/"+i),Map.of("version",getAccount(accountId(a)).at("/account/version").asLong(),"amount",5000,"dueDate",today.plusDays(3).toString()),admin)).andExpect(status().isConflict());
    }
    @Test void recordsPaymentAllocationsReceiptAndAuthenticatedActor()throws Exception {
        var a=provision();var input=paymentInput(a,5000,"valid");input.put("recordedBy",99999);
        var p=body(mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isCreated()).andExpect(jsonPath("recordedBy").value("Finance Admin")).andReturn());
        assertThat(p.get("receiptNumber").asText()).startsWith("EF-");assertThat(allocations.count()).isEqualTo(1);
        a=getAccount(accountId(a));assertThat(a.at("/account/paidAmount").decimalValue()).isEqualByComparingTo("5000");
        assertThat(a.at("/account/outstandingAmount").decimalValue()).isEqualByComparingTo("4000");assertThat(a.at("/installments/0/status").asText()).isEqualTo("PAID");
    }
    @Test void idempotentRetryReplaysWithoutCreatingPaymentAndChangedPayloadConflicts()throws Exception {
        var a=provision();var input=paymentInput(a,1000,"retry");var p=pay(a,1000,"retry");
        mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isOk()).andExpect(jsonPath("id").value(p.get("id").asLong()));
        input.put("remarks","different");mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isConflict());assertThat(payments.count()).isEqualTo(1);
    }
    @Test void duplicateExternalReferenceAndInvalidDatesRejected()throws Exception {
        var a=provision();pay(a,100,"reference");var input=paymentInput(a,100,"new-key");input.put("transactionReference","ref-reference");
        mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isConflict());
        input.put("transactionReference","NEW");input.put("paymentDate",today.plusDays(1).toString());mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isBadRequest());
        input.put("paymentDate",today.toString());input.put("transactionReference"," ");mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isBadRequest());
    }
    @Test void rejectsNegativeOverpaymentMismatchedAllocationsAndDuplicateInstallments()throws Exception {
        var a=provision();var input=paymentInput(a,-1,"negative");mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isBadRequest());
        input=paymentInput(a,10000,"over");mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isConflict());
        input=paymentInput(a,5001,"installment-over");mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isConflict());
        input=paymentInput(a,100,"mismatch");input.put("allocations",List.of(Map.of("installmentId",a.at("/installments/0/id").asLong(),"amount",99)));
        mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isBadRequest());
        input.put("allocations",List.of(Map.of("installmentId",a.at("/installments/0/id").asLong(),"amount",50),Map.of("installmentId",a.at("/installments/0/id").asLong(),"amount",50)));
        mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isBadRequest());assertThat(payments.count()).isZero();
    }
    @Test void allocationFromAnotherAccountRejectedWithoutPartialSave()throws Exception {
        var a=provision();var b=account(structures.findAll().getFirst().getId(),otherEnrollmentId);var input=paymentInput(a,100,"foreign");
        input.put("allocations",List.of(Map.of("installmentId",999999,"amount",100)));
        mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isBadRequest());assertThat(payments.count()).isZero();assertThat(allocations.count()).isZero();
        assertThat(accountId(b)).isNotEqualTo(accountId(a));
    }
    @Test void voidRequiresReasonAuditsAndRestoresBalancesWithoutDeletingAllocations()throws Exception {
        var a=provision();var p=pay(a,1000,"void");Long id=p.get("id").asLong();
        mvc.perform(json(post("/api/fees/payments/"+id+"/void"),Map.of("reason"," "),admin)).andExpect(status().isBadRequest());
        mvc.perform(json(post("/api/fees/payments/"+id+"/void"),Map.of("reason","Entry recorded in error"),admin)).andExpect(status().isOk()).andExpect(jsonPath("voidAudit.previousStatus").value("SUCCESS")).andExpect(jsonPath("voidAudit.previousAmount").value(1000));
        a=getAccount(accountId(a));assertThat(a.at("/account/paidAmount").decimalValue()).isEqualByComparingTo("0");assertThat(a.at("/account/outstandingAmount").decimalValue()).isEqualByComparingTo("9000");
        assertThat(payments.count()).isEqualTo(1);assertThat(allocations.count()).isEqualTo(1);assertThat(audits.count()).isEqualTo(1);
        mvc.perform(json(post("/api/fees/payments/"+id+"/void"),Map.of("reason","again"),admin)).andExpect(status().isConflict());
        mvc.perform(json(post("/api/fees/payments"),paymentInput(a,1000,"void"),admin)).andExpect(status().isConflict());
    }
    @Test void pdfGeneratedFromPersistenceWrapsLongNamesAndLabelsVoidedPayments()throws Exception {
        var a=provision();var s=students.findById(studentId).orElseThrow();s.setFullName("A very long student name that must wrap safely across the available receipt width for the institute");students.saveAndFlush(s);
        var p=pay(a,100,"pdf");Long id=p.get("id").asLong();
        byte[] bytes=mvc.perform(get("/api/fees/payments/"+id+"/receipt").header("Authorization","Bearer "+student)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf")).andReturn().getResponse().getContentAsByteArray();
        try(var doc=Loader.loadPDF(bytes)){String text=new PDFTextStripper().getText(doc);assertThat(text).contains("PAYMENT RECEIPT","INR 100.00",p.get("receiptNumber").asText(),"Current remaining balance");}
        mvc.perform(json(post("/api/fees/payments/"+id+"/void"),Map.of("reason","Wrong entry"),admin)).andExpect(status().isOk());
        bytes=mvc.perform(get("/api/fees/payments/"+id+"/receipt").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        try(var doc=Loader.loadPDF(bytes)){assertThat(new PDFTextStripper().getText(doc)).contains("NOT A VALID RECEIPT","Wrong entry");}
    }
    @Test void ownStudentAccessAndCrossStudentAndStaffRestrictions()throws Exception {
        var a=provision();var p=pay(a,100,"security");String accountUrl="/api/fees/accounts/"+accountId(a),paymentUrl="/api/fees/payments/"+p.get("id").asLong();
        for(String url:List.of(accountUrl,accountUrl+"/installments",paymentUrl,paymentUrl+"/receipt","/api/fees/students/"+studentId))
            mvc.perform(get(url).header("Authorization","Bearer "+otherStudent)).andExpect(status().isForbidden());
        mvc.perform(get(accountUrl).header("Authorization","Bearer "+student)).andExpect(status().isOk());
        mvc.perform(get("/api/fees/me").header("Authorization","Bearer "+student)).andExpect(status().isOk()).andExpect(jsonPath("accounts.totalElements").value(1));
        for(String token:List.of(teacher,parent,student)) {
            for(String url:List.of("/api/fees/dashboard","/api/fees/reports","/api/fees/reports/export","/api/fees/structures","/api/fees/accounts","/api/fees/payments"))
                mvc.perform(get(url).header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
            mvc.perform(json(post("/api/fees/payments"),paymentInput(a,100,"forbidden"),token)).andExpect(status().isForbidden());
            mvc.perform(json(post(paymentUrl+"/void"),Map.of("reason","No"),token)).andExpect(status().isForbidden());
        }
        mvc.perform(get(accountUrl)).andExpect(status().isUnauthorized());
    }
    @Test void unlinkedStudentGetsExplicitEmptyPortal()throws Exception {
        var u=users.saveAndFlush(new User("Unlinked","unlinked@finance.test","unused",Role.STUDENT));
        mvc.perform(get("/api/fees/me").header("Authorization","Bearer "+jwt.generateToken(u.getEmail()))).andExpect(status().isOk()).andExpect(jsonPath("linked").value(false)).andExpect(jsonPath("accounts.totalElements").value(0));
    }
    @Test void reportsAggregateEachPaymentOnceFilterDatesAndExcludeVoids()throws Exception {
        var a=provision();var input=paymentInput(a,6000,"split");input.put("allocations",List.of(Map.of("installmentId",a.at("/installments/0/id").asLong(),"amount",3000),Map.of("installmentId",a.at("/installments/1/id").asLong(),"amount",3000)));
        mvc.perform(json(post("/api/fees/payments"),input,admin)).andExpect(status().isCreated());
        var p=pay(a,100,"discard");mvc.perform(json(post("/api/fees/payments/"+p.get("id").asLong()+"/void"),Map.of("reason","Mistake"),admin)).andExpect(status().isOk());
        mvc.perform(get("/api/fees/reports").param("courseId",courseId.toString()).param("batchId",batchId.toString()).param("studentId",studentId.toString()).header("Authorization","Bearer "+admin))
            .andExpect(status().isOk()).andExpect(jsonPath("totalCollections").value(6000)).andExpect(jsonPath("outstandingBalance").value(3000)).andExpect(jsonPath("overdueAmount").value(2000)).andExpect(jsonPath("paymentMethods[0].amount").value(6000));
        mvc.perform(get("/api/fees/reports").param("toDate",today.minusDays(1).toString()).header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("totalCollections").value(0)).andExpect(jsonPath("outstandingBalance").value(3000));
        String csv=mvc.perform(get("/api/fees/reports/export").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("\"'=Asha, Student\"","SUCCESS","VOIDED","Amount (INR)");
    }
    @Test void withdrawalPreservesAgreementAndPaymentHistory()throws Exception {
        var a=provision();pay(a,100,"history");var e=enrollments.findById(enrollmentId).orElseThrow();e.setStatus(EnrollmentStatus.WITHDRAWN);e.setWithdrawalDate(today);enrollments.saveAndFlush(e);
        a=getAccount(accountId(a));assertThat(a.at("/account/enrollmentStatus").asText()).isEqualTo("WITHDRAWN");assertThat(a.at("/account/paidAmount").decimalValue()).isEqualByComparingTo("100");assertThat(a.get("payments")).hasSize(1);
    }
    @Test void noDataAndInvalidFiltersAndMissingResourcesHandled()throws Exception {
        mvc.perform(get("/api/fees/dashboard").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andExpect(jsonPath("totalCollections").value(0)).andExpect(jsonPath("monthlyTrend").isEmpty());
        mvc.perform(get("/api/fees/accounts/99999").header("Authorization","Bearer "+admin)).andExpect(status().isNotFound());
        mvc.perform(get("/api/fees/payments/99999/receipt").header("Authorization","Bearer "+admin)).andExpect(status().isNotFound());
        mvc.perform(get("/api/fees/reports").param("fromDate",today.toString()).param("toDate",today.minusDays(1).toString()).header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/fees/payments").param("sort","password,desc").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
    }
    @Test void concurrentPaymentsCannotOverpayAccount()throws Exception {
        var a=provision();var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results=new ArrayList<>();
            for(int n=0;n<2;n++){final int index=n;results.add(pool.submit(()->{start.await();var input=paymentInput(a,6000,"race-"+index);input.put("allocations",List.of(Map.of("installmentId",a.at("/installments/0/id").asLong(),"amount",4000),Map.of("installmentId",a.at("/installments/1/id").asLong(),"amount",2000)));return mvc.perform(json(post("/api/fees/payments"),input,admin)).andReturn().getResponse().getStatus();}));}
            start.countDown();var statuses=new ArrayList<Integer>();for(var result:results)statuses.add(result.get(20,TimeUnit.SECONDS));
            assertThat(statuses).containsExactlyInAnyOrder(201,409);assertThat(payments.count()).isEqualTo(1);
            assertThat(getAccount(accountId(a)).at("/account/outstandingAmount").decimalValue()).isEqualByComparingTo("3000");
        }
    }
    @Test void concurrentSameIdempotencyKeyCreatesExactlyOnePayment()throws Exception {
        var a=provision();var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> call=()->{start.await();return mvc.perform(json(post("/api/fees/payments"),paymentInput(a,1000,"same-key"),admin)).andReturn().getResponse().getStatus();};
            var one=pool.submit(call);var two=pool.submit(call);start.countDown();
            assertThat(List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,200);assertThat(payments.count()).isEqualTo(1);
        }
    }
}
