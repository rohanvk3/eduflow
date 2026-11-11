package com.eduflow.backend.demo;
import java.io.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
final class DemoPdf {
    static byte[] create(String title,int number)throws IOException{
        try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){
            var page=new PDPage();doc.addPage(page);
            try(var stream=new PDPageContentStream(doc,page)){
                stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);stream.newLineAtOffset(50,740);
                for(String line:new String[]{"EduPrime Coaching Institute - FICTIONAL DEMONSTRATION",title,"Original synthetic learning sheet "+number,"1. List the known quantities and the concept being applied.","2. Work through a simple example and show units at each step.","3. Check your answer by substitution and explain the reasoning.","Practice: calculate distance for a steady speed of 5 m/s over 8 s.","Worked answer: distance = speed x time = 40 m.","This original sample is not sourced from a coaching publisher.","Contact: office@eduprime.example | Kanpur, Uttar Pradesh"}){stream.showText(line);stream.newLineAtOffset(0,-25);}
                stream.endText();
            }doc.save(out);return out.toByteArray();
        }
    }
}
