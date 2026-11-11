package com.eduflow.backend.service;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import com.eduflow.backend.exception.*;
import java.util.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
@Component
public class ParentAccess {
 final AcademicAccess auth;final ParentProfileRepository profiles;final ParentStudentLinkRepository links;
 public ParentAccess(AcademicAccess auth,ParentProfileRepository profiles,ParentStudentLinkRepository links){this.auth=auth;this.profiles=profiles;this.links=links;}
 public Optional<ParentProfile> profile(){
  if(!auth.hasRole("PARENT"))throw new AccessDeniedException("Parent role required");
  return profiles.findOne((r,q,cb)->cb.and(cb.equal(r.get("user").get("email"),auth.email()),cb.equal(r.get("user").get("role"),Role.PARENT)));
 }
 public ParentProfile own(boolean active){
  var p=profile().orElseThrow(()->new ResourceNotFoundException("Contact the institute to create your parent profile"));
  if(active&&p.getStatus()!=StudentStatus.ACTIVE)throw new AccessDeniedException("Parent profile is inactive");return p;
 }
 public ParentStudentLink relationship(Long studentId){
  var p=own(true);
  return links.findOne((r,q,cb)->cb.and(cb.equal(r.get("parent").get("id"),p.getId()),cb.equal(r.get("student").get("id"),studentId),cb.equal(r.get("status"),ParentLinkStatus.VERIFIED))).orElseThrow(()->new AccessDeniedException("No verified relationship for this child"));
 }
 public Student child(Long studentId){return relationship(studentId).getStudent();}
 public void version(long current,Long expected){if(expected==null||expected!=current)throw new AcademicConflictException("Record changed; reload before retrying");}
}
