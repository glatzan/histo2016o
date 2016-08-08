package org.histo.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Patient;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.util.SearchOptions;
import org.histo.util.TimeUtil;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
@Scope(value = "session")
public class PatientDao extends AbstractDAO implements Serializable {

	public void getAllPaitent() {
		Person p = new Person();
		System.out.println(getSession().save(p));
	}

	public void getPatitentByDate(Date from, Date to) {
		Criteria c = getSession().createCriteria(Person.class, "pat");
		c.createAlias("pat.tasks", "tasks"); // inner join by default
		c.add(Restrictions.gt("tasks.taskOccoured", from)).add(Restrictions.lt("tasks.taskOccoured", to)).list();
	}

	/**
	 * Returns a list of useres with the given piz. At least 6 numbers of the
	 * piz are needed.
	 * 
	 * @param piz
	 * @return
	 */
	public List<Patient> searchForPatientsPiz(String piz) {
		Criteria c = getSession().createCriteria(Patient.class);
		String regex = "";
		if (piz.length() != 8) {
			regex = "[0-9]{" + (8 - piz.length()) + "}";
		}
		c.add(Restrictions.like("piz", piz + regex));
		return c.list();
	}

	/**
	 * Returns a patient object for a specific piz. The piz has to be 8
	 * characters long.
	 * 
	 * @param piz
	 * @return
	 */
	public Patient searchForPatientPiz(String piz) {
		if (piz.length() != 8)
			return null;

		Criteria c = getSession().createCriteria(Patient.class);
		c.add(Restrictions.eq("piz", piz));
		List<Patient> result = c.list();
		if (result != null && result.size() == 1)
			return result.get(0);

		return null;
	}

	public List<Patient> getPatientWithoutTasks(long fromDate, long toDate) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.add(Restrictions.ge("patient.addDate", fromDate)).add(Restrictions.le("patient.addDate", toDate));
		c.add(Restrictions.isEmpty("patient.tasks"));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientByAddDateToWorklist(long fromDate, long toDate) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.add(Restrictions.ge("patient.addDate", fromDate)).add(Restrictions.le("patient.addDate", toDate));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientBySampleCreationDateBetweenDates(long fromDate, long toDate) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.createAlias("patient.tasks", "_tasks");
		c.createAlias("_tasks.samples", "_samples");
		c.add(Restrictions.ge("_samples.generationDate", fromDate))
				.add(Restrictions.le("_samples.generationDate", toDate));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientByStainingsBetweenDates(long fromDate, long toDate, boolean completed) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.createAlias("patient.tasks", "_tasks");
		c.add(Restrictions.ge("_tasks.creationDate", fromDate)).add(Restrictions.le("_tasks.creationDate", toDate));
		c.add(Restrictions.eq("_tasks.stainingCompleted", completed));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getPatientByDiagnosBetweenDates(long fromDate, long toDate, boolean completed) {
		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.createAlias("patient.tasks", "_tasks");
		c.add(Restrictions.ge("_tasks.stainingCompletionDate", fromDate))
				.add(Restrictions.le("_tasks.stainingCompletionDate", toDate));
		c.add(Restrictions.eq("_tasks.stainingCompleted", true));
		c.add(Restrictions.eq("_tasks.diagnosisCompleted", completed));
		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
		return c.list();
	}

	public List<Patient> getWorklistDynamicallyByType(long fromDate, long toDate, int searchType) {
		switch (searchType) {
		case SearchOptions.SEARCH_FILTER_ADDTOWORKLIST:
			return getPatientByAddDateToWorklist(fromDate, toDate);
		case SearchOptions.SEARCH_FILTER_TASKCREATION:
			return getPatientBySampleCreationDateBetweenDates(fromDate, toDate);
		case SearchOptions.SEARCH_FILTER_STAINING:
			return getPatientByStainingsBetweenDates(fromDate, toDate, true);
		case SearchOptions.SEARCH_FILTER_DIAGNOSIS:
			return getPatientByDiagnosBetweenDates(fromDate, toDate, true);
		default:
			return null;
		}
	}

	public List<Patient> getPatientsByParameter(String name, String surname, Date date) {

		Criteria c = getSession().createCriteria(Patient.class, "patient");
		c.createAlias("patient.person", "_person");

		if (name != null && !name.isEmpty())
			c.add(Restrictions.ilike("_person.name", name, MatchMode.ANYWHERE));
		if (surname != null & !surname.isEmpty())
			c.add(Restrictions.ilike("_person.surname", surname, MatchMode.ANYWHERE));
		if (date != null)
			c.add(Restrictions.eq("_person.birthday", date));

		c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);

		List<Patient> result = (List<Patient>) c.list();
		return result != null ? result : new ArrayList<>();
	}
}
