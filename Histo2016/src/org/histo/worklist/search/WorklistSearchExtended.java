package org.histo.worklist.search;

import java.util.List;

import org.histo.config.enums.Eye;
import org.histo.dao.TaskDAO;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.service.dao.PatientDao;
import org.histo.service.dao.impl.PatientDaoImpl;
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

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

	@Override
	public List<Patient> getWorklist() {

		List<Task> tasks = taskDAO.getTaskByCriteria(this, true);

		List<Patient> patients = patientDao.findComplex(this, true, false, true);

		for (Task task : tasks) {

			for (Patient patient : patients) {
				if (task.getParent().getId() == patient.getId()) {
					for (Task pTask : patient.getTasks()) {
						if (pTask.getId() == task.getId())
							pTask.setActive(true);
					}
				}
			}
		}

		return patients;
	}

}

// private String name;
// private String surename;
// private Date birthday;
// private Person.Gender gender;
//
// private String material;
// private String caseHistory;
// private String surgeon;
// private String privatePhysician;
// private String siganture;
// private Eye eye = Eye.UNKNOWN;
//
// private Date patientAdded;
// private Date patientAddedTo;
//
// private Date taskCreated;
// private Date taskCreatedTo;
//
// private Date stainingCompleted;
// private Date stainingCompletedTo;
//
// private Date diagnosisCompleted;
// private Date diagnosisCompletedTo;
//
// private Date dateOfReceipt;
// private Date dateOfReceiptTo;
//
// private Date dateOfSurgery;
// private Date dateOfSurgeryTo;
//
// private String diagnosis;
// private String category;
// private String malign;