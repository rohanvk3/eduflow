package com.eduflow.backend.repository;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class AnalyticsRepository {
    final EntityManager em;

    public AnalyticsRepository(EntityManager em) {
        this.em = em;
    }

    public List<Object[]> rows(String hql, Map<String, Object> args, int offset, int limit) {
        var q = em.createQuery(hql, Object[].class);
        q.getParameters().forEach(p -> q.setParameter(p.getName(), args.get(p.getName())));
        return q.setFirstResult(offset).setMaxResults(limit).getResultList();
    }

    public Object scalar(String hql, Map<String, Object> args) {
        var q = em.createQuery(hql);
        q.getParameters().forEach(p -> q.setParameter(p.getName(), args.get(p.getName())));
        return q.getSingleResult();
    }

    public <T> T find(Class<T> type, Long id) {
        return em.find(type, id);
    }
}
