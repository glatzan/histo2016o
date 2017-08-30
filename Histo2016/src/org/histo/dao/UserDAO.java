package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.histo.model.HistoUser;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class UserDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = -5033258085582728679L;

	public List<HistoUser> loadAllUsers() {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);
		criteria.select(root);

		criteria.distinct(true);
		criteria.orderBy(qb.asc(root.get("id")));

		List<HistoUser> users = getSession().createQuery(criteria).getResultList();

		return users;
	}
}
