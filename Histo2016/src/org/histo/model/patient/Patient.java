package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.DiagnosisStatus;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.StainingStatus;
import org.histo.model.Person;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.DiagnosisInfo;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.StainingInfo;
import org.histo.util.TimeUtil;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "patient_sequencegenerator", sequenceName = "patient_sequence")
public class Patient implements Parent<Patient>, DiagnosisInfo<Task>, StainingInfo<Task>, CreationDate, LogAble, ArchivAble {

	private long id;

	private long version;

	/**
	 * PIZ
	 */
	private String piz = "";

	/**
	 * Insurance of the patient
	 */
	private String insurance = "";

	/**
	 * Date of adding to the database
	 */
	private long creationDate = 0;

	/**
	 * Person data
	 */
	private Person person;

	/**
	 * Task for this patient
	 */
	private List<Task> tasks;

	/**
	 * Currently selected task, transient
	 */
	private Task selectedTask;

	/**
	 * True if insurance is private
	 */
	private boolean privateInsurance = false;

	/**
	 * True if patient was added as an external patient.
	 */
	private boolean externalPatient = false;

	/**
	 * If true the patient is archived. Thus he won't be displayed.
	 */
	private boolean archived = false;

	public Patient() {
		setCreationDate(0);
	}
	/*
	 * ************************** Transient ****************************
	 */

	/**
	 * Returns a list with all currently active tasks
	 * 
	 * @return
	 */
	@Transient
	public ArrayList<Task> getActivTasks() {
		ArrayList<Task> result = new ArrayList<Task>();
		for (Task task : tasks) {
			if (task.isArchived())
				continue;

			if (task.isActiveOrActionToPerform())
				result.add(task);
		}

		return result;
	}

	/**
	 * Returns a list with tasks which are not active
	 * 
	 * @return
	 */
	@Transient
	public ArrayList<Task> getNoneActivTasks() {
		ArrayList<Task> result = new ArrayList<Task>(getTasks());
		result.removeAll(getActivTasks());
		return result.isEmpty() ? null : result;
	}

	/*
	 * ************************** Transient ****************************
	 */

	/*
	 * ************************** Getter/Setter ****************************
	 */
	@Override
	@Id
	@GeneratedValue(generator = "patient_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	@Column
	public String getPiz() {
		return piz;
	}

	public void setPiz(String piz) {
		this.piz = piz;
	}

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id DESC")
	public List<Task> getTasks() {
		if (tasks == null)
			tasks = new ArrayList<Task>();
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	@OneToOne(cascade = CascadeType.ALL)
	@NotAudited
	public Person getPerson() {
		return person;
	}

	public void setPerson(Person person) {
		this.person = person;
	}

	@Basic
	public boolean isExternalPatient() {
		return externalPatient;
	}

	public void setExternalPatient(boolean externalPatient) {
		this.externalPatient = externalPatient;
	}

	@Column
	@Override
	public long getCreationDate() {
		return creationDate;
	}

	@Override
	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Column
	public String getInsurance() {
		return insurance;
	}

	public void setInsurance(String insurance) {
		this.insurance = insurance;
	}

	@Transient
	public Task getSelectedTask() {
		return selectedTask;
	}

	public void setSelectedTask(Task selectedTask) {
		this.selectedTask = selectedTask;
	}

	public boolean isPrivateInsurance() {
		return privateInsurance;
	}

	public void setPrivateInsurance(boolean privateInsurance) {
		this.privateInsurance = privateInsurance;
	}

	/*
	 * ************************** Getter/Setter ****************************
	 */

	/********************************************************
	 * Interface DiagnosisInfo
	 ********************************************************/
	/**
	 * Overwrites the {@link DiagnosisInfo} interfaces, and returns the status
	 * of the diagnoses.
	 */
	@Override
	@Transient
	public DiagnosisStatus getDiagnosisStatus() {
		return getDiagnosisStatus(getTasks());
	}

	/********************************************************
	 * Interface DiagnosisInfo
	 ********************************************************/

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/
	/**
	 * Overwrites the {@link StainingInfo} interfaces new method. Returns true
	 * if the creation date was on the same as the current day.
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (isNew(getCreationDate()))
			return true;

		for (Task task : getTasks()) {
			if (task.isNew())
				return true;
		}
		return false;
	}

	/**
	 * Returns the status of the staining process. Either it can return staining
	 * performed, staining needed, restaining needed (restaining is returned if
	 * at least one staining is marked as restaining).
	 */
	@Override
	@Transient
	public StainingStatus getStainingStatus() {
		return getStainingStatus(getTasks());
	}

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/

	/*
	 * ************************** Interface StainingTreeParent
	 * ****************************
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return this;
	}

	@Override
	public boolean isArchived() {
		return archived;
	}

	@Override
	public void setArchived(boolean archived) {
		this.archived = archived;
	}

	@Transient
	@Override
	public String getTextIdentifier() {
		return null;
	}

	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return null;
	}

	@Transient
	@Override
	public Patient getParent() {
		return null;
	}

	@Override
	public void setParent(Patient parent) {
	}
	/*
	 * ************************** Interface StainingTreeParent
	 * ****************************
	 */
}
