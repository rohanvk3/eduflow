package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.service.BatchService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** UUID-tagged fixtures inside the inherited rollback transaction. No existing rows are changed. */
@EnabledIfEnvironmentVariable(named="EDUFLOW_MYSQL_TEST",matches="true")
class FinanceMySqlTests extends AcademicMySqlTests {
    @Test void mysqlAgreementSchedulePaymentReceiptReportAndVoid()throws Exception {
        String tag=UUID.randomUUID().toString().replace("-","");LocalDate today=BatchService.today();
        var u=users.saveAndFlush(new User("Finance QA",tag+"@finance.invalid","unused",Role.ADMIN));token=jwt.generateToken(u.getEmail());
        var s=new Student("QA-"+tag);s.setFullName("Finance QA");s.setEmail(tag+"@student.invalid");s.setPhone("9876543210");s.setDateOfBirth(today.minusYears(18));s.setAdmissionDate(today.minusDays(8));s.setAddress("QA");s.setGuardianName("Guardian");s.setGuardianPhone("9876543211");students.saveAndFlush(s);
        long course=create("/api/courses",Map.of("code","QA-"+tag,"name","Finance QA","durationWeeks",12));
        long batch=create("/api/batches",Map.of("code","QA-"+tag,"name","Finance QA batch","courseId",course,"capacity",5,"startDate",today.minusDays(10).toString(),"endDate",today.plusMonths(2).toString(),"status","ACTIVE"));
        long enrollment=create("/api/batches/"+batch+"/enrollments",Map.of("studentId",s.getId(),"enrollmentDate",today.minusDays(5).toString()));
        long structure=create("/api/fees/structures",Map.of("courseId",course,"name","QA fee","tuitionFee",1000,"admissionFee",0,"materialFee",0,"otherFee",0,"effectiveFrom",today.minusDays(10).toString(),"status","ACTIVE"));
        var account=mapper.readTree(mvc.perform(post("/api/fees/accounts").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("enrollmentId",enrollment,"feeStructureId",structure,"discountAmount",100,"agreementDate",today.toString())))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
        long id=account.at("/account/id").asLong();
        var schedule=mapper.readTree(mvc.perform(post("/api/fees/accounts/"+id+"/installments").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("version",0,"installments",List.of(Map.of("installmentNumber",1,"amount",900,"dueDate",today.toString())))))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long installment=schedule.at("/installments/0/id").asLong();
        long payment=create("/api/fees/payments",Map.of("feeAccountId",id,"amount",300,"paymentDate",today.toString(),"paymentMethod","UPI","transactionReference",tag,"idempotencyKey",tag,"allocations",List.of(Map.of("installmentId",installment,"amount",300))));
        mvc.perform(get("/api/fees/payments/"+payment+"/receipt").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/api/fees/accounts").param("studentId",s.getId().toString()).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("content[0].paidAmount").value(300)).andExpect(jsonPath("content[0].outstandingAmount").value(600));
        mvc.perform(get("/api/fees/reports").param("courseId",Long.toString(course)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("totalCollections").value(300)).andExpect(jsonPath("outstandingBalance").value(600));
        mvc.perform(post("/api/fees/payments/"+payment+"/void").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"Rollback-only verification\"}")).andExpect(status().isOk()).andExpect(jsonPath("voidAudit.previousAmount").value(300));
        mvc.perform(get("/api/fees/accounts/"+id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("account.paidAmount").value(0)).andExpect(jsonPath("account.outstandingAmount").value(900));
    }
}
