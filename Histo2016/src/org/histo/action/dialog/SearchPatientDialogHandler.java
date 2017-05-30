package org.histo.action.dialog;

import java.util.Date;

import org.histo.config.enums.Dialog;
import org.histo.model.patient.Patient;
import org.histo.model.transitory.SearchOptions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SearchPatientDialogHandler extends AbstractDialog {

	private Patient patient;

	/**
	 * Patient to search for, piz
	 */
	private String patientPiz;

	/**
	 * Patient to search for, name
	 */
	private String patientName;

	/**
	 * Patient to search for, surname
	 */
	private String patientSurname;

	/**
	 * Patient to search for, birthday
	 */
	private Date patientBirthday;

	/**
	 * True if to many matches have been found in the clinic database, an so the
	 * clinic database did not return any data
	 */
	private boolean toManyMatchesInClinicDatabase;

	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public void initBean() {
		super.initBean(null, Dialog.WORKLIST_SEARCH);

		setPatientBirthday(new Date());
		setPatientName("");
		setPatientPiz("");
		setPatientSurname("");

		setToManyMatchesInClinicDatabase(false);
	}

	// ************************ Getter/Setter ************************
	public String getPatientPiz() {
		return patientPiz;
	}

	public void setPatientPiz(String patientPiz) {
		this.patientPiz = patientPiz;
	}

	public String getPatientName() {
		return patientName;
	}

	public void setPatientName(String patientName) {
		this.patientName = patientName;
	}

	public String getPatientSurname() {
		return patientSurname;
	}

	public void setPatientSurname(String patientSurname) {
		this.patientSurname = patientSurname;
	}

	public Date getPatientBirthday() {
		return patientBirthday;
	}

	public void setPatientBirthday(Date patientBirthday) {
		this.patientBirthday = patientBirthday;
	}

	public boolean isToManyMatchesInClinicDatabase() {
		return toManyMatchesInClinicDatabase;
	}

	public void setToManyMatchesInClinicDatabase(boolean toManyMatchesInClinicDatabase) {
		this.toManyMatchesInClinicDatabase = toManyMatchesInClinicDatabase;
	}

}
