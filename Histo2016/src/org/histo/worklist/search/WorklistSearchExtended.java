package org.histo.worklist.search;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Eye;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Configurable
public class WorklistSearchExtended extends WorklistSearch {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	/**
	 * Name of Material
	 */
	private String material;

	/**
	 * List of physicians
	 */
	private Physician[] surgeons;

	/**
	 * List of physicians
	 */
	private Physician[] signature;

	/**
	 * Case history
	 */
	private String caseHistory;

	/**
	 * Diagnosis text
	 */
	private String diagnosisText;

	/**
	 * Diagnosis
	 */
	private String diagnosis;

	/**
	 * Malign, tri state, 0 = nothing, 1= true, 2 = false
	 */
	private String malign = "0";

	/**
	 * Eye
	 */
	private Eye eye = Eye.UNKNOWN;

	/**
	 * ward
	 */
	private String ward;

	/**
	 * List of stainings
	 */
	private List<StainingPrototype> stainings;
	
	@Getter
	@Setter
	private String name;

	@Getter
	@Setter
	private String surename;

	@Getter
	@Setter
	private Date birthday;

	@Getter
	@Setter
	private Person.Gender gender;

	@Getter
	@Setter
	private Date patientAdded;

	@Getter
	@Setter
	private Date patientAddedTo;

	@Getter
	@Setter
	private Date taskCreated;

	@Getter
	@Setter
	private Date taskCreatedTo;

	@Getter
	@Setter
	private Date stainingCompleted;

	@Getter
	@Setter
	private Date stainingCompletedTo;

	@Getter
	@Setter
	private Date diagnosisCompleted;

	@Getter
	@Setter
	private Date diagnosisCompletedTo;

	@Getter
	@Setter
	private Date dateOfReceipt;

	@Getter
	@Setter
	private Date dateOfReceiptTo;

	@Getter
	@Setter
	private Date dateOfSurgery;

	@Getter
	@Setter
	private Date dateOfSurgeryTo;

	@Getter
	@Setter
	private String category;

	@Override
	public List<Patient> getWorklist() {
		ArrayList<Patient> result = new ArrayList<Patient>();
		result.addAll(patientDao.getPatientByCriteria(this, true));
		return result;
	}

}
