package com.eduflow.backend.service;

import com.eduflow.backend.dto.AdminDtos.Branding;
import com.eduflow.backend.dto.AdminDtos.Settings;
import com.eduflow.backend.entity.InstituteSettings;
import com.eduflow.backend.exception.AcademicConflictException;
import com.eduflow.backend.exception.ResourceNotFoundException;
import com.eduflow.backend.repository.AdministrationRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;

@Service
public class InstituteSettingsService {
    final EntityManager em;
    final AdministrationRepository settings;
    final AssignmentFileStorage files;
    final AuditRecorder audit;
    final TransactionTemplate tx;
    final String initialName;

    public InstituteSettingsService(EntityManager em, AdministrationRepository settings, AssignmentFileStorage files, AuditRecorder audit, PlatformTransactionManager manager,@org.springframework.beans.factory.annotation.Value("${app.institute.name:EduFlow}")String initialName) {
        this.em = em;
        this.settings = settings;
        this.files = files;
        this.audit = audit;
        this.tx = new TransactionTemplate(manager);
        if(initialName.isBlank()||initialName.length()>150)throw new IllegalArgumentException("Initial institute name must contain 1 to 150 characters");
        this.initialName=initialName.strip();
    }

    @PostConstruct
    void initialize() {
        try {
            tx.executeWithoutResult(s -> {
                if (!settings.existsById(1L)) {var created=new InstituteSettings();created.instituteName=initialName;settings.saveAndFlush(created);}
            });
        } catch (org.springframework.dao.DataIntegrityViolationException | PersistenceException ex) {
            if (!Boolean.TRUE.equals(tx.execute(s -> settings.existsById(1L)))) throw ex;
        }
    }

    public InstituteSettings lock() {
        return em.find(InstituteSettings.class, 1L, LockModeType.PESSIMISTIC_WRITE);
    }

    InstituteSettings current() {
        return settings.findById(1L).orElseThrow(() -> new IllegalStateException("Institute settings unavailable"));
    }

    @Transactional(readOnly = true)
    public String instituteName() {
        return current().instituteName;
    }

    @Transactional(readOnly = true)
    public Branding branding() {
        var s = current();
        return new Branding(s.instituteName, s.instituteCode, s.logoStorageKey != null);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Settings get() {
        return view(current());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Settings update(Settings input) {
        var s = lock();
        if (s.version != input.version()) throw new AcademicConflictException("Settings changed; reload before saving");
        try {
            ZoneId.of(input.timezone());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid timezone identifier");
        }
        if (!input.timezone().equals("Asia/Kolkata") || !input.currencyCode().equals("INR"))
            throw new IllegalArgumentException("Existing financial and academic records use Asia/Kolkata and INR; other values require a separate migration");
        if ((input.academicYearStart() == null) != (input.academicYearEnd() == null) || input.academicYearStart() != null && input.academicYearEnd().isBefore(input.academicYearStart()))
            throw new IllegalArgumentException("Academic year requires an ordered start/end pair");
        if (input.website() != null && !input.website().isBlank()) {
            try {
                var uri = URI.create(input.website());
                if (!java.util.Set.of("https", "http").contains(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null)
                    throw new IllegalArgumentException();
            } catch (Exception ex) {
                throw new IllegalArgumentException("Website must be a safe HTTP/HTTPS address");
            }
        }
        s.instituteName = input.instituteName().strip();
        s.instituteCode = input.instituteCode().strip();
        s.email = input.email();
        s.phoneNumber = input.phoneNumber();
        s.alternatePhone = input.alternatePhone();
        s.addressLine1 = input.addressLine1();
        s.addressLine2 = input.addressLine2();
        s.city = input.city();
        s.state = input.state();
        s.postalCode = input.postalCode();
        s.country = input.country();
        s.website = input.website();
        s.timezone = input.timezone();
        s.currencyCode = input.currencyCode();
        s.academicYearStart = input.academicYearStart();
        s.academicYearEnd = input.academicYearEnd();
        s.updatedAt = Instant.now();
        em.flush();
        audit.record("INSTITUTE_SETTINGS_UPDATED", "INSTITUTE", 1L, "Institute configuration updated");
        return view(s);
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Settings logo(MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 1024 * 1024)
            throw new IllegalArgumentException("Logo must be non-empty and at most 1 MiB");
        byte[] bytes = file.getBytes();
        boolean png = bytes.length > 8 && bytes[0] == (byte) 137 && bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71;
        boolean jpg = bytes.length > 3 && bytes[0] == (byte) 255 && bytes[1] == (byte) 216 && bytes[2] == (byte) 255;
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(java.util.Locale.ROOT);
        if (!(png && name.endsWith(".png") || jpg && (name.endsWith(".jpg") || name.endsWith(".jpeg"))))
            throw new IllegalArgumentException("Upload a valid PNG or JPEG logo");
        try (var stream = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(stream);
            if (!readers.hasNext()) throw new IllegalArgumentException("Invalid logo image");
            var reader = readers.next();
            try {
                reader.setInput(stream);
                if (reader.getWidth(0) > 2000 || reader.getHeight(0) > 2000)
                    throw new IllegalArgumentException("Logo dimensions must not exceed 2000×2000");
                if (reader.read(0) == null) throw new IllegalArgumentException("Invalid logo image");
            } finally {
                reader.dispose();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Invalid or corrupted logo image");
        }
        var s = lock();
        String key = files.put(bytes), old = s.logoStorageKey;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) files.delete(key);
            }

            public void afterCommit() {
                if (old != null) try {
                    files.delete(old);
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger(getClass()).warn("Old logo cleanup requires retry");
                }
            }
        });
        s.logoStorageKey = key;
        s.logoContentType = png ? "image/png" : "image/jpeg";
        s.updatedAt = Instant.now();
        audit.record("INSTITUTE_LOGO_UPDATED", "INSTITUTE", 1L, "Institute logo replaced");
        em.flush();
        return view(s);
    }

    @Transactional(readOnly = true)
    public org.springframework.http.ResponseEntity<byte[]> logo() {
        var s = current();
        if (s.logoStorageKey == null) throw new ResourceNotFoundException("No institute logo uploaded");
        return org.springframework.http.ResponseEntity.ok().header("X-Content-Type-Options", "nosniff").header("Cache-Control", "no-store").contentType(org.springframework.http.MediaType.parseMediaType(s.logoContentType)).body(files.read(s.logoStorageKey));
    }

    Settings view(InstituteSettings s) {
        return new Settings(s.instituteName, s.instituteCode, s.email, s.phoneNumber, s.alternatePhone, s.addressLine1, s.addressLine2, s.city, s.state, s.postalCode, s.country, s.website, s.timezone, s.currencyCode, s.academicYearStart, s.academicYearEnd, s.version, s.logoStorageKey != null);
    }
}
