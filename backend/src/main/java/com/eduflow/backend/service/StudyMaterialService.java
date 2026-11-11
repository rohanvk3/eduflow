package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ResourceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import java.net.URI;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class StudyMaterialService {
 final ResourceAccess a;final StudyMaterialRepository materials;final StudyMaterialVersionRepository versions;final AssignmentFileValidator validator;final AssignmentFiles files;
 final NotificationOutbox notifications;
 public StudyMaterialService(ResourceAccess a,StudyMaterialRepository materials,StudyMaterialVersionRepository versions,AssignmentFileValidator validator,AssignmentFiles files,NotificationOutbox notifications){this.a=a;this.materials=materials;this.versions=versions;this.validator=validator;this.files=files;this.notifications=notifications;}
 Specification<StudyMaterial> filter(String search,Long courseId,Long batchId,Long categoryId,Long folderId,ResourceType type,ResourceVisibility visibility,Long uploadedBy){
  String pattern=CourseService.pattern(search);
  return (r,q,cb)->{var p=cb.and(a.scope(r,q,cb),cb.or(cb.like(cb.lower(r.get("title")),pattern,'\\'),cb.like(cb.lower(r.get("description")),pattern,'\\')));
   if(!a.staff())p=cb.and(p,cb.equal(r.get("visibility"),ResourceVisibility.PUBLISHED));
   if(courseId!=null)p=cb.and(p,cb.equal(r.get("course").get("id"),courseId));if(batchId!=null)p=cb.and(p,cb.equal(r.get("batch").get("id"),batchId));
   if(categoryId!=null)p=cb.and(p,cb.equal(r.get("category").get("id"),categoryId));if(folderId!=null)p=cb.and(p,cb.equal(r.get("folder").get("id"),folderId));
   if(type!=null)p=cb.and(p,cb.equal(r.get("resourceType"),type));if(visibility!=null)p=cb.and(p,cb.equal(r.get("visibility"),visibility));if(uploadedBy!=null)p=cb.and(p,cb.equal(r.get("uploadedBy").get("id"),uploadedBy));return p;};
 }
 public PageResponse<MaterialView> list(int page,int size,String search,Long courseId,Long batchId,Long categoryId,Long folderId,ResourceType type,ResourceVisibility visibility,Long uploadedBy,String sort){
  CourseService.validatePage(page,size,search);
  return PageResponse.from(materials.findAll(filter(search,courseId,batchId,categoryId,folderId,type,visibility,uploadedBy),FinanceService.paging(page,size,sort,Set.of("id","title","createdAt","updatedAt","visibility","resourceType"))).map(this::view));
 }
 public MaterialView get(Long id){var m=a.find(StudyMaterial.class,id,"Material",false);a.read(m);return view(m);}
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public MaterialView save(Long id,MaterialRequest input,MultipartFile file){
  var course=a.find(Course.class,input.courseId(),"Course",true);var batch=a.batch(input.batchId(),course);a.manage(course,batch);
  var m=id==null?new StudyMaterial():a.find(StudyMaterial.class,id,"Material",true);
  if(id!=null){a.manage(m.getCourse(),m.getBatch());a.unchanged(m.getVersion(),input.version());if(m.getVisibility()!=ResourceVisibility.DRAFT)a.conflict("Published metadata is frozen; upload a version or create a new draft");}
  var category=a.find(ResourceCategory.class,input.categoryId(),"Category",false);
  if(category.getStatus()!=StudentStatus.ACTIVE&&(id==null||!m.getCategory().getId().equals(category.getId())))throw new IllegalArgumentException("Select an active category");
  var folder=input.folderId()==null?null:a.find(ResourceFolder.class,input.folderId(),"Folder",true);
  if(folder!=null&&(!folder.getCourse().getId().equals(course.getId())||!Objects.equals(ResourceOrganizationService.batchId(folder.getBatch()),ResourceOrganizationService.batchId(batch))))throw new IllegalArgumentException("Folder must have the same course and batch scope");
  String title=input.title().strip();
  if(materials.count((r,q,cb)->cb.and(cb.equal(cb.lower(r.get("title")),title.toLowerCase(Locale.ROOT)),cb.equal(r.get("course").get("id"),course.getId()),batch==null?cb.isNull(r.get("batch")):cb.equal(r.get("batch").get("id"),batch.getId()),cb.notEqual(r.get("id"),id==null?0L:id)))>0)a.conflict("A material with this title already exists in the selected scope");
  if(id!=null&&m.getCurrentVersion()>0&&m.getResourceType()!=input.resourceType())a.conflict("File-backed resource type cannot be changed after upload");
  m.setTitle(title);m.setDescription(input.description());m.setCourse(course);m.setBatch(batch);m.setCategory(category);m.setFolder(folder);m.setResourceType(input.resourceType());
  m.setExternalUrl(input.resourceType()==ResourceType.LINK?safeUrl(input.externalUrl()):null);
  if(input.resourceType()==ResourceType.LINK&&file!=null)throw new IllegalArgumentException("Link resources do not accept binary files");
  if(id==null)m.setUploadedBy(a.user());materials.saveAndFlush(m);
  if(file!=null)storeVersion(m,file,"Initial upload");a.shared.em.flush();return view(m);
 }
 public static String safeUrl(String input){
  try{URI u=new URI(input==null?"":input.strip());if(!Set.of("https","http").contains(u.getScheme()==null?"":u.getScheme().toLowerCase(Locale.ROOT))||u.getHost()==null||u.getUserInfo()!=null||u.toString().codePoints().anyMatch(Character::isISOControl)||u.toString().contains("\\")||u.toASCIIString().length()>2000)throw new IllegalArgumentException("Use an absolute HTTP or HTTPS URL without embedded credentials (at most 2000 characters)");return u.toASCIIString();}
  catch(java.net.URISyntaxException ex){throw new IllegalArgumentException("Invalid external URL");}
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public MaterialView upload(Long id,Long expected,MultipartFile file,String notes){
  var m=a.find(StudyMaterial.class,id,"Material",true);a.manage(m.getCourse(),m.getBatch());a.unchanged(m.getVersion(),expected);
  if(m.getVisibility()==ResourceVisibility.ARCHIVED)a.conflict("Archived materials cannot receive versions");
  if(m.getResourceType()==ResourceType.LINK)throw new IllegalArgumentException("Links do not have binary versions");
  if(notes!=null&&notes.length()>2000)throw new IllegalArgumentException("Change notes may contain at most 2000 characters");storeVersion(m,file,notes);a.shared.em.flush();return view(m);
 }
 private void storeVersion(StudyMaterial m,MultipartFile file,String notes){
  var validated=validator.validateResource(file);String mime=validated.contentType();
  // A future malware scanner belongs here, before the shared transactional file write.
  boolean match=switch(m.getResourceType()){case IMAGE->mime.startsWith("image/");case PRESENTATION->mime.contains("presentationml");case DOCUMENT->!mime.startsWith("image/")&&!mime.contains("presentationml");case LINK->false;};
  if(!match)throw new IllegalArgumentException("Resource type does not match the uploaded file");
  var v=new StudyMaterialVersion();v.setMaterial(m);v.setVersionNumber(m.getCurrentVersion()+1);v.setOriginalFileName(validated.name());v.setContentType(mime);v.setFileSize(validated.bytes().length);v.setStorageKey(files.save(validated.bytes()));v.setUploadedBy(a.user());v.setChangeNotes(notes);versions.saveAndFlush(v);m.setCurrentVersion(v.getVersionNumber());
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public MaterialView publish(Long id,Long expected){
  var m=a.find(StudyMaterial.class,id,"Material",true);a.manage(m.getCourse(),m.getBatch());a.unchanged(m.getVersion(),expected);
  if(m.getVisibility()==ResourceVisibility.ARCHIVED)a.conflict("Archived materials cannot be republished");
  if(m.getCategory().getStatus()!=StudentStatus.ACTIVE)a.conflict("Select an active category before publication");
  if(m.getResourceType()!=ResourceType.LINK&&m.getCurrentVersion()==0)a.conflict("Upload a valid file before publication");
  if(m.getVisibility()==ResourceVisibility.PUBLISHED&&Objects.equals(m.getPublishedVersion(),m.getCurrentVersion()))a.conflict("The current version is already published");
  m.setVisibility(ResourceVisibility.PUBLISHED);m.setPublishedVersion(m.getCurrentVersion());m.setPublishedAt(a.shared.now());a.shared.em.flush();notifications.capture("material-published:"+id+":"+m.getPublishedVersion(),NotificationType.STUDY_MATERIAL_PUBLISHED,"MATERIAL",id,String.valueOf(m.getPublishedVersion()));return view(m);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public MaterialView archive(Long id,Long expected){var m=a.find(StudyMaterial.class,id,"Material",true);a.manage(m.getCourse(),m.getBatch());a.unchanged(m.getVersion(),expected);if(m.getVisibility()!=ResourceVisibility.PUBLISHED)a.conflict("Only published materials can be archived");m.setVisibility(ResourceVisibility.ARCHIVED);a.shared.em.flush();return view(m);}
 public List<VersionView> versions(Long id){var m=a.find(StudyMaterial.class,id,"Material",false);a.manage(m.getCourse(),m.getBatch());return versions.findAll((r,q,cb)->cb.equal(r.get("material").get("id"),id),Sort.by(Sort.Direction.DESC,"versionNumber")).stream().map(this::versionView).toList();}
 public record Download(String name,String contentType,byte[] bytes){}
 public Download download(Long id,Long versionId,boolean preview){
  var m=a.find(StudyMaterial.class,id,"Material",false);a.read(m);if(m.getResourceType()==ResourceType.LINK)throw new IllegalArgumentException("External links do not have downloadable files");
  StudyMaterialVersion v;
  if(versionId!=null){a.manage(m.getCourse(),m.getBatch());v=a.find(StudyMaterialVersion.class,versionId,"Version",false);if(!v.getMaterial().getId().equals(id))throw new IllegalArgumentException("Version does not belong to this material");}
  else{int number=a.staff()?m.getCurrentVersion():Optional.ofNullable(m.getPublishedVersion()).orElse(0);v=version(m,number);}
  if(preview&&!v.getContentType().equals("application/pdf")&&!v.getContentType().startsWith("image/"))throw new IllegalArgumentException("Inline preview supports PDF and images; download this document instead");
  return new Download(v.getOriginalFileName(),v.getContentType(),files.read(v.getStorageKey()));
 }
 StudyMaterialVersion version(StudyMaterial m,int number){return versions.findOne((r,q,cb)->cb.and(cb.equal(r.get("material").get("id"),m.getId()),cb.equal(r.get("versionNumber"),number))).orElseThrow(()->new com.eduflow.backend.exception.ResourceNotFoundException("Resource file is unavailable"));}
 VersionView versionView(StudyMaterialVersion v){return new VersionView(v.getId(),v.getVersionNumber(),v.getOriginalFileName(),v.getContentType(),v.getFileSize(),v.getUploadedBy().getFullName(),v.getChangeNotes(),v.getCreatedAt());}
 MaterialView view(StudyMaterial m){
  int visibleVersion=a.staff()?m.getCurrentVersion():Optional.ofNullable(m.getPublishedVersion()).orElse(0);
  return new MaterialView(m.getId(),m.getTitle(),m.getDescription(),m.getCourse().getId(),m.getCourse().getName(),ResourceOrganizationService.batchId(m.getBatch()),m.getBatch()==null?null:m.getBatch().getName(),m.getCategory().getId(),m.getCategory().getName(),m.getFolder()==null?null:m.getFolder().getId(),m.getFolder()==null?null:m.getFolder().getName(),a.staff()?m.getUploadedBy().getId():null,m.getUploadedBy().getFullName(),m.getResourceType(),m.getVisibility(),m.getExternalUrl(),visibleVersion,m.getPublishedVersion(),m.getPublishedAt(),m.getCreatedAt(),m.getVersion(),visibleVersion==0?null:versionView(version(m,visibleVersion)));
 }
 public Statistics stats(){
  var scope=filter("",null,null,null,null,null,null,null);
  long total=materials.count(scope),published=materials.count(scope.and((r,q,cb)->cb.equal(r.get("visibility"),ResourceVisibility.PUBLISHED))),draft=materials.count(scope.and((r,q,cb)->cb.equal(r.get("visibility"),ResourceVisibility.DRAFT))),archived=materials.count(scope.and((r,q,cb)->cb.equal(r.get("visibility"),ResourceVisibility.ARCHIVED)));
  long mine=a.staff()?materials.count(scope.and((r,q,cb)->cb.equal(r.get("uploadedBy").get("id"),a.user().getId()))):0;
  return new Statistics(total,published,draft,archived,mine,counts(scope,"course"),counts(scope,"batch"),counts(scope,"category"),materials.findAll(scope,PageRequest.of(0,6,Sort.by(Sort.Direction.DESC,"createdAt","id"))).map(this::view).getContent());
 }
 List<CountView> counts(Specification<StudyMaterial> scope,String relation){
  var cb=a.shared.em.getCriteriaBuilder();var q=cb.createTupleQuery();var r=q.from(StudyMaterial.class);var related=r.join(relation);
  q.multiselect(related.get("id"),related.get("name"),cb.count(r)).where(scope.toPredicate(r,q,cb)).groupBy(related.get("id"),related.get("name")).orderBy(cb.desc(cb.count(r)));
  return a.shared.em.createQuery(q).setMaxResults(100).getResultList().stream().map(t->new CountView((Long)t.get(0),(String)t.get(1),(Long)t.get(2))).toList();
 }
 public Map<String,List<ScopeView>> options(){
  String query="select b from Batch b";Map<String,Object> args=new HashMap<>();
  if(!a.admin()){
   if(a.staff()){query+=" where exists(select t.id from BatchTeacherAssignment t where t.batch=b and t.active=true and t.teacher.user.email=:email)";}
   else{query+=" where exists(select n.id from BatchEnrollment n where n.batch=b and n.student.user.email=:email and n.status=:status and n.enrollmentDate<=:day and (n.withdrawalDate is null or n.withdrawalDate>:day) and (n.completionDate is null or n.completionDate>=:day))";args.put("status",EnrollmentStatus.ACTIVE);args.put("day",a.shared.now().atZone(AssignmentAccess.ZONE).toLocalDate());}
   args.put("email",a.shared.academic.email());
  }
  var jq=a.shared.em.createQuery(query+" order by b.name,b.id",Batch.class);args.forEach(jq::setParameter);
  var rows=jq.setMaxResults(1000).getResultList();Map<Long,ScopeView> courses=new LinkedHashMap<>();
  rows.forEach(b->courses.put(b.getCourse().getId(),new ScopeView(b.getCourse().getId(),b.getCourse().getName(),null)));
  if(a.admin())a.shared.em.createQuery("select c from Course c order by c.name",Course.class).setMaxResults(1000).getResultList().forEach(c->courses.put(c.getId(),new ScopeView(c.getId(),c.getName(),null)));
  return Map.of("courses",new ArrayList<>(courses.values()),"batches",rows.stream().map(b->new ScopeView(b.getId(),b.getName(),b.getCourse().getId())).toList());
 }
}
