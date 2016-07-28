package org.histo.dao;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.hibernate.Criteria;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.histo.model.Patient;
import org.histo.model.Person;
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

    public List<Person> searchForPatientsPiz(String piz) {
	Criteria c = getSession().createCriteria(Person.class);
	String regex = "";
	if(piz.length() != 8){
	    regex =  "[0-9]{"+ (8-piz.length()) +"}";
	}
	c.add(Restrictions.like("piz", piz + regex));
	return c.list();
    }
    
    
    public List<Patient> getPatientWithoutTasks(long fromDate, long toDate){
	Criteria c = getSession().createCriteria(Patient.class, "patient");
	c.add(Restrictions.ge("patient.addDate", fromDate)).add(Restrictions.le("patient.addDate", toDate));
	c.add(Restrictions.isEmpty("patient.tasks"));
	c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	return c.list();
    }
    
    public List<Patient> getPatientByAddDateToWorklist(long fromDate, long toDate){
	Criteria c = getSession().createCriteria(Patient.class, "patient");
	c.add(Restrictions.ge("patient.addDate", fromDate)).add(Restrictions.le("patient.addDate", toDate));
	c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	return c.list();
    }
    
    public List<Patient> getPatientBySampleCreationDateBetweenDates(long fromDate, long toDate){
	Criteria c = getSession().createCriteria(Patient.class, "patient");
	c.createAlias("patient.tasks", "_tasks");
	c.createAlias("_tasks.samples", "_samples");
	c.add(Restrictions.ge("_samples.generationDate", fromDate)).add(Restrictions.le("_samples.generationDate", toDate));
	c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	return c.list();
    }
    
    public List<Patient> getPatientByStainingsBetweenDates(long fromDate, long toDate, boolean completed){
	Criteria c = getSession().createCriteria(Patient.class, "patient");
	c.createAlias("patient.tasks", "_tasks");
	c.add(Restrictions.ge("_tasks.creationDate", fromDate)).add(Restrictions.le("_tasks.creationDate", toDate));
	c.add(Restrictions.eq("_tasks.stainingCompleted", completed));
	c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	return c.list();
    }
    
    public List<Patient> getPatientByDiagnosBetweenDates(long fromDate, long toDate, boolean completed){
	Criteria c = getSession().createCriteria(Patient.class, "patient");
	c.createAlias("patient.tasks", "_tasks");
	c.add(Restrictions.ge("_tasks.stainingCompletionDate", fromDate)).add(Restrictions.le("_tasks.stainingCompletionDate", toDate));
	c.add(Restrictions.eq("_tasks.stainingCompleted", true));
	c.add(Restrictions.eq("_tasks.diagnosisCompleted", completed));
	c.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
	return c.list();
    }
    
    public List<Patient> getWorklistDynamicallyByType(long fromDate, long toDate, int searchType){
	switch (searchType) {
	case SearchOptions.SEARCH_FILTER_ADDTOWORKLIST:
	    return getPatientByAddDateToWorklist(fromDate, toDate);
	case SearchOptions.SEARCH_FILTER_TASKCREATION:
	    return getPatientBySampleCreationDateBetweenDates(fromDate, toDate);
	case SearchOptions.SEARCH_FILTER_STAINING:
	    return getPatientByStainingsBetweenDates(fromDate, toDate, true);
	case SearchOptions.SEARCH_FILTER_DIAGNOSIS:
	    return getPatientByDiagnosBetweenDates(fromDate, toDate,true);	    
	default:
	    return null;
	}
    }

    

}
