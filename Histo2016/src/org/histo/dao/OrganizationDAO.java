package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.util.StreamUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class OrganizationDAO extends AbstractDAO implements Serializable {

	private static final long serialVersionUID = 6964886166116853913L;

	private static Logger logger = Logger.getLogger("histo");

	public Organization getOrganizationByName(String name) {
		DetachedCriteria query = DetachedCriteria.forClass(Organization.class, "organization");

		query.add(Restrictions.like("name", name));
		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return ((List<Organization>) query.getExecutableCriteria(getSession()).list()).stream()
				.collect(StreamUtils.singletonCollector());
	}

	public List<Organization> getOrganizations() {
		DetachedCriteria query = DetachedCriteria.forClass(Organization.class, "organization");

		query.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		return (List<Organization>) query.getExecutableCriteria(getSession()).list();
	}

	public Organization createOrganization(String name, Contact contact) {
		Organization newOrganization = new Organization();
		newOrganization.setName(name);
		newOrganization.setContact(contact);

		saveDataRollbackSave(newOrganization, "log.organization.created", new Object[] { name });

		return newOrganization;
	}

}
