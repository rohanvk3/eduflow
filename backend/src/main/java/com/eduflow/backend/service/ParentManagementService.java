package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ParentDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.exception.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true) @PreAuthorize("hasRole('ADMIN')")
public class ParentManagementService {
 @org.springframework.beans.factory.annotation.Autowired private AuditRecorder adminAudit;
 final ParentAccess access;final AssignmentAccess a;final ParentProfileRepository profiles;final ParentStudentLinkRepository links;final ParentLinkAuditRepository audits;final UserRepository users;final PasswordEncoder encoder;
 public ParentManagementService(ParentAccess access,AssignmentAccess a,ParentProfileRepository profiles,ParentStudentLinkRepository links,ParentLinkAuditRepository audits,UserRepository users,PasswordEncoder encoder){this.access=access;this.a=a;this.profiles=profiles;this.links=links;this.audits=audits;this.users=users;this.encoder=encoder;}
 public PageResponse<ProfileView> directory(int page,int size,String search,StudentStatus status){
  CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
  return PageResponse.from(profiles.findAll((r,q,cb)->{var p=cb.or(cb.like(cb.lower(r.get("user").get("fullName")),pattern,'\\'),cb.like(cb.lower(r.get("user").get("email")),pattern,'\\'),cb.like(r.get("phoneNumber"),pattern,'\\'));return status==null?p:cb.and(p,cb.equal(r.get("status"),status));},FinanceService.paging(page,size,"id,desc",Set.of("id"))).map(this::view));
 }
 public PageResponse<UserChoice> candidates(int page,int size,String search){
  CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
  return PageResponse.from(users.findAll((r,q,cb)->{var sub=q.subquery(Long.class);var p=sub.from(ParentProfile.class);sub.select(p.get("user").get("id"));return cb.and(cb.equal(r.get("role"),Role.PARENT),cb.not(r.get("id").in(sub)),cb.or(cb.like(cb.lower(r.get("fullName")),pattern,'\\'),cb.like(cb.lower(r.get("email")),pattern,'\\')));},FinanceService.paging(page,size,"id,desc",Set.of("id"))).map(u->new UserChoice(u.getId(),u.getFullName(),u.getEmail())));
 }
 public ProfileView get(Long id){return view(a.find(ParentProfile.class,id,"Parent profile",false));}
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public ProfileView create(CreateProfile input){
  User user;
  if(input.userId()!=null){
   user=a.find(User.class,input.userId(),"User",true);if(user.getRole()!=Role.PARENT)throw new IllegalArgumentException("Choose an existing PARENT account; roles are never changed automatically");
   if(input.password()!=null&&!input.password().isBlank())throw new IllegalArgumentException("Existing account passwords cannot be changed through profile creation");
  }else{
   if(input.fullName()==null||input.fullName().isBlank()||input.email()==null||input.email().isBlank()||input.password()==null||input.password().length()<8)throw new IllegalArgumentException("Name, valid login email and an initial password of at least 8 characters are required");
   if(input.password().getBytes(StandardCharsets.UTF_8).length>72)throw new IllegalArgumentException("Password must not exceed 72 UTF-8 bytes");
   String email=input.email().strip().toLowerCase(Locale.ROOT);if(users.existsByEmail(email))throw new DuplicateEmailException();
   user=users.saveAndFlush(new User(input.fullName().strip(),email,encoder.encode(input.password()),Role.PARENT));
  }
  adminAudit.record("PARENT_PROFILE_CREATED","USER",user.getId(),"Parent account/profile workflow completed");final Long uid=user.getId();if(profiles.count((r,q,cb)->cb.equal(r.get("user").get("id"),uid))>0)throw new AcademicConflictException("This user already has a parent profile");
  var p=new ParentProfile();p.setUser(user);p.setPhoneNumber(input.phoneNumber());p.setAlternatePhone(blank(input.alternatePhone()));p.setAddress(input.address());profiles.saveAndFlush(p);return view(p);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public ProfileView update(Long id,AdminProfile input){var p=a.find(ParentProfile.class,id,"Parent profile",true);edit(p,new EditProfile(input.version(),input.fullName(),input.phoneNumber(),input.alternatePhone(),input.email(),input.address()));p.setStatus(input.status());p.touch();profiles.flush();return view(p);}
 @PreAuthorize("hasRole('PARENT')")public ProfileView mine(){return view(access.own(false));}
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasRole('PARENT')")
 public ProfileView updateMine(EditProfile input){var own=access.own(true);var p=a.find(ParentProfile.class,own.getId(),"Parent profile",true);if(p.getStatus()!=StudentStatus.ACTIVE)throw new org.springframework.security.access.AccessDeniedException("Parent profile is inactive");edit(p,input);p.touch();profiles.flush();return view(p);}
 private void edit(ParentProfile p,EditProfile input){access.version(p.getVersion(),input.version());if(p.getUser().getRole()!=Role.PARENT)throw new IllegalArgumentException("Account must have PARENT role");p.getUser().setFullName(input.fullName().strip());p.setPhoneNumber(input.phoneNumber());p.setAlternatePhone(blank(input.alternatePhone()));String contact=blank(input.email());p.setContactEmail(contact!=null&&!contact.equalsIgnoreCase(p.getUser().getEmail())?contact:null);p.setAddress(input.address());}
 private String blank(String v){return v==null||v.isBlank()?null:v.strip();}
 ProfileView view(ParentProfile p){var u=p.getUser();return new ProfileView(p.getId(),u.getId(),u.getFullName(),u.getEmail(),p.getContactEmail()==null?u.getEmail():p.getContactEmail(),p.getPhoneNumber(),p.getAlternatePhone(),p.getAddress(),p.getStatus(),p.getVersion(),p.getCreatedAt());}
 public PageResponse<LinkView> links(Long parentId,Long studentId,ParentLinkStatus status,int page,int size){
  CourseService.validatePage(page,size,"");
  return PageResponse.from(links.findAll((r,q,cb)->{var p=cb.conjunction();if(parentId!=null)p=cb.and(p,cb.equal(r.get("parent").get("id"),parentId));if(studentId!=null)p=cb.and(p,cb.equal(r.get("student").get("id"),studentId));if(status!=null)p=cb.and(p,cb.equal(r.get("status"),status));return p;},FinanceService.paging(page,size,"id,desc",Set.of("id"))).map(this::linkView));
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public LinkView createLink(LinkRequest input){
  var p=a.find(ParentProfile.class,input.parentId(),"Parent profile",true);var s=a.find(Student.class,input.studentId(),"Student",true);active(p);
  var link=links.findOne((r,q,cb)->cb.and(cb.equal(r.get("parent").get("id"),p.getId()),cb.equal(r.get("student").get("id"),s.getId()))).orElse(null);
  if(link!=null&&link.getStatus()!=ParentLinkStatus.REVOKED)throw new AcademicConflictException("A pending or verified relationship already exists");
  ParentLinkStatus previous=link==null?null:link.getStatus();if(link==null)link=new ParentStudentLink();
  link.setParent(p);link.setStudent(s);link.setStatus(ParentLinkStatus.PENDING);link.setRelationshipType(input.relationshipType());link.setPrimaryGuardian(input.primaryGuardian());link.setVerifiedAt(null);link.setVerifiedBy(null);links.saveAndFlush(link);audit(link,previous,input.reason());return linkView(link);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED)
 public LinkView transition(Long id,LinkAction input,boolean verify){
  var read=a.find(ParentStudentLink.class,id,"Relationship",false);
  var p=a.find(ParentProfile.class,read.getParent().getId(),"Parent profile",true);a.find(Student.class,read.getStudent().getId(),"Student",true);
  a.em.refresh(read,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);access.version(read.getVersion(),input.version());var previous=read.getStatus();
  if(verify){
   active(p);if(previous!=ParentLinkStatus.PENDING)throw new AcademicConflictException("Only pending relationships can be verified");
   if(read.getPrimaryGuardian()&&links.count((r,q,cb)->cb.and(cb.equal(r.get("student").get("id"),read.getStudent().getId()),cb.equal(r.get("status"),ParentLinkStatus.VERIFIED),cb.isTrue(r.get("primaryGuardian"))))>0)throw new AcademicConflictException("The student already has a verified primary guardian");
   read.setStatus(ParentLinkStatus.VERIFIED);read.setVerifiedBy(a.user());read.setVerifiedAt(a.now());
  }else{if(previous==ParentLinkStatus.REVOKED)throw new AcademicConflictException("Relationship is already revoked");read.setStatus(ParentLinkStatus.REVOKED);}
  audit(read,previous,input.reason());links.flush();return linkView(read);
 }
 private void active(ParentProfile p){if(p.getStatus()!=StudentStatus.ACTIVE||p.getUser().getRole()!=Role.PARENT)throw new AcademicConflictException("An active PARENT profile is required");}
 private void audit(ParentStudentLink l,ParentLinkStatus previous,String reason){var event=new ParentLinkAudit();event.setLink(l);event.setPreviousStatus(previous);event.setNewStatus(l.getStatus());event.setRelationshipType(l.getRelationshipType());event.setPrimaryGuardian(l.getPrimaryGuardian());event.setChangedBy(a.user());event.setChangedAt(a.now());event.setReason(reason.strip());audits.saveAndFlush(event);adminAudit.record("PARENT_LINK_"+l.getStatus().name(),"PARENT_LINK",l.getId(),"Parent relationship state changed with domain audit history");}
 public PageResponse<AuditView> history(Long id,int page,int size){a.find(ParentStudentLink.class,id,"Relationship",false);return PageResponse.from(audits.findAll((r,q,cb)->cb.equal(r.get("link").get("id"),id),FinanceService.paging(page,size,"id,desc",Set.of("id"))).map(x->new AuditView(x.getId(),x.getPreviousStatus(),x.getNewStatus(),x.getRelationshipType(),x.getPrimaryGuardian(),x.getChangedBy().getFullName(),x.getChangedAt(),x.getReason())));}
 LinkView linkView(ParentStudentLink l){return new LinkView(l.getId(),l.getParent().getId(),l.getParent().getUser().getFullName(),l.getStudent().getId(),l.getStudent().getFullName(),l.getStudent().getAdmissionNumber(),l.getRelationshipType(),l.getStatus(),l.getPrimaryGuardian(),l.getVerifiedBy()==null?null:l.getVerifiedBy().getFullName(),l.getVerifiedAt(),l.getVersion());}
}
