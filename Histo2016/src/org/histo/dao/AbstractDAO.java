package org.histo.dao;

import java.io.Serializable;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Scope;
import org.springframework.transaction.annotation.Transactional;

@Configurable
@Transactional
@Scope(value = "session")
public abstract class AbstractDAO implements Serializable {

    private static final long serialVersionUID = 8566919900494360311L;

    @Autowired
    protected SessionFactory sessionFactory;

    private Session session;

    public Session getSession() {
	try {
	    return sessionFactory.getCurrentSession();
	} catch (HibernateException hibernateException) {
	    hibernateException.printStackTrace();
	    if (session == null || !session.isOpen()) {
		session = sessionFactory.openSession();
	    }
	}
	return session;
    }

    public static void printStats(Statistics stats) {
	System.out.println("Fetch Count=" + stats.getEntityFetchCount());
	System.out.println("Second Level Hit Count=" + stats.getSecondLevelCacheHitCount());
	System.out.println("Second Level Miss Count=" + stats.getSecondLevelCacheMissCount());
	System.out.println("Second Level Put Count=" + stats.getSecondLevelCachePutCount());
    }
}
