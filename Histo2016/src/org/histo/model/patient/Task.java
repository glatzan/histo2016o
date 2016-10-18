package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
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
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Eye;
import org.histo.config.enums.PdfTemplate;
import org.histo.config.enums.TaskPriority;
import org.histo.model.Accounting;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.Report;
import org.histo.model.util.DiagnosisStatus;
import org.histo.model.util.LogAble;
import org.histo.model.util.StainingStatus;
import org.histo.model.util.TaskTree;
import org.histo.ui.StainingTableChooser;
import org.histo.util.TimeUtil;
import org.primefaces.component.tabview.TabView;
import org.primefaces.event.TabChangeEvent;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "task_sequencegenerator", sequenceName = "task_sequence")
public class Task implements TaskTree<Patient>, StainingStatus, DiagnosisStatus, LogAble {

	public static final int TAB_DIAGNOSIS = 0;
	public static final int TAB_STAINIG = 1;

	public static final byte EYE_RIGHT = 0;
	public static final byte EYE_LEFT = 1;

	private long id;

	private long version;

	/**
	 * Generated Task ID as String
	 */
	private String taskID = "";

	/**
	 * Priority of the task
	 */
	private TaskPriority taskPriority;

	/**
	 * The Patient of the task;
	 */
	private Patient parent;

	/**
	 * Date of creation
	 */
	private long creationDate = 0;

	/**
	 * The date of the sugery
	 */
	private long dateOfSugery = 0;

	/**
	 * Date of reception of the first material
	 */
	private long dateOfReceipt = 0;

	/**
	 * The dueDate
	 */
	private long dueDate = 0;

	/**
	 * If a dueDate is given
	 */
	private boolean dueDateSelected = false;

	/**
	 * Liste aller Personen die über die Diangose informiert werden sollen.
	 */
	private List<Contact> contacts;

	/**
	 * Stationär/ambulant/Extern
	 */
	private byte typeOfOperation;

	/**
	 * Details of the case
	 */
	private String caseHistory = "";

	/**
	 * Ward of the patient
	 */
	private String ward = "";

	/**
	 * Ey of the samples right/left/both
	 */
	private Eye eye = Eye.RIGHT;

	/**
	 * List with all samples
	 */
	private List<Sample> samples;

	/**
	 * Der Task ist archiviert und wird nicht mehr angezeigt wenn true
	 */
	private boolean archived = false;

	/**
	 * all stainings completed
	 */
	private boolean stainingCompleted = false;

	/**
	 * date of staining completion
	 */
	private long stainingCompletionDate = 0;

	/**
	 * True if every diagnosis is finalized
	 */
	private boolean diagnosisCompleted = false;

	/**
	 * Date of diagnosis finalization
	 */
	private long diagnosisCompletionDate = 0;

	/**
	 * True if all persons within the contact list have been notified about the
	 * result.
	 */
	private boolean notificationCompleted = false;

	/**
	 * The date of the completion of the notificaiton.
	 */
	private long notificationCompletionDate = 0;

	/**
	 * Generated PDFs of this task
	 */
	private List<PDFContainer> attachedPdfs;

	private Accounting accounting;

	private Council council;

	private Report report;
	
	/********************************************************
	 * Transient Variables
	 ********************************************************/
	/**
	 * Die Ausgewählte Probe
	 */
	private Sample selectedSample;

	/**
	 * Der Index des TabViews 0 = Diangosen Tab 1 = Objektträger Tab
	 */
	private int tabIndex;

	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	private ArrayList<StainingTableChooser> stainingTableChoosers;

	/**
	 * If set to true, this task is shown in the navigation column on the left
	 * hand side, however there are actions to perform or not.
	 */
	private boolean active;

	/**
	 * True if lazy initialision was successful.
	 */
	private boolean initialized;

	/********************************************************
	 * Transient Variables
	 ********************************************************/

	public Task() {
	}

	public Task(Patient parent) {
		this.parent = parent;
	}

	/********************************************************
	 * Transient
	 ********************************************************/
	/**
	 * Updated den Tabindex wenn ein andere Tab (Diagnose oder Färbung) in der
	 * Gui ausgewählt wurde TODO: Remove or use
	 * 
	 * @param event
	 */
	public void onTabChange(TabChangeEvent event) {
		setTabIndex(((TabView) event.getSource()).getIndex());
	}

