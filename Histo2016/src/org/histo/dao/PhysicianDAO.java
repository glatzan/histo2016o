package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.config.enums.ContactRole;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.util.CopySettingsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PhysicianDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 7297474866699408016L;

	@Autowired
	private OrganizationDAO organizationDAO;

	/**
	 * Gets a list of physicians which are associated with one role in the
	 * Contactrole Array. If archived is true, even archived physicians will be
	 * Returned.
	 * 
	 * @param roles
	 * @param archived
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(ContactRole role, boolean archived) {
		return getPhysicians(new ContactRole[] { role }, archived);
	}

	/**
	 * Gets a list of physicians which are associated with one role in the
	 * Contactrole Array. If archived is true, even archived physicians will be
	 * Returned.
	 * 
	 * @param roles
	 * @param archived
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(List<ContactRole> roles, boolean archived) {
		ContactRole[] contactRoles = new ContactRole[roles.size()];
		roles.toArray(contactRoles);
		return getPhysicians(contactRoles, archived);
	}

	/**
	 * Gets a list of physicians which are associated with one role in the
	 * Contactrole Array. If archived is true, even archived physicians will be
	 * Returned.
	 * 
	 * @param roles
	 * @param archived
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<Physician> getPhysicians(ContactRole[] roles, boolean archived) {

		if (roles == null || roles.length == 0)
			return new ArrayList<>();

		DetachedCriteria query = DetachedCriteria.forClass(Physician.class, "physician");
		query.addOrder(Order.asc("id"));
		query.createAlias("physician.associatedRoles", "a");
		// don't select archived physicians
		if (!archived)
			query.add(Restrictions.eq("archived", false));

		query.add(Restrictions.in("a.elements", roles));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Physician> result = query.getExecutableCriteria(getSession()).list();

		return result;
	}

	/**
	 * Load a physician with the given uid. Is used if a physician was added as a
	 * surgeon and no login was performed. Is located in UserDao because of the
	 * scope.
	 * 
	 * @param uid
	 * @return
	 */
	public Physician getPhysicianByUID(String uid) {
		// Create CriteriaBuilder
		CriteriaBuilder qb = getSession().getCriteriaBuilder();

		// Create CriteriaQuery
		CriteriaQuery<Physician> criteria = qb.createQuery(Physician.class);
		Root<Physician> root = criteria.from(Physician.class);
		criteria.select(root);

		criteria.where(qb.like(root.get("uid"), uid));
		criteria.distinct(true);

		List<Physician> physician = getSession().createQuery(criteria).getResultList();

		if (!physician.isEmpty()) {
			return physician.get(0);
		}

		return null;
	}

	/**
	 * Gets a physician for the provided person, returns null if none physician is
	 * available
	 * 
	 * @param person
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public Physician getPhysicianByPerson(Person person) {

		DetachedCriteria query = DetachedCriteria.forClass(Physician.class, "physician");

		query.add(Restrictions.eq("person", person));

		List<Physician> result = query.getExecutableCriteria(getSession()).list();

		if (result != null && result.size() == 1)
			return result.get(0);

		return null;
	}

	/**
	 * Checks if physician is saved in database, if so the saved physician will be
	 * updted, otherwiese a new physician will be created.
	 * 
	 * @param physician
	 * @return
	 */
	public Physician addOrMergePhysician(Physician physician) {
		// if the physician was added as surgeon the useracc an the
		// physician will be merged
		Physician physicianFromDatabase = getPhysicianByUID(physician.getUid());

		// undating the foud physician
		if (physicianFromDatabase != null) {
			logger.info("Physician already in database " + physician.getPerson().getFullName());
			mergePhysicians(physician, physicianFromDatabase);

			physicianFromDatabase.setArchived(false);

			// overwriting roles
			physicianFromDatabase.setAssociatedRoles(physician.getAssociatedRoles());

			save(physicianFromDatabase, resourceBundle.get("log.settings.physician.ldap.update",
					physicianFromDatabase.getPerson().getFullName()));

			physician = physicianFromDatabase;

		} else {
			logger.info("Creating new phyisician " + physician.getPerson().getFullName());

			// removing physicians temp id
			// TODO crate container object
			physician.setId(0);

			organizationDAO.synchronizeOrganizations(physician.getPerson().getOrganizsations());

			save(physician,
					resourceBundle.get("log.settings.physician.ldap.save", physician.getPerson().getFullName()));
		}

		return physician;
	}

	/**
	 * Merges two physicians an updates their organizations (
	 * 
	 * @param source
	 * @param destination
	 */
	public void mergePhysicians(Physician source, Physician destination) {
		CopySettingsUtil.copyPhysicianData(source, destination, false);
		organizationDAO.synchronizeOrganizations(destination.getPerson().getOrganizsations());
		save(destination, "user.role.settings.update", new Object[] { destination.toString() });
	}

	/**
	 * Archives or restores a physician
	 * 
	 * @param physician
	 * @param archive
	 */
	public void archivePhysician(Physician physician, boolean archive) {
		physician.setArchived(archive);
		save(physician,
				resourceBundle.get(archive ? "log.settings.physician.archived" : "log.settings.physician.archived.undo",
						physician.getPerson().getFullName()));
	}

	public void incrementPhysicianPriorityCounter(long id) {

		
		Physician physician = getSession().get(Physician.class, id);
		physician.setPriorityCount(physician.getPriorityCount()+1);

		getSession().update(physician);
		
	}

}