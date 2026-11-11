package com.eduflow.backend.service;

import com.eduflow.backend.dto.FinanceDtos.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Each payment is aggregated once, independently of its installment allocations. */
@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class FinanceReportService {
    private final NamedParameterJdbcTemplate jdbc;
    private final FinanceService finance;
    public FinanceReportService(NamedParameterJdbcTemplate jdbc,FinanceService finance) {this.jdbc=jdbc;this.finance=finance;}
    private static final String JOIN=" from fee_payments p join student_fee_accounts a on a.id=p.fee_account_id join batch_enrollments e on e.id=a.enrollment_id join batches b on b.id=e.batch_id join courses c on c.id=b.course_id join students s on s.id=a.student_id ";
    private static final String ACCOUNTS=" from student_fee_accounts a join batch_enrollments e on e.id=a.enrollment_id join batches b on b.id=e.batch_id join courses c on c.id=b.course_id ";
    private static final String PAID=" left join (select fee_account_id,sum(amount) paid from fee_payments where status='SUCCESS' group by fee_account_id) paid on paid.fee_account_id=a.id ";
    private String scope(Long course,Long batch,Long student,Map<String,Object> p) {
        String sql=" where 1=1 ";
        if(course!=null){sql+=" and c.id=:course ";p.put("course",course);}
        if(batch!=null){sql+=" and b.id=:batch ";p.put("batch",batch);}
        if(student!=null){sql+=" and a.student_id=:student ";p.put("student",student);}
        return sql;
    }
    private BigDecimal amount(String sql,Map<String,Object> p) {
        BigDecimal value=jdbc.queryForObject(sql,p,BigDecimal.class);return value==null?new BigDecimal("0.00"):value.setScale(2);
    }
    private List<Metric> groups(String sql,Map<String,Object> p) {
        return jdbc.query(sql,p,(rs,n)->new Metric(rs.getString(1),rs.getBigDecimal(2).setScale(2)));
    }
    private LocalDate[] dates(LocalDate from,LocalDate to) {
        LocalDate today=BatchService.today();if(to==null)to=today;if(from==null)from=to.minusMonths(11).withDayOfMonth(1);
        FinanceService.dateRange(from,to);
        if(java.time.temporal.ChronoUnit.DAYS.between(from,to)>3660)throw new IllegalArgumentException("Report range cannot exceed ten years");
        return new LocalDate[]{from,to};
    }
    public FinanceReport report(Long course,Long batch,Long student,LocalDate from,LocalDate to,int page,int size) {
        var dates=dates(from,to);from=dates[0];to=dates[1];Map<String,Object> p=new HashMap<>();
        String where=scope(course,batch,student,p);p.put("from",from);p.put("to",to);
        String selected=where+" and p.status='SUCCESS' and p.payment_date between :from and :to ";
        BigDecimal agreed=amount("select coalesce(sum(a.final_payable_amount),0)"+ACCOUNTS+where,p);
        BigDecimal lifetime=amount("select coalesce(sum(p.amount),0)"+JOIN+where+" and p.status='SUCCESS'",p);
        BigDecimal collected=amount("select coalesce(sum(p.amount),0)"+JOIN+selected,p);
        BigDecimal due=amount("select coalesce(sum(a.final_payable_amount-coalesce(paid.paid,0)),0)"+ACCOUNTS+PAID+where,p);
        p.put("today",BatchService.today());p.put("month",BatchService.today().withDayOfMonth(1));
        String installmentPaid=" left join (select pa.installment_id,sum(pa.amount) paid from payment_allocations pa join fee_payments fp on fp.id=pa.payment_id where fp.status='SUCCESS' group by pa.installment_id) ip on ip.installment_id=i.id ";
        BigDecimal overdue=amount("select coalesce(sum(i.amount-coalesce(ip.paid,0)),0)"+ACCOUNTS+" join fee_installments i on i.fee_account_id=a.id "+installmentPaid+where+" and i.due_date<:today",p);
        BigDecimal today=amount("select coalesce(sum(p.amount),0)"+JOIN+where+" and p.status='SUCCESS' and p.payment_date=:today",p);
        BigDecimal month=amount("select coalesce(sum(p.amount),0)"+JOIN+where+" and p.status='SUCCESS' and p.payment_date between :month and :today",p);
        var methods=groups("select p.payment_method,sum(p.amount)"+JOIN+selected+" group by p.payment_method order by p.payment_method",p);
        var trends=groups("select concat(year(p.payment_date),'-',right(concat('0',month(p.payment_date)),2)),sum(p.amount)"+JOIN+selected+" group by 1 order by 1",p);
        var courses=groups("select c.name,sum(p.amount)"+JOIN+selected+" group by c.id,c.name order by sum(p.amount) desc,c.id limit 100",p);
        var batches=groups("select b.name,sum(a.final_payable_amount-coalesce(paid.paid,0))"+ACCOUNTS+PAID+where+" group by b.id,b.name order by sum(a.final_payable_amount-coalesce(paid.paid,0)) desc,b.id limit 100",p);
        return new FinanceReport("INR","Asia/Kolkata",from,to,agreed,collected,lifetime,due,overdue,today,month,methods,trends,courses,batches,
            finance.paymentPage(page,size,"",course,batch,student,null,from,to,null,"paymentDate,desc"));
    }
    public byte[] export(Long course,Long batch,Long student,LocalDate from,LocalDate to) {
        var d=dates(from,to);Map<String,Object> p=new HashMap<>();String where=scope(course,batch,student,p);
        p.put("from",d[0]);p.put("to",d[1]);where+=" and p.payment_date between :from and :to ";
        Long count=jdbc.queryForObject("select count(*)"+JOIN+where,p,Long.class);
        if(count!=null && count>10000)throw new IllegalArgumentException("Export exceeds 10,000 payments; narrow the filters");
        var rows=jdbc.query("select p.receipt_number,p.payment_date,s.full_name,s.admission_number,c.name,b.name,p.fee_account_id,p.amount,p.payment_method,p.status"+JOIN+where+" order by p.payment_date,p.id limit 10000",p,
            (rs,n)->List.of(rs.getString(1),rs.getDate(2).toLocalDate().toString(),rs.getString(3),rs.getString(4),rs.getString(5),rs.getString(6),
                rs.getString(7),rs.getBigDecimal(8).setScale(2).toPlainString(),rs.getString(9),rs.getString(10)));
        StringBuilder csv=new StringBuilder("\uFEFFReceipt number,Payment date,Student,Admission number,Course,Batch,Fee account,Amount (INR),Method,Status\r\n");
        for(var row:rows)csv.append(row.stream().map(FinanceReportService::csvCell).collect(java.util.stream.Collectors.joining(","))).append("\r\n");
        return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    public static String csvCell(String value) {
        String s=value==null?"":value;
        String stripped=s.stripLeading();
        if(!stripped.isEmpty() && ("=+-@".indexOf(stripped.charAt(0))>=0 || s.charAt(0)=='\t' || s.charAt(0)=='\r' || s.charAt(0)=='\n'))s="'"+s;
        return "\""+s.replace("\"","\"\"")+"\"";
    }
}
