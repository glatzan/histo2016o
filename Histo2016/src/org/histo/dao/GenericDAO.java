package org.histo.dao;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;


import org.hibernate.*;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


/**
 * The DAO class for generic entities
 *
 * @author Thomas Hemprich
 */

@Component
@Transactional
@Scope("session")
public class GenericDAO extends AbstractDAO {

    @SuppressWarnings("unchecked")
    public <C> C get(Class<C> clazz, Serializable serializable) {
        return (C) getSession().get(clazz, serializable);
    }

    @SuppressWarnings("unchecked")
    public <C> List<C> findAllByNamedWildcardParameter(Class<C> clazz,
                                                       String parameterName, Object parameterValue) {
        return getSession().createCriteria(clazz).add(
                Restrictions.like(parameterName, parameterValue))
                .setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY) // Added to avoid possible duplicates introduced by eager fetching
                .list();
    }

    public <C> List<C> findAllByNamedParameter(Class<C> clazz,
                                               String parameterName, Object parameterValue) {
        return findAllByNamedParameter(clazz, parameterName, parameterValue, null, true);
    }

    @SuppressWarnings("unchecked")
    public <C> List<C> findAllByNamedParameter(Class<C> clazz,
                                               String parameterName, Object parameterValue, String orderByColumnName,
                                               boolean ascending) {
        Criteria criteria = getSession().createCriteria(clazz)
                .add(Restrictions.eq(parameterName, parameterValue));

        if (orderByColumnName != null) {
            criteria.addOrder(
                    ascending ? Order.asc(orderByColumnName) : Order.desc(orderByColumnName));
        }

        return criteria.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY) // Added to avoid possible duplicates introduced by eager fetching
                .list();
    }

    public <C> Integer count(Class<C> clazz) {
        return ((Long) getSession().createCriteria(clazz)
                .setProjection(Projections.rowCount())
                .uniqueResult()).intValue();
    }

    public <C> Integer countByNamedParameter(Class<C> clazz,
                                             String parameterName, Object parameterValue) {
        return ((Long) getSession().createCriteria(clazz)
                .add(Restrictions.eq(parameterName, parameterValue))
                .setProjection(Projections.rowCount())
                .uniqueResult()).intValue();
    }

    @SuppressWarnings("unchecked")
    public <C> C save(C object) {
        Session session = getSession();
        try {
            session.saveOrUpdate(object);
        } catch (HibernateException hibernateException) {
            object = (C) session.merge(object);
            hibernateException.printStackTrace();
            System.out.println("!--------------------merging" );
        }
        return object;
    }

    public void save(Collection<?> objects) {
        Session session = getSession();
        for (Object object : objects) {
            try {
                session.saveOrUpdate(object);
            } catch (HibernateException hibernateException) {
                object = session.merge(object);
        	hibernateException.printStackTrace();
            }
        }
    }

    public void delete(Object object) {
        Session session = getSession();
        try {
            session.delete(object);
        } catch (HibernateException hibernateException) {
            session.delete(session.merge(object));
        }
    }

    public void delete(Collection<?> objects) {
        Session session = getSession();
        for (Object object : objects) {
            try {
                session.delete(object);
            } catch (HibernateException hibernateException) {
                session.delete(session.merge(object));
            }
        }
    }

    public <C> boolean isEntityWithNamedParameterExistent(Class<C> clazz,
                                                          String parameterName, String parameterValue) {
        return (Long) getSession().createCriteria(clazz)
                .add(Restrictions.eq(parameterName, parameterValue))
                .setProjection(Projections.rowCount())
                .uniqueResult() > 0;
    }

    public <C> boolean isEntityWithNamedParameterExistent(Class<C> clazz,
                                                          String parameterName, Long parameterValue) {
        return (Long) getSession().createCriteria(clazz)
                .add(Restrictions.eq(parameterName, parameterValue))
                .setProjection(Projections.rowCount())
                .uniqueResult() > 0;
    }

    public void refresh(Object object) {
        getSession().refresh(object);
    }

}