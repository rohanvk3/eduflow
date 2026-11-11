package com.eduflow.backend.demo;

import com.eduflow.backend.dto.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.UserRepository;
import com.eduflow.backend.security.AccountPrincipal;
import com.eduflow.backend.service.*;
import jakarta.persistence.EntityManager;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** One atomic dataset transaction. Business mutations use the actual authorized services. */
@Service @Profile("demo")
@ConditionalOnProperty(name="eduflow.demo.seed.enabled",havingValue="true")
public class DemoDataSeeder {
    final ApplicationContext app; final Environment env; final EntityManager em; final Validator validator; final DemoTimeline time;
    final DemoDataVerificationService verification;
    final List<User> teachers=new ArrayList<>(),students=new ArrayList<>(),parents=new ArrayList<>();
    final List<Long> courses=new ArrayList<>(),batches=new ArrayList<>(),teacherIds=new ArrayList<>(),studentIds=new ArrayList<>(),enrollmentIds=new ArrayList<>();
    final List<List<Integer>> roster=new ArrayList<>();
    User admin; LocalDate today,start;
    public DemoDataSeeder(ApplicationContext app,Environment env,EntityManager em,Validator validator,DemoTimeline time,DemoDataVerificationService verification){this.app=app;this.env=env;this.em=em;this.validator=validator;this.time=time;this.verification=verification;}
    <T>T service(Class<T> type){return app.getBean(type);}
    <T>T valid(T input){var violations=validator.validate(input);if(!violations.isEmpty())throw new IllegalArgumentException("Demo DTO validation failed: "+violations.iterator().next().getPropertyPath());return input;}
    static BigDecimal amount(long value){return BigDecimal.valueOf(value).setScale(2);}
    static String phone(int index){return String.format(Locale.ROOT,"00000%05d",index);}
    static Instant instant(LocalDate day,int hour){return day.atTime(hour,0).atZone(ZoneId.of("Asia/Kolkata")).toInstant();}
    void as(User user){var principal=new AccountPrincipal(user);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));}
    User user(String key,String name,Role role){
        var u=new User(name,key+"@eduprime.example",service(PasswordEncoder.class).encode(DemoSeedGuard.password(env,role.name())),role);
        service(UserRepository.class).saveAndFlush(u);return u;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED,rollbackFor=Exception.class)
    public Map<String,Object> seed() throws Exception {
        var previous=SecurityContextHolder.getContext().getAuthentication();
        try {
            if(em.createQuery("select count(a) from AuditLog a where a.action='DEMO_DATASET_COMPLETED' and a.entityType='DEMO'",Long.class).getSingleResult()>0){
                as(service(UserRepository.class).findByEmail("admin@eduprime.example").orElseThrow());return verification.verify(true);
            }
            if(em.createQuery("select count(u) from User u",Long.class).getSingleResult()!=0)throw new IllegalStateException("Refusing a nonempty unowned demo database");
            today=BatchService.today();start=today.minusDays(140);
            var before=verification.inventory();
            if(start.isBefore(LocalDate.of(2026,4,1))||today.isAfter(LocalDate.of(2027,3,31)))throw new IllegalStateException("This dataset models academic year 2026-2027; use within its supported demonstration dates");
            stage("Institute, accounts and academic roster");identity();academics();parentLinks();
            stage("Finalized attendance history");attendance();
            stage("Fee agreements, allocations and payments");finance();
            stage("Exams and published results");exams();
            stage("Assignments, submissions and feedback");assignments();
            stage("Original PDF resources and versions");resources();
            stage("Announcements, durable notifications and preferences");notices();
            for(int i=116;i<120;i++){
                service(BatchEnrollmentService.class).withdraw(batches.get(i%8),enrollmentIds.get(i),valid(new WithdrawalRequest(today)));
                service(StudentService.class).deactivateStudent(studentIds.get(i));
                service(AdministrationService.class).status(students.get(i).getId(),AccountStatus.INACTIVE);
            }
            time.live();as(admin);
            service(AuditRecorder.class).record("DEMO_DATASET_COMPLETED","DEMO",1L,DemoSeedGuard.NAMESPACE+";asOf="+today+";atomic-v1");
            var result=verification.verify(false);
            @SuppressWarnings("unchecked") var after=(Map<String,Long>)result.get("counts");
            var inserted=new LinkedHashMap<String,Long>();after.forEach((key,value)->inserted.put(key,value-before.getOrDefault(key,0L)));
            result.put("insertedByTable",inserted);result.put("insertedBusinessRecords",inserted.values().stream().mapToLong(Long::longValue).sum());
            return result;
        } finally {time.live();SecurityContextHolder.getContext().setAuthentication(previous);}
    }
    void stage(String label){System.out.println("EDUPRIME_DEMO: "+label);}
    void identity(){
        admin=user("admin","EduPrime Demo Administrator",Role.ADMIN);as(admin);
        var settings=service(InstituteSettingsService.class).get();
        service(InstituteSettingsService.class).update(valid(new AdminDtos.Settings("EduPrime Coaching Institute (Fictional)",DemoSeedGuard.NAMESPACE,"office@eduprime.example",phone(0),"","Fictional demonstration campus; not a real address","","Kanpur","Uttar Pradesh","000000","India","https://eduprime.example","Asia/Kolkata","INR",LocalDate.of(2026,4,1),LocalDate.of(2027,3,31),settings.version(),false)));
        String[] names={"Amit Sharma","Neha Gupta","Rajesh Tiwari","Pooja Mishra","Sandeep Verma","Shalini Singh","Vikram Tripathi","Meera Srivastava","Deepak Agarwal","Ritu Patel","Nitin Mehta","Kavita Joshi"};
        String[] subjects={"Physics","Chemistry","Mathematics","Biology","Mathematics","Chemistry","Physics","Biology","Mathematics","Physics","Chemistry","Biology"};
        for(int i=0;i<12;i++){
            var u=user(String.format("teacher%02d",i+1),names[i],Role.TEACHER);teachers.add(u);
            var t=service(TeacherService.class).createTeacher(valid(new TeacherRequest(String.format("EP-T-%02d",i+1),names[i],u.getEmail(),phone(200+i),i%2==0?"Male":"Female",LocalDate.of(1983+i%10,3,10),"M.Sc., B.Ed. (synthetic)",subjects[i],Set.of(subjects[i]),5+i%12,start.minusYears(2),"Fictional faculty address; demonstration only",amount(45000+i*2500),u.getId())));
            teacherIds.add(t.id());
        }
        String[] first={"Aarav","Ananya","Rohan","Priya","Aditya","Sneha","Ishita","Kunal","Kavya","Devansh","Nisha","Arjun","Aditi","Vihaan","Saanvi","Dhruv","Mehak","Yash","Tanvi","Kabir","Riya","Ishan","Avni","Samar"};
        String[] last={"Sharma","Gupta","Mishra","Verma","Singh","Yadav","Agarwal","Tiwari","Srivastava","Tripathi","Patel","Mehta","Joshi","Saxena","Bansal","Kapoor","Pandey","Chauhan","Sinha","Rastogi","Malhotra","Dubey","Kumar","Rai"};
        for(int i=0;i<120;i++){
            String name=first[i%24]+" "+last[(i+(i/24)*7)%24];var u=user(String.format("student%03d",i+1),name,Role.STUDENT);students.add(u);
            var input=valid(new StudentRequest(name,u.getEmail(),phone(1000+i),LocalDate.of(i%8==7?2011:2008+(i%3),2,1+i%25),i%2==0?"Male":"Female","Fictional Demo Locality "+(1+i%12)+"; not a real residential address","Fictional Guardian "+(i+1),phone(3000+i),start,u.getId()));
            // The public student service generates random admission numbers. Bootstrap only this validated identity
            // directly to obtain stable demo admission keys; all operational records below use business services.
            var s=new Student(String.format("EP-2627-%03d",i+1));s.setFullName(input.fullName());s.setEmail(input.email());s.setPhone(input.phone());s.setDateOfBirth(input.dateOfBirth());s.setGender(input.gender());s.setAddress(input.address());s.setGuardianName(input.guardianName());s.setGuardianPhone(input.guardianPhone());s.setAdmissionDate(input.admissionDate());s.setUser(u);em.persist(s);em.flush();studentIds.add(s.getId());
        }
    }
    void academics(){
        String[] names={"JEE Main + Advanced 2027","NEET UG 2027","Class 11 Science Foundation","Class 12 PCM Board Preparation","Class 12 PCB Board Preparation","Class 10 Foundation Programme"};
        for(int i=0;i<6;i++)courses.add(service(CourseService.class).create(valid(new CourseRequest("EP-C"+(i+1),names[i],"Fictional EduPrime programme: concepts, guided practice, doubt clearing and periodic assessments. English-medium instruction.",i<2?48:36))).id());
        String[] batchNames={"JEE Achievers Morning","JEE Rankers Evening","NEET Champions Morning","NEET Target Evening","XI Science Foundation A","XII PCM Board Excellence","XII PCB Board Excellence","X Foundation Scholars"};
        int[] courseIndex={0,0,1,1,2,3,4,5};
        for(int b=0;b<8;b++){
            var batch=service(BatchService.class).create(valid(new BatchRequest("EP-B"+(b+1),batchNames[b],courses.get(courseIndex[b]),40,start,today.plusDays(90),BatchStatus.ACTIVE)));batches.add(batch.id());roster.add(new ArrayList<>());
            for(int k=0;k<3;k++)service(BatchTimetableService.class).assign(batch.id(),valid(new TeacherAssignmentRequest(teacherIds.get((b+4*k)%12))));
            for(int slot=0;slot<(b<4?2:1);slot++){
                int n=b+slot*8;LocalTime begin=LocalTime.of(8+n/4,15*(n%4));
                service(BatchTimetableService.class).create(batch.id(),valid(new ScheduleRequest(teacherIds.get((b+4*slot)%12),DayOfWeek.of(1+n%5),begin,begin.plusMinutes(45),"Fictional Demo Room "+(b+1),true)));
            }
        }
        for(int i=0;i<120;i++){
            int b=i%8;var e=service(BatchEnrollmentService.class).enroll(batches.get(b),valid(new EnrollmentRequest(studentIds.get(i),start.plusDays(1))));enrollmentIds.add(e.id());roster.get(b).add(i);
            if(i<48&&b<4){int extra=b<2?5:6;service(BatchEnrollmentService.class).enroll(batches.get(extra),valid(new EnrollmentRequest(studentIds.get(i),start.plusDays(7))));roster.get(extra).add(i);}
        }
    }
    void parentLinks(){
        as(admin);var service=service(ParentManagementService.class);
        for(int i=0;i<90;i++){
            String name="Demo Guardian "+String.format("%03d",i+1);var u=user(String.format("parent%03d",i+1),name,Role.PARENT);parents.add(u);
            var p=service.create(valid(new ParentDtos.CreateProfile(u.getId(),null,null,null,phone(4000+i),"","Fictional guardian locality; demonstration only")));
            int first=i<60?i:60+(i-60)*2;
            for(int k=0;k<(i<60?1:2);k++){
                var l=service.createLink(valid(new ParentDtos.LinkRequest(p.id(),studentIds.get(first+k),i%2==0?RelationshipType.FATHER:RelationshipType.MOTHER,k==0,"Synthetic guardian identity checked for demonstration")));
                service.transition(l.id(),valid(new ParentDtos.LinkAction(l.version(),"Verified synthetic demo relationship")),true);
            }
            if(i<7){var l=service.createLink(valid(new ParentDtos.LinkRequest(p.id(),studentIds.get(119-i),RelationshipType.GUARDIAN,false,"Synthetic access-boundary illustration")));if(i>=4)service.transition(l.id(),valid(new ParentDtos.LinkAction(l.version(),"Revoked synthetic link; access must stop")),false);}
        }
    }
    void attendance(){
        var service=service(AttendanceSessionService.class);
        for(int n=0;n<40;n++)for(int b=0;b<8;b++){
            as(teachers.get((b+4*(n%3))%12));LocalDate day=today.minusDays(126).plusDays(n*3L);time.at(instant(day,16));
            var session=service.create(valid(new AttendanceDtos.CreateSession(batches.get(b),day,null,LocalTime.of(8,0),LocalTime.of(9,30),"Concept practice and doubt-clearing "+(n+1))));
            var records=new ArrayList<AttendanceDtos.RecordInput>();
            for(int i:roster.get(b)){
                int score=Math.floorMod(i*13+n*7,100);int absence=i%10==0?(n<20?35:15):10;
                AttendanceStatus status=score<absence?AttendanceStatus.ABSENT:score<absence+6?AttendanceStatus.LATE:score<absence+10?AttendanceStatus.EXCUSED:AttendanceStatus.PRESENT;
                records.add(valid(new AttendanceDtos.RecordInput(studentIds.get(i),status,status==AttendanceStatus.EXCUSED?"Synthetic approved absence":null)));
            }
            session=service.save(session.session().id(),valid(new AttendanceDtos.SaveRecords(session.session().version(),records)));
            service.finalizeSession(session.session().id(),valid(new AttendanceDtos.FinalizeRequest(session.session().version())));
        }
        as(admin);
    }
    void finance(){
        var service=service(FinanceService.class);long[] totals={96000,84000,48000,42000,44000,28000};var structures=new ArrayList<Long>();
        for(int c=0;c<6;c++)structures.add(service.saveStructure(null,valid(new FinanceDtos.StructureRequest(courses.get(c),"EP-2627 Tuition Agreement "+(c+1),amount(totals[c]-3000),amount(1000),amount(2000),amount(0),start,null,StudentStatus.ACTIVE))).id());
        int[] courseIndex={0,0,1,1,2,3,4,5};
        int voidCount=0;
        for(int i=0;i<120;i++){
            int c=courseIndex[i%8];var details=service.createAccount(valid(new FinanceDtos.AccountRequest(enrollmentIds.get(i),structures.get(c),amount(i%12==0?4000:0),start.plusDays(1))));
            BigDecimal payable=details.account().finalPayableAmount(),quarter=payable.divide(amount(4));var schedule=new ArrayList<FinanceDtos.InstallmentInput>();
            for(int k=0;k<4;k++)schedule.add(valid(new FinanceDtos.InstallmentInput(k+1,quarter,start.plusDays(7+k*48L))));
            details=service.createSchedule(details.account().id(),valid(new FinanceDtos.ScheduleRequest(details.account().version(),schedule)));
            int band=(i+i/8)%4;int count=band==0?4:band==1?3:band==2?1:0;
            for(int k=0;k<count;k++){
                var installment=details.installments().get(k);var method=PaymentMethod.values()[(i+k)%3];LocalDate paid=start.plusDays(5+k*43L);
                time.at(instant(paid,12));service.recordPayment(valid(new FinanceDtos.PaymentRequest(details.account().id(),installment.amount(),paid,method,method==PaymentMethod.CASH?null:String.format("EP-DEMO-TXN-%03d-%d",i+1,k+1),String.format("EP-DEMO-PAY-%03d-%d",i+1,k+1),List.of(new FinanceDtos.AllocationInput(installment.id(),installment.amount())),"Synthetic verified receipt; no external payment was made")));
            }
            if(voidCount<12&&count<4){var installment=details.installments().get(count);var p=service.recordPayment(valid(new FinanceDtos.PaymentRequest(details.account().id(),amount(500),start.plusDays(50),PaymentMethod.CASH,null,String.format("EP-DEMO-VOID-%03d",i+1),List.of(new FinanceDtos.AllocationInput(installment.id(),amount(500))),"Synthetic erroneous entry for audited void demonstration")));
                service.voidPayment(p.payment().id(),valid(new FinanceDtos.VoidRequest("Synthetic correction: duplicate cash entry was never collected")));voidCount++;}
        }
    }
    void exams(){
        var service=service(ExamService.class);var results=service(ExamResultService.class);
        String[] subjects={"Physics Unit Test","Chemistry Chapter Test","Mathematics Practice","Biology Revision","JEE Mock Assessment","NEET Mock Assessment","Board Preparation"};
        for(int n=0;n<30;n++){
            int b=n%8;as(teachers.get((b+4*((n/8)%3))%12));LocalDate day=n<24?today.minusDays(112).plusDays((n/8)*35L+n%8):today.plusDays(7+n-24);
            Instant begins=n==24||n==25?Instant.now().minusSeconds(3600):instant(day,10),ends=n==24||n==25?Instant.now().plusSeconds(172800):begins.plusSeconds(3600);time.at(begins.minusSeconds(86400));
            boolean online=n>=24;var e=service.save(null,valid(new ExamDtos.ExamRequest(String.format("EP-EX-%02d",n+1),subjects[n%subjects.length]+" "+(n+1),"Original synthetic assessment; varied results demonstrate learning trends",batches.get(b),online?ExamType.ONLINE_MCQ:ExamType.OFFLINE,online?45:null,amount(100),amount(40),begins,ends,null)));
            if(online)for(int q=0;q<4;q++){
                service.saveQuestion(e.id(),null,valid(new ExamDtos.QuestionRequest("Demo reasoning question "+(q+1)+": choose the correct arithmetic identity.",amount(25),q+1,List.of(new ExamDtos.OptionInput("2 + 2 = 4",true),new ExamDtos.OptionInput("2 + 2 = 5",false)),e.version())));e=service.get(e.id());
            }
            if(n>=28)continue;
            e=service.publish(e.id(),valid(new ExamDtos.VersionRequest(e.version())));
            if(n<24){time.at(ends.plusSeconds(60));var marks=new ArrayList<ExamDtos.OfflineInput>();for(int i:roster.get(b))marks.add(valid(new ExamDtos.OfflineInput(studentIds.get(i),amount(Math.min(98,32+(i*17+n*7)%53+(n/8)*5)),"Synthetic assessment; review worked examples and practise consistently")));
                results.saveOffline(e.id(),valid(new ExamDtos.OfflineRequest(e.version(),marks)));e=service.get(e.id());results.publish(e.id(),valid(new ExamDtos.VersionRequest(e.version())));}
            else if(n<26){time.live();for(int index:roster.get(b).subList(1,5)){as(students.get(index));var attempt=service(ExamAttemptService.class).start(e.id());var q=attempt.questions().getFirst();attempt=service(ExamAttemptService.class).save(attempt.id(),valid(new ExamDtos.SaveAnswers(attempt.version(),List.of(new ExamDtos.AnswerInput(q.id(),q.options().getFirst().id())))));service(ExamAttemptService.class).submit(attempt.id());}as(teachers.get((b+4*((n/8)%3))%12));}
        }
        as(admin);
    }
    void assignments(){
        var service=service(AssignmentService.class);var submissions=service(SubmissionService.class);
        String[] titles={"Newton's Laws Practice Set","Organic Chemistry Worksheet","Calculus Problem Set","Human Physiology Revision","Trigonometry Practice","Electrostatics Numerical Problems","Chemical Bonding Review","Genetics Concept Map"};
        for(int n=0;n<25;n++){
            int b=n%8;User faculty=teachers.get((b+4*((n/8)%3))%12);as(faculty);
            LocalDate day=n<20?today.minusDays(100).plusDays(n*4L):today.plusDays(n-20);Instant due=instant(day.plusDays(5),18);time.at(instant(day,8));
            var a=service.save(null,valid(new AssignmentDtos.AssignmentRequest(titles[n%8]+" "+(n+1),"Original demo exercise: explain each step and verify units. English-medium instructions.",batches.get(b),amount(100),due,n%3==0,null)));
            if(n>=23)continue;
            a=service.publish(a.id(),valid(new AssignmentDtos.VersionRequest(a.version())));
            if(n<20)for(int i:roster.get(b)){
                if((i+n)%5==0)continue;
                boolean late=n%3==0&&(i+n)%7==0;time.at(late?due.plusSeconds(3600):due.minusSeconds(86400+(i%4)*3600L));as(students.get(i));
                var work=submissions.create(a.id(),valid(new AssignmentDtos.SubmissionRequest("Synthetic solution by "+students.get(i).getFullName()+": identify the known quantities, apply the concept and check the result.",null,null)),List.of());
                if((i+n)%4!=0){as(faculty);time.at(due.plusSeconds(7200));submissions.grade(work.id(),valid(new AssignmentDtos.GradeRequest(work.version(),amount(45+(i*11+n*3)%53),"Good conceptual effort. Revise the final calculation and show units.",null)));}
            }
        }
        as(admin);
    }
    void resources() throws Exception {
        var organization=service(ResourceOrganizationService.class);var materials=service(StudyMaterialService.class);
        String[] labels={"Physics","Chemistry","Mathematics","Biology","Revision and Exam Strategy"};var categories=new ArrayList<Long>();
        for(String label:labels)categories.add(organization.saveCategory(null,valid(new ResourceDtos.CategoryRequest(label,"Original fictional EduPrime demo resources",null))).id());
        var folders=new ArrayList<Long>();for(int b=0;b<8;b++){var batch=service(BatchService.class).get(batches.get(b));folders.add(organization.saveFolder(null,valid(new ResourceDtos.FolderRequest("EP Revision Library "+(b+1),"Original demo notes",batch.courseId(),batches.get(b),null))).id());}
        String[] titles={"Physics Formula Sheet","Organic Chemistry Notes","Mathematics Revision Guide","Biology Chapter Summary","JEE Mock Test Instructions","NEET Preparation Checklist","Board Answer-Writing Guide","Foundation Problem-Solving Notes"};
        for(int n=0;n<50;n++){
            int b=n%8;as(teachers.get((b+4*((n/8)%3))%12));var batch=service(BatchService.class).get(batches.get(b));time.at(instant(today.minusDays(110-n*2L),12));
            byte[] pdf=DemoPdf.create(titles[n%8],n+1);
            var m=materials.save(null,valid(new ResourceDtos.MaterialRequest(titles[n%8]+" "+(n+1),"Original synthetic study sheet. Not copied from any coaching publisher.",batch.courseId(),batches.get(b),categories.get(n%5),folders.get(b),ResourceType.DOCUMENT,null,null)),new DemoUpload("eduprime-notes-"+(n+1)+".pdf",pdf));
            if(n<46)m=materials.publish(m.id(),m.version());
            if(n<4){m=materials.upload(m.id(),m.version(),new DemoUpload("eduprime-notes-"+(n+1)+"-revised.pdf",DemoPdf.create(titles[n%8]+" Revised",n+1)),"Additional original worked example");materials.publish(m.id(),m.version());}
        }
        as(admin);
    }
    void notices(){
        var service=service(AnnouncementService.class);String[] titles={"Diwali holiday notice (fictional calendar)","Mock test schedule","Parent-teacher meeting","Fee installment reminder","Exam results published","New study materials available","Assignment deadlines","Batch timetable update","Doubt-clearing clinic","Board revision workshop"};
        for(int n=0;n<20;n++){
            as(admin);time.at(instant(today.minusDays(80-n*4L),10));
            var a=service.save(null,valid(new NotificationDtos.AnnouncementRequest(titles[n%10]+" "+(n+1),"Fictional EduPrime notice for demonstration only. Please check your authorized timetable, study library and institute portal. Contact office@eduprime.example; no real event or payment is requested.",n%3==0?AnnouncementAudience.BATCH:AnnouncementAudience.ALL,null,null,n%3==0?batches.get(n%8):null,n==19?Instant.now().plusSeconds(86400):null,null)));
            if(n<18||n==19)service.publish(a.id(),valid(new NotificationDtos.VersionRequest(a.version())));
        }
        time.live();
        // Attendance alerts are opt-in. Enable them through the real preferences service for parents.
        for(User p:parents){as(p);service(NotificationService.class).updatePreferences(valid(new NotificationDtos.PreferenceRequest(true,true,true,true,true,true)));}
        as(admin);int batchesDone=0;while(service(NotificationDeliveryService.class).deliverBatch()>0){if(++batchesDone>20000)throw new IllegalStateException("Demo notification delivery did not converge");}
        for(User u:allUsers()){
            as(u);var page=service(NotificationService.class).list(null,"ALL",null,null,"",0,100);
            for(int n=0;n<Math.min(2,page.content().size());n++)service(NotificationService.class).read(page.content().get(n).id());
        }
        as(admin);service(AuditRecorder.class).record("DEMO_DATASET_VERIFIED","DEMO",1L,"Synthetic workflow population and recipient delivery complete");
    }
    List<User> allUsers(){var users=new ArrayList<User>();users.add(admin);users.addAll(teachers);users.addAll(students);users.addAll(parents);return users;}
}