	/**
	 * Returns a contact marked als primary with the given role.
	 * 
	 * @param contactRole
	 * @return
	 */
	@Transient
	public Contact getPrimaryContact(ContactRole contactRole) {
		for (Contact contact : contacts) {
			if (contact.getRole() == contactRole && contact.isPrimaryContact())
				return contact;
		}

		return null;
	}

	/**
	 * Returns true if either the task is active or a diagnosis or a staining is
	 * needed.
	 * 
	 * @return
	 */
	@Transient
	public boolean isActiveOrActionToPerform() {
		return isActive() || isDiagnosisNeeded() || isReDiagnosisNeeded() || isStainingNeeded()
				|| isReDiagnosisNeeded();
	}

	@Transient
	public boolean isNewAndActionsPending() {
		if (isNew() && (!isStainingCompleted() || !isDiagnosisCompleted()))
			return true;
		return false;
	}

	/********************************************************
	 * Transient
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "task_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	@NotAudited
	public List<Contact> getContacts() {
		if (contacts == null)
			contacts = new ArrayList<Contact>();
		return contacts;
	}

	public void setContacts(List<Contact> contacts) {
		this.contacts = contacts;
	}

	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	public List<Sample> getSamples() {
		if (samples == null)
			samples = new ArrayList<Sample>();
		return samples;
	}

	public void setSamples(List<Sample> samples) {
		this.samples = samples;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	public List<PDFContainer> getAttachedPdfs() {
		if (attachedPdfs == null)
			attachedPdfs = new ArrayList<PDFContainer>();
		return attachedPdfs;
	}

	public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
		this.attachedPdfs = attachedPdfs;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}

	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	public long getDateOfReceipt() {
		return dateOfReceipt;
	}

	public void setDateOfReceipt(long dateOfReceipt) {
		this.dateOfReceipt = dateOfReceipt;
	}

	public long getDueDate() {
		return dueDate;
	}

	public void setDueDate(long dueDate) {
		this.dueDate = dueDate;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public long getDateOfSugery() {
		return dateOfSugery;
	}

	public void setDateOfSugery(long dateOfSugery) {
		this.dateOfSugery = dateOfSugery;
	}

	public byte getTypeOfOperation() {
		return typeOfOperation;
	}

	public void setTypeOfOperation(byte typeOfOperation) {
		this.typeOfOperation = typeOfOperation;
	}

	public String getCaseHistory() {
		return caseHistory;
	}

	public void setCaseHistory(String caseHistory) {
		this.caseHistory = caseHistory;
	}

	@Enumerated(EnumType.STRING)
	public Eye getEye() {
		return eye;
	}

	public void setEye(Eye eye) {
		this.eye = eye;
	}

	public boolean isDueDateSelected() {
		return dueDateSelected;
	}

	public void setDueDateSelected(boolean dueDateSelected) {
		this.dueDateSelected = dueDateSelected;
	}

	public boolean isStainingCompleted() {
		return stainingCompleted;
	}

	public void setStainingCompleted(boolean stainingCompleted) {
		this.stainingCompleted = stainingCompleted;
	}

	public long getStainingCompletionDate() {
		return stainingCompletionDate;
	}

	public void setStainingCompletionDate(long stainingCompletionDate) {
		this.stainingCompletionDate = stainingCompletionDate;
	}

	public boolean isDiagnosisCompleted() {
		return diagnosisCompleted;
	}

	public void setDiagnosisCompleted(boolean diagnosisCompleted) {
		this.diagnosisCompleted = diagnosisCompleted;
	}

	public long getDiagnosisCompletionDate() {
		return diagnosisCompletionDate;
	}

	public void setDiagnosisCompletionDate(long diagnosisCompletionDate) {
		this.diagnosisCompletionDate = diagnosisCompletionDate;
	}

	public String getWard() {
		return ward;
	}

	public void setWard(String ward) {
		this.ward = ward;
	}

	@Enumerated(EnumType.ORDINAL)
	public TaskPriority getTaskPriority() {
		return taskPriority;
	}

	public void setTaskPriority(TaskPriority taskPriority) {
		this.taskPriority = taskPriority;
	}

	public boolean isNotificationCompleted() {
		return notificationCompleted;
	}

	public void setNotificationCompleted(boolean notificationCompleted) {
		this.notificationCompleted = notificationCompleted;
	}

	public long getNotificationCompletionDate() {
		return notificationCompletionDate;
	}

	public void setNotificationCompletionDate(long notificationCompletionDate) {
		this.notificationCompletionDate = notificationCompletionDate;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public Council getCouncil() {
		return council;
	}

	public void setCouncil(Council council) {
		this.council = council;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public Accounting getAccounting() {
		return accounting;
	}

	public void setAccounting(Accounting accounting) {
		this.accounting = accounting;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public Report getReport() {
		if(report == null)
			report = new Report();
		return report;
	}

	public void setReport(Report report) {
		this.report = report;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/


	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/
	@Transient
	public Date getCreationDateAsDate() {
		return new Date(getCreationDate());
	}

	public void setCreationDateAsDate(Date date) {
		setCreationDate(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDateOfSugeryAsDate() {
		return new Date(getDateOfSugery());
	}

	public void setDateOfSugeryAsDate(Date date) {
		setDateOfSugery(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Date getDateOfReceiptAsDate() {
		return new Date(getDateOfReceipt());
	}

	public void setDateOfReceiptAsDate(Date date) {
		setDateOfReceipt(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public Sample getSelectedSample() {
		return selectedSample;
	}

	public void setSelectedSample(Sample selectedSample) {
		this.selectedSample = selectedSample;
	}

	@Transient
	public ArrayList<StainingTableChooser> getStainingTableRows() {
		return stainingTableChoosers;
	}

	public void setStainingTableRows(ArrayList<StainingTableChooser> stainingTableChoosers) {
		this.stainingTableChoosers = stainingTableChoosers;
	}

	@Transient
	public int getTabIndex() {
		return tabIndex;
	}

	public void setTabIndex(int tabIndex) {
		this.tabIndex = tabIndex;
	}

	@Transient
	public Date getDueDateAsDate() {
		return new Date(getDueDate());
	}

	public void setDueDateAsDate(Date date) {
		setDueDate(TimeUtil.setDayBeginning(date).getTime());
	}

	@Transient
	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Transient
	public boolean isInitialized() {
		return initialized;
	}

	public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}

	/**
	 * Returns a report with the given type. If no matching record was found
	 * null will be returned.
	 * 
	 * @param type
	 * @return
	 */
	@Transient
	public PDFContainer getReport(PdfTemplate type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType() == type)
				return pdfContainer;
		}
		return null;
	}

	/**
	 * Adds a report to the report list
	 * 
	 * @return
	 */
	@Transient
	public void addReport(PDFContainer pdfTemplate) {
		getAttachedPdfs().add(pdfTemplate);
	}

	/**
	 * Removes a report with a specific type from the database
	 * 
	 * @param type
	 * @return
	 */
	@Transient
	public PDFContainer removeReport(PdfTemplate type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType() == type) {
				getAttachedPdfs().remove(pdfContainer);
				return pdfContainer;
			}
		}
		return null;
	}

	/**
	 * Returns true if a diagnosis is marked as malign.
	 * 
	 * @return
	 */
	@Transient
	public boolean isMalign() {
		for (Sample sample : getSamples()) {
			if (sample.isMalign())
				return true;
		}
		return false;
	}

	/********************************************************
	 * Transient Getter/Setter
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
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (!sample.isDiagnosisPerformed())
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
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isDiagnosisNeeded())
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
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isReDiagnosisNeeded())
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
	 * Gibt true zurück, wenn die Aufgabe am heutigen Tag erstellt wurde
	 */
	@Override
	@Transient
	public boolean isNew() {
		if (TimeUtil.isDateOnSameDay(creationDate, System.currentTimeMillis()))
			return true;
		return false;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingStauts <br>
	 * Gibt true zurück, wenn die Probe am heutigen Tag erstellt wrude
	 */
	@Transient
	@Override
	public boolean isStainingPerformed() {
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (!sample.isStainingPerformed())
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
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isStainingNeeded())
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
		for (Sample sample : samples) {

			if (sample.isArchived())
				continue;

			if (sample.isReStainingNeeded())
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
	@ManyToOne(targetEntity = Patient.class)
	public Patient getParent() {
		return parent;
	}

	public void setParent(Patient parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble
	 */
	@Basic
	public boolean isArchived() {
		return archived;
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Setzt alle Kinder
	 */
	public void setArchived(boolean archived) {
		this.archived = archived;
		// setzt Kinder
		for (Sample sample : getSamples()) {
			sample.setArchived(archived);
		}
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt die TaskID als
	 * identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return getTaskID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zurück
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.TASK_ARCHIV;
	}
	/********************************************************
	 * Interface StainingTreeParent
	 ********************************************************/

}
