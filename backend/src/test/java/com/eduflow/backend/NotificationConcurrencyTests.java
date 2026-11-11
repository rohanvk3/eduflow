package com.eduflow.backend;

import com.eduflow.backend.entity.*;
import com.eduflow.backend.repository.NotificationJobRepository;
import com.eduflow.backend.repository.NotificationRepository;
import com.eduflow.backend.service.AnnouncementPublication;
import com.eduflow.backend.service.NotificationDeliveryService;
import com.eduflow.backend.service.NotificationReminderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:notification-concurrency;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "app.exams.expiry-enabled=false"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationConcurrencyTests {
    @Autowired
    EntityManager em;
    @Autowired
    PlatformTransactionManager manager;
    @Autowired
    NotificationReminderService reminders;
    @Autowired
    NotificationDeliveryService delivery;
    @Autowired
    AnnouncementPublication publication;
    @Autowired
    NotificationRepository notifications;
    @Autowired
    NotificationJobRepository jobs;

    long[] fixture(boolean scheduled) {
        return new TransactionTemplate(manager).execute(tx -> {
            var u = new User("Coordinator", UUID.randomUUID() + "@test.invalid", "unused", Role.ADMIN);
            em.persist(u);
            var a = new Announcement();
            a.setTitle("Notice");
            a.setContent("Notice");
            a.setAudienceType(AnnouncementAudience.ALL);
            a.setCreatedBy(u);
            a.setCreatedAt(Instant.now());
            a.setUpdatedAt(Instant.now());
            a.setStatus(scheduled ? AnnouncementStatus.SCHEDULED : AnnouncementStatus.DRAFT);
            a.setPublishAt(Instant.now().minusSeconds(1));
            em.persist(a);
            em.flush();
            return new long[]{u.getId(), a.getId()};
        });
    }

    void clean(long[] ids) {
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            em.createQuery("delete from Notification n where n.recipient.id=:id").setParameter("id", ids[0]).executeUpdate();
            em.createQuery("delete from NotificationTarget t where t.job.relatedType='ANNOUNCEMENT' and t.job.relatedId=:id").setParameter("id", ids[1]).executeUpdate();
            em.createQuery("delete from NotificationJob j where j.relatedType='ANNOUNCEMENT' and j.relatedId=:id").setParameter("id", ids[1]).executeUpdate();
            em.remove(em.find(Announcement.class, ids[1]));
            em.remove(em.find(User.class, ids[0]));
        });
    }

    List<Integer> concurrent(Callable<Integer> task) throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            Callable<Integer> ready = () -> {
                start.await();
                return task.call();
            };
            var a = pool.submit(ready);
            var b = pool.submit(ready);
            start.countDown();
            return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentSchedulersAndWorkersDeliverOneCopy() throws Exception {
        long[] ids = fixture(true);
        try {
            assertThat(concurrent(reminders::scheduledAnnouncements)).containsExactlyInAnyOrder(1, 0);
            assertThat(concurrent(delivery::deliverBatch)).containsExactlyInAnyOrder(1, 0);
            assertThat(jobs.count()).isEqualTo(1);
            assertThat(notifications.count()).isEqualTo(1);
            assertThat(reminders.scheduledAnnouncements()).isZero();
            assertThat(delivery.deliverBatch()).isZero();
        } finally {
            clean(ids);
        }
    }

    @Test
    void rollbackCannotLeavePublicationWithoutItsDeliveryJob() {
        long[] ids = fixture(false);
        try {
            assertThatThrownBy(() -> new TransactionTemplate(manager).executeWithoutResult(tx -> {
                publication.publish(em.find(Announcement.class, ids[1]), false);
                throw new IllegalStateException("Simulated transaction failure");
            })).isInstanceOf(IllegalStateException.class);
            new TransactionTemplate(manager).executeWithoutResult(tx -> {
                assertThat(em.find(Announcement.class, ids[1]).getStatus()).isEqualTo(AnnouncementStatus.DRAFT);
                assertThat(jobs.count()).isZero();
                assertThat(notifications.count()).isZero();
            });
        } finally {
            clean(ids);
        }
    }
}
