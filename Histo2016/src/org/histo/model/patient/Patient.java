package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
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
import org.histo.model.Person;
import org.histo.model.util.DiagnosisStatus;
import org.histo.model.util.LogAble;
import org.histo.model.util.StainingStatus;
import org.histo.model.util.StainingTreeParent;
import org.histo.util.TimeUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "patient_sequencegenerator", sequenceName = "patient_sequence")
public class Patient implements StainingTreeParent<Patient>, DiagnosisStatus, StainingStatus, LogAble {

	@Expose
	private long id;

	private long version;
	
	/**
	 * PIZ
	 */
	@Expose
	private String piz = "";

	/**
	 * Insurance of the patient
	 */
	@Expose
	private String insurance = "";

	/**
	 * True if insurance is private
	 */
	private boolean privateInsurance;
	
	/**
	 * True if patient was added as an external patient.
	 */
	@Expose
	private boolean externalPatient = false;

	/**
	 * Date of adding to the database
	 */
	@Expose
	private Date addDate = null;

	/**
	 * Person data
	 */
	@Expose
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
	 * Wenn archived true ist, wird dieser patient nicht mehr angezeigt
	 */
	private boolean archived;

	@Transient
	public String asGson() {
		final GsonBuilder builder = new GsonBuilder();
		builder.excludeFieldsWithoutExposeAnnotation();
		final Gson gson = builder.create();
		return gson.toJson(this);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
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

	@OneToMany(cascade = {CascadeType.REFRESH, CascadeType.ALL}, mappedBy = "parent", fetch = FetchType.EAGER)
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
	public Date getAddDate() {
		return addDate;
	}

	public void setAddDate(Date addDate) {
		this.addDate = addDate;
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
	/********************************************************
	 * Getter/Setter
	 ********************************************************/


	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/
	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn alle Diagnosen finalisiert wurden.
	 */
	@Override
	@Transient
	public boolean isDiagnosisPerformed() {
		for (Task task : tasks) {
			
			if (task.isArchived())
				continue;

			if (!task.isDiagnosisPerformed())
				return false;
		}
		return true;
	}

	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn mindestens eine Dinagnose nicht finalisiert wurde.
	 */
	@Override
	@Transient
	public boolean isDiagnosisNeeded() {
		for (Task task : tasks) {
			
			if (task.isArchived())
				continue;

			if (task.isDiagnosisNeeded())
				return true;
		}
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface DiagnosisStatus <br>
	 * Gibt true zurück wenn mindestens eine Dinagnose nicht finalisiert wurde.
	 */
	@Override
	@Transient
	public boolean isReDiagnosisNeeded() {
		for (Task task : tasks) {
			
			if (task.isArchived())
				continue;

			if (task.isReDiagnosisNeeded())
				return true;
		}
		return false;
	}

	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/

	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/
	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn der Patient oder der Auftrag heute erstellt wurde.
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (TimeUtil.isDateOnSameDay(getAddDate(), new Date(System.currentTimeMillis())))
			return true;

		for (Task task : getTasks()) {
			if (task.isNew())
				return true;
		}
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn die Probe am heutigen Tag erstellt wrude
	 */
	@Transient
	@Override
	public boolean isStainingPerformed() {
		for (Task task : getTasks()) {

			if (task.isArchived())
				continue;

			if (!task.isStainingPerformed())
				return false;
		}
		return true;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht und die Batchnumber
	 * == 0 ist.
	 */
	@Override
	@Transient
	public boolean isStainingNeeded() {
		for (Task task : getTasks()) {

			if (task.isArchived())
				continue;

			if (task.isStainingNeeded())
				return true;
		}
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zrück wenn mindestens eine Färbung aussteht und die Batchnumber
	 * > 0 ist.
	 */
	@Override
	@Transient
	public boolean isReStainingNeeded() {
		for (Task task : getTasks()) {

			if (task.isArchived())
				continue;

			if (task.isReStainingNeeded())
				return true;
		}
		return false;
	}

	/********************************************************
	 * Interface StainingStauts
	 ********************************************************/

	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
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
	public String getArchiveDialog() {
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
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/
	@Transient
	public ArrayList<Task> getActivTasks(){
		ArrayList<Task> result = new ArrayList<>();
		for (Task task : tasks) {
			if(task.isArchived())
				continue;
			
			if(task.isCurrentlyActive() || task.isDiagnosisNeeded() || task.isReDiagnosisNeeded() || task.isStainingNeeded() || task.isReStainingNeeded())
				result.add(task);
		}
		
		return result;
	}

}
