package org.histo.dao;



import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Transactional
@Scope(value="session")
public abstract class AbstractDAO implements Serializable{

    @Autowired 
    private SessionFactory sessionFactory;

    private Session session;

    public Session getSession() {
        try {
            return sessionFactory.getCurrentSession();
        } catch (HibernateException hibernateException) {
            hibernateException.printStackTrace();
            System.out.println("Creation new session!!!!!!!!!!!!!!!" );
            if (session == null || !session.isOpen()) {
                session = sessionFactory.openSession();
            }
        }
        return session;
    }

}
