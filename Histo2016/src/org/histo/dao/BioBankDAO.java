package org.histo.dao;

import java.io.Serializable;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.Hibernate;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;
import org.histo.model.BioBank;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class BioBankDAO extends AbstractDAO implements Serializable {

	@Autowired
	private GenericDAO genericDAO;

	private static final long serialVersionUID = 1663852599257860298L;

	/**
	 * Returns the matching BioBank object for the given task.
	 * 
	 * @param task
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public BioBank getAssociatedBioBankObject(Task task) {
		DetachedCriteria query = DetachedCriteria.forClass(BioBank.class, "bioBank");
		query.add(Restrictions.eq("task", task));
		query.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY);
		List<BioBank> result = query.getExecutableCriteria(getSession()).list();

		if (result != null && result.size() == 1)
			return result.get(0);

		return null;
	}

	/**
	 * Initializes the datalist and the taks of an biobank object
	 * 
	 * @param bioBank
	 */
	public BioBank initializeBioBank(BioBank bioBank) {
		bioBank = genericDAO.refresh(bioBank);
		Hibernate.initialize(bioBank.getAttachedPdfs());
		Hibernate.initialize(bioBank.getTask());
		return bioBank;
	}
}
