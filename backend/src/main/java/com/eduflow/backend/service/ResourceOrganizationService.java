package com.eduflow.backend.service;
import com.eduflow.backend.dto.*;
import com.eduflow.backend.dto.ResourceDtos.*;
import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.*;
import java.text.Normalizer;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @Transactional(readOnly=true) @PreAuthorize("hasAnyRole('ADMIN','TEACHER','STUDENT')")
public class ResourceOrganizationService {
 final ResourceAccess a;final ResourceCategoryRepository categories;final ResourceFolderRepository folders;
 public ResourceOrganizationService(ResourceAccess a,ResourceCategoryRepository categories,ResourceFolderRepository folders){this.a=a;this.categories=categories;this.folders=folders;}
 public PageResponse<CategoryView> categories(int page,int size,String search,StudentStatus status){
  CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
  return PageResponse.from(categories.findAll((r,q,cb)->{var p=cb.like(cb.lower(r.get("name")),pattern,'\\');if(!a.admin()||status!=null)p=cb.and(p,cb.equal(r.get("status"),a.admin()?status:StudentStatus.ACTIVE));return p;},FinanceService.paging(page,size,"name,asc",Set.of("name","id"))).map(this::categoryView));
 }
 public CategoryView category(Long id){var c=a.find(ResourceCategory.class,id,"Category",false);if(!a.admin()&&c.getStatus()!=StudentStatus.ACTIVE)throw new org.springframework.security.access.AccessDeniedException("Category inactive");return categoryView(c);}
 @Transactional @PreAuthorize("hasRole('ADMIN')")
 public CategoryView saveCategory(Long id,CategoryRequest input){
  var c=id==null?new ResourceCategory():a.find(ResourceCategory.class,id,"Category",true);
  String name=Normalizer.normalize(input.name().strip().replaceAll("\\s+"," "),Normalizer.Form.NFKC),normalized=name.toLowerCase(Locale.ROOT);
  if(name.isBlank()||name.length()>100)throw new IllegalArgumentException("Invalid category name");
  if(categories.count((r,q,cb)->cb.and(cb.equal(r.get("normalizedName"),normalized),cb.notEqual(r.get("id"),id==null?0L:id)))>0)a.conflict("Category name already exists");
  c.setName(name);c.setNormalizedName(normalized);c.setDescription(input.description());c.setIcon(input.icon());return categoryView(categories.saveAndFlush(c));
 }
 @Transactional @PreAuthorize("hasRole('ADMIN')")
 public CategoryView deactivate(Long id){var c=a.find(ResourceCategory.class,id,"Category",true);c.setStatus(StudentStatus.INACTIVE);return categoryView(c);}
 CategoryView categoryView(ResourceCategory c){return new CategoryView(c.getId(),c.getName(),c.getDescription(),c.getIcon(),c.getStatus());}
 public PageResponse<FolderView> folders(int page,int size,String search,Long courseId,Long batchId,Long parentFolderId,boolean rootOnly,boolean courseWideOnly){
  CourseService.validatePage(page,size,search);String pattern=CourseService.pattern(search);
  return PageResponse.from(folders.findAll((r,q,cb)->{var p=cb.and(a.scope(r,q,cb),cb.like(cb.lower(r.get("name")),pattern,'\\'));if(courseId!=null)p=cb.and(p,cb.equal(r.get("course").get("id"),courseId));if(batchId!=null)p=cb.and(p,cb.equal(r.get("batch").get("id"),batchId));if(courseWideOnly)p=cb.and(p,cb.isNull(r.get("batch")));if(parentFolderId!=null)p=cb.and(p,cb.equal(r.get("parentFolder").get("id"),parentFolderId));else if(rootOnly)p=cb.and(p,cb.isNull(r.get("parentFolder")));return p;},FinanceService.paging(page,size,"name,asc",Set.of("name","id"))).map(this::folderView));
 }
 public FolderView folder(Long id){var f=a.find(ResourceFolder.class,id,"Folder",false);a.read(f.getCourse(),f.getBatch());return folderView(f);}
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public FolderView saveFolder(Long id,FolderRequest input){
  // Serialize hierarchy changes per course, including concurrent cycle checks.
  var course=a.find(Course.class,input.courseId(),"Course",true);var batch=a.batch(input.batchId(),course);a.manage(course,batch);
  var f=id==null?new ResourceFolder():a.find(ResourceFolder.class,id,"Folder",true);
  if(id!=null){a.manage(f.getCourse(),f.getBatch());if(!f.getCourse().getId().equals(course.getId())||!Objects.equals(batchId(f.getBatch()),batchId(batch)))a.conflict("Folder access scope cannot be changed; create another folder");}
  var parent=input.parentFolderId()==null?null:a.find(ResourceFolder.class,input.parentFolderId(),"Parent folder",false);
  if(parent!=null){
   if(!parent.getCourse().getId().equals(course.getId())||!Objects.equals(batchId(parent.getBatch()),batchId(batch)))throw new IllegalArgumentException("Parent must have the same course and batch scope");
   Set<Long> visited=new HashSet<>();var cursor=parent;int depth=0;
   while(cursor!=null){if(!visited.add(cursor.getId())||Objects.equals(cursor.getId(),id))throw new IllegalArgumentException("Circular folder hierarchy is not allowed");if(++depth>20)throw new IllegalArgumentException("Folders support at most 20 levels");cursor=cursor.getParentFolder();}
  }
  f.setName(input.name().strip());f.setDescription(input.description());f.setCourse(course);f.setBatch(batch);f.setParentFolder(parent);if(id==null)f.setCreatedBy(a.user());folders.saveAndFlush(f);return folderView(f);
 }
 @Transactional(isolation=Isolation.READ_COMMITTED) @PreAuthorize("hasAnyRole('ADMIN','TEACHER')")
 public void deleteFolder(Long id){
  var existing=a.find(ResourceFolder.class,id,"Folder",false);a.manage(existing.getCourse(),existing.getBatch());a.find(Course.class,existing.getCourse().getId(),"Course",true);var f=a.find(ResourceFolder.class,id,"Folder",true);
  if(folders.count((r,q,cb)->cb.equal(r.get("parentFolder").get("id"),id))>0||a.shared.em.createQuery("select count(m) from StudyMaterial m where m.folder.id=:id",Long.class).setParameter("id",id).getSingleResult()>0)a.conflict("Move resources and child folders before deleting this folder");folders.delete(f);
 }
 public FolderView folderView(ResourceFolder f){
  String query="select count(m) from StudyMaterial m where m.folder.id=:id"+(!a.staff()?" and m.visibility=com.eduflow.backend.entity.ResourceVisibility.PUBLISHED":"");
  long count=a.shared.em.createQuery(query,Long.class).setParameter("id",f.getId()).getSingleResult();
  return new FolderView(f.getId(),f.getName(),f.getDescription(),f.getCourse().getId(),f.getCourse().getName(),batchId(f.getBatch()),f.getBatch()==null?null:f.getBatch().getName(),f.getParentFolder()==null?null:f.getParentFolder().getId(),f.getParentFolder()==null?null:f.getParentFolder().getName(),count);
 }
 static Long batchId(Batch b){return b==null?null:b.getId();}
}
