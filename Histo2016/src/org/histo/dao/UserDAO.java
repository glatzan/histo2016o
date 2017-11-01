package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.hibernate.Hibernate;
import org.histo.config.enums.ContactRole;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class UserDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = -5033258085582728679L;

	@Autowired
	private OrganizationDAO organizationDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	public List<HistoUser> getUsers(boolean archived) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);
		criteria.select(root);

		if (!archived)
			criteria.where(qb.equal(root.get("archived"), false));

		criteria.distinct(true);
		criteria.orderBy(qb.asc(root.get("id")));

		List<HistoUser> users = getSession().createQuery(criteria).getResultList();

		return users;
	}

	public HistoUser loadUserByName(String name) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<HistoUser> criteria = qb.createQuery(HistoUser.class);
		Root<HistoUser> root = criteria.from(HistoUser.class);
		criteria.select(root);

		criteria.where(qb.like(root.get("username"), name));
		criteria.distinct(true);

		List<HistoUser> users = getSession().createQuery(criteria).getResultList();

		if (!users.isEmpty()) {
			return users.get(0);
		}

		return null;
	}

	public boolean addUser(Physician physician) {

		if (physician == null) {
			return false;
		}

		String userName = physician.getUid();

		// removing id from the list
		physician.setId(0);

		// checking if histouser exsists
		HistoUser histoUser = loadUserByName(userName);

		if (histoUser == null) {
			logger.info("No User found, creating new HistoUser " + physician.getPerson().getFullName());
			histoUser = new HistoUser(userName);

			// saving or updating physician, also updating organizations
			physician = physicianDAO.synchronizePhysician(physician);

			if (physician.getAssociatedRoles().size() == 0)
				physician.addAssociateRole(ContactRole.NONE);

			save(physician, "log.settings.physician.ldap.update", new Object[] { physician.toString() });

			histoUser.setPhysician(physician);

		} else {
			histoUser.getPhysician().copyIntoObject(physician);
			organizationDAO.synchronizeOrganizations(physician.getPerson().getOrganizsations());
		}

		save(histoUser, "log.userSettings.update", new Object[] { histoUser.toString() });

		return true;
	}

	public List<HistoGroup> getGroups(boolean archived) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<HistoGroup> criteria = qb.createQuery(HistoGroup.class);
		Root<HistoGroup> root = criteria.from(HistoGroup.class);
		criteria.select(root);

		if (!archived)
			criteria.where(qb.equal(root.get("archived"), false));

		criteria.distinct(true);
		criteria.orderBy(qb.asc(root.get("id")));

		List<HistoGroup> groups = getSession().createQuery(criteria).getResultList();

		return groups;
	}

	public HistoGroup initializeGroup(HistoGroup group, boolean initialize) {
		group = reattach(group);

		if (initialize) {
			Hibernate.initialize(group.getSettings());
		}

		return group;
	}

	public HistoGroup getHistoGroup(long id, boolean initialize) {

		HistoGroup group = get(HistoGroup.class, id);

		if (initialize) {
			Hibernate.initialize(group.getSettings());
		}

		return group;
	}

}
