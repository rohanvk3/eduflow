package com.eduflow.backend.demo;

import com.eduflow.backend.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("demo")
public class DemoDataVerificationService {
    final JdbcTemplate jdbc;final AssignmentFileStorage files;final FinanceReportService finance;
    public DemoDataVerificationService(JdbcTemplate jdbc,AssignmentFileStorage files,FinanceReportService finance){this.jdbc=jdbc;this.files=files;this.finance=finance;}
    long number(String sql){return jdbc.queryForObject(sql,Long.class);}
    BigDecimal money(String sql){return jdbc.queryForObject(sql,BigDecimal.class).setScale(2);}
    void zero(String sql,String message){if(number(sql)!=0)throw new IllegalStateException("Demo integrity failed: "+message);}
    public Map<String,Long> inventory(){
        var counts=new LinkedHashMap<String,Long>();
        for(String table:jdbc.queryForList("select table_name from information_schema.tables where table_schema=database() and table_type='BASE TABLE' order by table_name",String.class)){
            if(!table.matches("[a-z0-9_]+"))throw new IllegalStateException("Unexpected table identifier");counts.put(table,number("select count(*) from `"+table+"`"));
        }return counts;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> verify(boolean reused){
        var expected=new LinkedHashMap<String,Long>();
        expected.put("users",223L);expected.put("teachers",12L);expected.put("students",120L);expected.put("parent_profiles",90L);expected.put("courses",6L);expected.put("batches",8L);expected.put("batch_enrollments",144L);expected.put("batch_schedules",12L);expected.put("attendance_sessions",320L);expected.put("student_fee_accounts",120L);expected.put("fee_payments",252L);expected.put("exams",30L);expected.put("assignments",25L);expected.put("study_materials",50L);expected.put("announcements",20L);
        var counts=new LinkedHashMap<String,Long>();
        for(var item:expected.entrySet()){long actual=number("select count(*) from "+item.getKey());if(actual!=item.getValue())throw new IllegalStateException("Demo count mismatch: "+item.getKey()+" expected "+item.getValue()+" actual "+actual);counts.put(item.getKey(),actual);}
        for(String table:List.of("attendance_records","fee_installments","payment_allocations","exam_results","exam_attempts","assignment_recipients","assignment_submissions","study_material_versions","parent_student_links","notifications","audit_logs"))counts.put(table,number("select count(*) from "+table));
        counts.putAll(inventory());
        zero("select count(*) from users where email not like '%@eduprime.example'","non-demo account");
        zero("select count(*) from students where admission_number not like 'EP-2627-%' or user_id is null or phone not like '00000%'","student identity namespace");
        zero("select count(*) from attendance_sessions where status<>'FINALIZED'","unfinalized history");
        zero("select count(*) from attendance_records r join attendance_sessions s on s.id=r.session_id where not exists (select 1 from batch_enrollments e where e.student_id=r.student_id and e.batch_id=s.batch_id and e.enrollment_date<=s.session_date and (e.withdrawal_date is null or e.withdrawal_date>s.session_date) and (e.completion_date is null or e.completion_date>=s.session_date))","attendance eligibility");
        zero("select count(*) from batches b where (select count(*) from batch_enrollments e where e.batch_id=b.id and e.status='ACTIVE')>b.capacity","batch capacity");
        zero("select count(*) from student_fee_accounts a where a.final_payable_amount<>(select coalesce(sum(i.amount),0) from fee_installments i where i.fee_account_id=a.id)","installment reconciliation");
        zero("select count(*) from fee_payments p where p.amount<>(select coalesce(sum(a.amount),0) from payment_allocations a where a.payment_id=p.id)","payment allocation reconciliation");
        zero("select count(*) from student_fee_accounts a where a.final_payable_amount<(select coalesce(sum(p.amount),0) from fee_payments p where p.fee_account_id=a.id and p.status='SUCCESS')","overpayment");
        zero("select count(*) from fee_installments i where i.amount<(select coalesce(sum(a.amount),0) from payment_allocations a join fee_payments p on p.id=a.payment_id where a.installment_id=i.id and p.status='SUCCESS')","installment over-allocation");
        zero("select count(*) from exam_results r join exams e on e.id=r.exam_id where r.obtained_marks<0 or r.obtained_marks>e.total_marks","exam marks");
        zero("select count(*) from assignment_submissions s join assignments a on a.id=s.assignment_id where s.obtained_marks>a.max_marks or not exists (select 1 from assignment_recipients r where r.assignment_id=s.assignment_id and r.student_id=s.student_id)","submission eligibility/marks");
        zero("select count(*) from parent_student_links where status='VERIFIED' and (verified_by_id is null or verified_at is null)","unverified parent access");
        zero("select count(*) from notifications n join users u on u.id=n.recipient_id where u.email not like '%@eduprime.example'","notification recipient ownership");
        zero("select count(*) from (select recipient_id,deduplication_key from notifications group by recipient_id,deduplication_key having count(*)>1) duplicates","duplicate notifications");
        zero("select count(*) from (select exam_id,student_id from exam_attempts group by exam_id,student_id having count(*)>1) duplicates","duplicate attempts");
        var keys=jdbc.queryForList("select storage_key from study_material_versions",String.class);
        for(String key:keys){byte[] data=files.read(key);if(data.length<4||data[0]!='%'||data[1]!='P')throw new IllegalStateException("Original demo PDF is missing");}
        if(keys.size()!=54)throw new IllegalStateException("Demo version history must have 54 PDFs");
        BigDecimal collected=money("select coalesce(sum(amount),0) from fee_payments where status='SUCCESS'");
        BigDecimal allocated=money("select coalesce(sum(a.amount),0) from payment_allocations a join fee_payments p on p.id=a.payment_id where p.status='SUCCESS'");
        if(collected.compareTo(allocated)!=0)throw new IllegalStateException("Demo collected revenue does not match successful allocations");
        var report=finance.report(null,null,null,BatchService.today().minusDays(180),BatchService.today(),0,10);
        if(report.lifetimeCollections().compareTo(collected)!=0)throw new IllegalStateException("Demo report/ledger collection mismatch");
        BigDecimal agreed=money("select coalesce(sum(final_payable_amount),0) from student_fee_accounts");
        if(report.outstandingBalance().compareTo(agreed.subtract(collected))!=0)throw new IllegalStateException("Demo outstanding balances do not reconcile");
        var result=new LinkedHashMap<String,Object>();result.put("namespace",DemoSeedGuard.NAMESPACE);result.put("institute","EduPrime Coaching Institute (Fictional), Kanpur");result.put("counts",counts);result.put("insertedBusinessRecords",reused?0:counts.values().stream().mapToLong(Long::longValue).sum());result.put("reusedBusinessRecords",reused?counts.values().stream().mapToLong(Long::longValue).sum():0);result.put("currency","INR");result.put("collected",collected);result.put("agreedFees",agreed);result.put("outstanding",agreed.subtract(collected));result.put("overdue",report.overdueAmount());result.put("activeEnrollments",number("select count(*) from batch_enrollments where status='ACTIVE'"));result.put("voidedPayments",number("select count(*) from fee_payments where status='VOIDED'"));result.put("publishedResults",number("select count(*) from exam_results r join exams e on e.id=r.exam_id where e.results_published=true"));result.put("filesVerified",keys.size());result.put("integrity","PASSED");result.put("reused",reused);return result;
    }
}
