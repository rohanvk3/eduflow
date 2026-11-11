package com.eduflow.backend.service;

import com.eduflow.backend.entity.NotificationCoordinator;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class NotificationCoordination {
    final EntityManager em;
    final TransactionTemplate tx;

    public NotificationCoordination(EntityManager em, PlatformTransactionManager manager) {
        this.em = em;
        this.tx = new TransactionTemplate(manager);
    }

    @PostConstruct
    void initialize() {
        for (String name : new String[]{"delivery", "reminders-exams", "reminders-assignments", "reminders-fees", "scheduled-announcements"}) {
            try {
                tx.executeWithoutResult(s -> {
                    if (em.find(NotificationCoordinator.class, name) == null) {
                        em.persist(new NotificationCoordinator(name));
                        em.flush();
                    }
                });
            } catch (org.springframework.dao.DataIntegrityViolationException |
                     jakarta.persistence.PersistenceException collision) {
                // Concurrent startup may win the unique primary-key insert. Verify rather than hide a real error.
                if (!Boolean.TRUE.equals(tx.execute(s -> em.find(NotificationCoordinator.class, name) != null)))
                    throw collision;
            }
        }
    }

    public NotificationCoordinator lock(String name) {
        var row = em.find(NotificationCoordinator.class, name, LockModeType.PESSIMISTIC_WRITE);
        if (row == null) throw new IllegalStateException("Notification coordination unavailable");
        return row;
    }
}
