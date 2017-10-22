package org.histo.worklist.search;

import java.util.Date;
import java.util.List;

import org.histo.config.enums.Eye;
import org.histo.model.Person;
import org.histo.model.immutable.patientmenu.PatientMenuModel;
import org.histo.model.patient.Patient;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class WorklistSearchExtended extends WorklistSearch {

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
	private String material;
	
	@Getter
	@Setter
	private String caseHistory;
	
	@Getter
	@Setter
	private String surgeon;
	
	@Getter
	@Setter
	private String privatePhysician;
	
	@Getter
	@Setter
	private String siganture;
	
	@Getter
	@Setter
	private Eye eye = Eye.UNKNOWN;

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
	private String diagnosis;
	
	@Getter
	@Setter
	private String category;
	
	@Getter
	@Setter
	private String malign;

	@Override
	public List<PatientMenuModel> getWorklist() {
		// TODO Auto-generated method stub
		return null;
	}

}
