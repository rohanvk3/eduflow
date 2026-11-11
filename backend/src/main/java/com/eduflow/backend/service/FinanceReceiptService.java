package com.eduflow.backend.service;

import com.eduflow.backend.entity.FeePaymentStatus;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','STUDENT')")
public class FinanceReceiptService {
    private final FinanceService finance;
    private final InstituteSettingsService institute;
    public FinanceReceiptService(FinanceService finance,InstituteSettingsService institute) {this.finance=finance;this.institute=institute;}
    public byte[] receipt(Long id) {
        var p=finance.payment(id);var account=finance.account(p.feeAccountId()).account();
        return render(p,account,false);
    }
    @PreAuthorize("hasRole('PARENT')")
    public byte[] parentReceipt(Long studentId,Long paymentId) {
        var p=finance.parentPayment(studentId,paymentId);var account=finance.parentAccount(studentId,p.feeAccountId()).account();
        return render(p,account,true);
    }
    private byte[] render(com.eduflow.backend.dto.FinanceDtos.PaymentResponse p,com.eduflow.backend.dto.FinanceDtos.AccountResponse account,boolean parentRead) {
        try(var doc=new PDDocument();var output=new ByteArrayOutputStream();
            var fontStream=new ClassPathResource("fonts/NotoSans-Regular.ttf").getInputStream()) {
            var font=PDType0Font.load(doc,fontStream);var writer=new ReceiptWriter(doc,font);
            writer.line(institute.instituteName(),22);writer.line(p.status()==FeePaymentStatus.VOIDED?"VOIDED PAYMENT — NOT A VALID RECEIPT":"PAYMENT RECEIPT",15);
            writer.line("Receipt: "+p.receiptNumber(),11);writer.line("Payment date: "+p.paymentDate(),11);
            writer.line("Student: "+p.studentName(),12);writer.line("Admission number: "+p.admissionNumber(),11);
            writer.line("Course: "+p.courseName(),11);writer.line("Batch: "+p.batchName(),11);
            writer.line("Fee account: #"+p.feeAccountId(),11);writer.line("Amount received: "+inr(p.amount()),16);
            writer.line("Method: "+p.paymentMethod().name().replace('_',' '),11);
            if(p.transactionReference()!=null && !p.transactionReference().isBlank())writer.line("Transaction reference: "+p.transactionReference(),11);
            writer.line("Recorded by: "+p.recordedBy(),11);
            for(var x:p.allocations())writer.line("Installment "+x.installmentNumber()+": "+inr(x.amount()),11);
            writer.line("Current remaining balance: "+inr(account.outstandingAmount()),12);
            if(p.voidAudit()!=null && !parentRead) {
                writer.line("VOIDED: "+p.voidAudit().reason(),12);writer.line("Voided by "+p.voidAudit().changedBy()+" at "+p.voidAudit().changedAt(),10);
            }
            writer.line("Generated: "+LocalDateTime.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ofPattern("dd MMM uuuu, HH:mm:ss"))+" (Asia/Kolkata)",10);
            writer.line("Balance reflects successful payments at generation time. This receipt is generated from institute records.",9);
            writer.close();doc.save(output);return output.toByteArray();
        }catch(IOException ex) {throw new IllegalStateException("Could not generate receipt",ex);}
    }
    private static String inr(BigDecimal amount) {return "INR "+String.format(Locale.forLanguageTag("en-IN"),"%,.2f",amount);}
    /** Wraps by rendered width, splitting long words and adding pages as needed. */
    private static class ReceiptWriter implements AutoCloseable {
        final PDDocument doc;final PDFont font;PDPageContentStream stream;float y;
        ReceiptWriter(PDDocument doc,PDFont font)throws IOException {this.doc=doc;this.font=font;page();}
        void page()throws IOException {
            if(stream!=null)stream.close();var page=new PDPage(PDRectangle.A4);doc.addPage(page);stream=new PDPageContentStream(doc,page);y=785;
        }
        void line(String text,float size)throws IOException {
            StringBuilder safe=new StringBuilder();
            for(int cp:text.codePoints().toArray()) {
                if(Character.isISOControl(cp)){safe.append(' ');continue;}
                String ch=new String(Character.toChars(cp));
                try{font.getStringWidth(ch);safe.append(ch);}catch(IllegalArgumentException ex){safe.append('?');}
            }
            StringBuilder row=new StringBuilder();
            for(String word:safe.toString().split(" ")) {
                if(font.getStringWidth(row+" "+word)/1000*size>490 && !row.isEmpty()){draw(row.toString(),size);row.setLength(0);}
                for(int cp:word.codePoints().toArray()) {
                    String ch=new String(Character.toChars(cp));
                    if(font.getStringWidth(row.toString()+ch)/1000*size>490){draw(row.toString(),size);row.setLength(0);}
                    row.append(ch);
                }
                row.append(' ');
            }
            if(!row.isEmpty())draw(row.toString().stripTrailing(),size);y-=8;
        }
        void draw(String text,float size)throws IOException {if(y<55)page();stream.beginText();stream.setFont(font,size);stream.newLineAtOffset(50,y);stream.showText(text);stream.endText();y-=size+5;}
        public void close()throws IOException {stream.close();}
    }
}
