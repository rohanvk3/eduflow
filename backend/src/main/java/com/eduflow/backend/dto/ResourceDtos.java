package com.eduflow.backend.dto;
import com.eduflow.backend.entity.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
public final class ResourceDtos {
 private ResourceDtos(){}
 public record CategoryRequest(@NotBlank @Size(max=100)String name,@Size(max=2000)String description,@Size(max=40)String icon){}
 public record CategoryView(Long id,String name,String description,String icon,StudentStatus status){}
 public record FolderRequest(@NotBlank @Size(max=150)String name,@Size(max=2000)String description,@NotNull Long courseId,Long batchId,Long parentFolderId){}
 public record FolderView(Long id,String name,String description,Long courseId,String courseName,Long batchId,String batchName,Long parentFolderId,String parentFolderName,long resourceCount){}
 public record MaterialRequest(@NotBlank @Size(max=180)String title,@Size(max=10000)String description,@NotNull Long courseId,Long batchId,@NotNull Long categoryId,Long folderId,@NotNull ResourceType resourceType,@Size(max=2000)String externalUrl,Long version){}
 public record VersionRequest(@NotNull Long version){}
 public record VersionView(Long id,int versionNumber,String originalFileName,String contentType,long fileSize,String uploadedBy,String changeNotes,LocalDateTime createdAt){}
 public record MaterialView(Long id,String title,String description,Long courseId,String courseName,Long batchId,String batchName,Long categoryId,String categoryName,Long folderId,String folderName,Long uploadedById,String uploadedBy,ResourceType resourceType,ResourceVisibility visibility,String externalUrl,int currentVersion,Integer publishedVersion,Instant publishedAt,LocalDateTime createdAt,long version,VersionView file){}
 public record CountView(Long id,String name,long count){}
 public record Statistics(long totalMaterials,long publishedMaterials,long draftMaterials,long archivedMaterials,long uploadedByMe,List<CountView> courses,List<CountView> batches,List<CountView> categories,List<MaterialView> recent){}
 public record ScopeView(Long id,String name,Long courseId){}
}

