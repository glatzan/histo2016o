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

import org.apache.log4j.Logger;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisStatus;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Eye;
import org.histo.config.enums.NotificationStatus;
import org.histo.config.enums.StainingStatus;
import org.histo.config.enums.TaskPriority;
import org.histo.model.Accounting;
import org.histo.model.Contact;
import org.histo.model.Council;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.CreationDate;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.DiagnosisInfo;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.NotificationInfo;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.SaveAble;
import org.histo.model.interfaces.StainingInfo;
import org.histo.ui.StainingTableChooser;
import org.histo.util.TimeUtil;

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "task_sequencegenerator", sequenceName = "task_sequence")
public class Task implements Parent<Patient>, StainingInfo, DiagnosisInfo, NotificationInfo, DeleteAble, LogAble,
		SaveAble, HasDataList {

	private static Logger logger = Logger.getLogger("org.histo");
	
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
	 * The Patient of the task;
	 */
	private Patient parent;

	/**
	 * If true the program will provide default names for samples and blocks
	 */
	private boolean useAutoNomenclature;

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
	 * Priority of the task
	 */
	private TaskPriority taskPriority;

	/**
	 * The dueDate
	 */
	private long dueDate = 0;

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
	 * date of staining completion
	 */
	private long stainingCompletionDate = 0;

	/**
	 * Date of diagnosis finalization
	 */
	private long diagnosisCompletionDate = 0;

	/**
	 * The date of the completion of the notificaiton.
	 */
	private long notificationCompletionDate = 0;

	/**
	 * True if staining has to be performed
	 */
	private boolean stainingPhase;

	/**
	 * True if diagnosis has to be done
	 */
	private boolean diagnosisPhase;

	/**
	 * True if notification is pending
	 */
	private boolean notificationPhase;

	/**
	 * 
	 */
	private boolean finalized;

	/**
	 * Liste aller Personen die über die Diangose informiert werden sollen.
	 */
	private List<Contact> contacts;

	/**
	 * List with all samples
	 */
	private List<Sample> samples;

	/**
	 * Element containg all diangnoses
	 */
	private DiagnosisContainer diagnosisContainer;

	/**
	 * Generated PDFs of this task, lazy
	 */
	private List<PDFContainer> attachedPdfs;

	/**
	 * List of all councils of this task, lazy
	 */
	private List<Council> councils;

	private Accounting accounting;

	/********************************************************
	 * Transient Variables
	 ********************************************************/
	/**
	 * Die Ausgewählte Probe
	 */
	private Sample selectedSample;

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

	/**
	 * Initializes a task with important values.
	 * 
	 * @param parent
	 */
	public Task(Patient parent) {

		long currentDay = TimeUtil.setDayBeginning(System.currentTimeMillis());
		setCreationDate(currentDay);
		setDateOfReceipt(currentDay);
		setDueDate(currentDay);
		setDateOfSugery(currentDay);

		// 20xx -2000 = tasknumber
		setParent(parent);

	}

	/********************************************************
	 * Transient
	 ********************************************************/

	public void updateAllNames() {
		for (Sample sample : samples) {
			sample.updateAllNames(useAutoNomenclature);
		}
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
	public boolean isActiveOrActionPending() {
		if (isActive() || getDiagnosisStatus().getLevel() >= DiagnosisStatus.DIAGNOSIS_NEEDED.getLevel()
				|| getStainingStatus().getLevel() >= StainingStatus.STAINING_NEEDED.getLevel() || isNotificationPhase())
			return true;
		return false;
	}

	/**
	 * Creates linear list of all slides of the given task. The
	 * StainingTableChosser is used as holder class in order to offer an option
	 * to select the slides by clicking on a checkbox. Archived elements will
	 * not be shown if showArchived is false.
	 */
	@Transient
	public final void generateSlideGuiList() {
		generateSlideGuiList(false);
	}

	/**
	 * Creates linear list of all slides of the given task. The
	 * StainingTableChosser is used as holder class in order to offer an option
	 * to select the slides by clicking on a checkbox. Archived elements will
	 * not be shown if showArchived is false.
	 * 
	 * @param showArchived
	 */
	@Transient
	public final void generateSlideGuiList(boolean showArchived) {
		if (getStainingTableRows() == null)
			setStainingTableRows(new ArrayList<>());
		else
			getStainingTableRows().clear();

		boolean even = false;

		for (Sample sample : getSamples()) {
			// skips archived tasks

			StainingTableChooser sampleChooser = new StainingTableChooser(sample, even);
			getStainingTableRows().add(sampleChooser);

			for (Block block : sample.getBlocks()) {
				// skips archived blocks

				StainingTableChooser blockChooser = new StainingTableChooser(block, even);
				getStainingTableRows().add(blockChooser);
				sampleChooser.addChild(blockChooser);

				for (Slide staining : block.getSlides()) {
					// skips archived sliedes

					StainingTableChooser stainingChooser = new StainingTableChooser(staining, even);
					getStainingTableRows().add(stainingChooser);
					blockChooser.addChild(stainingChooser);
				}
			}
			even = !even;
		}
	}

	/**
	 * Checks if all staings are performed an returns true if the status has
	 * changed. If no change occurred false will be returned.
	 * 
	 * @return
	 */
	@Transient
	public boolean hasStatingStatusChanged() {
		logger.trace("Method: hasStatingStatusChanged()");
		// staining is performed and date = 0, so staining was performed
		// recently
		if (getStainingStatus() == StainingStatus.PERFORMED && getStainingCompletionDate() == 0) {
			setStainingCompletionDate(System.currentTimeMillis());
			setStainingPhase(false);
			return true;
			// the staining process was performed (date != 0), but there are new
			// slides to stain
		} else if (getStainingStatus() != StainingStatus.PERFORMED && getStainingCompletionDate() != 0) {
			setStainingCompletionDate(0);
			setStainingPhase(true);
			return true;
		}

		// status has not changed
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
	@OrderBy("creationDate DESC")
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

	public long getStainingCompletionDate() {
		return stainingCompletionDate;
	}

	public void setStainingCompletionDate(long stainingCompletionDate) {
		this.stainingCompletionDate = stainingCompletionDate;
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

	public long getNotificationCompletionDate() {
		return notificationCompletionDate;
	}

	public void setNotificationCompletionDate(long notificationCompletionDate) {
		this.notificationCompletionDate = notificationCompletionDate;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@OrderBy("dateOfRequest DESC")
	public List<Council> getCouncils() {
		return councils;
	}

	public void setCouncils(List<Council> councils) {
		this.councils = councils;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public Accounting getAccounting() {
		return accounting;
	}

	public void setAccounting(Accounting accounting) {
		this.accounting = accounting;
	}

	@OneToOne(mappedBy = "parent", fetch = FetchType.LAZY)
	public DiagnosisContainer getDiagnosisContainer() {
		return diagnosisContainer;
	}

	public void setDiagnosisContainer(DiagnosisContainer diagnosisContainer) {
		this.diagnosisContainer = diagnosisContainer;
	}

	public boolean isUseAutoNomenclature() {
		return useAutoNomenclature;
	}

	public void setUseAutoNomenclature(boolean useAutoNomenclature) {
		this.useAutoNomenclature = useAutoNomenclature;
	}

	public boolean isStainingPhase() {
		return stainingPhase;
	}

	public boolean isDiagnosisPhase() {
		return diagnosisPhase;
	}

	public boolean isNotificationPhase() {
		return notificationPhase;
	}

	public void setStainingPhase(boolean stainingPhase) {
		this.stainingPhase = stainingPhase;
	}

	public void setDiagnosisPhase(boolean diagnosisPhase) {
		this.diagnosisPhase = diagnosisPhase;
	}

	public void setNotificationPhase(boolean notificationPhase) {
		this.notificationPhase = notificationPhase;
	}

	public boolean isFinalized() {
		return finalized;
	}

	public void setFinalized(boolean finalized) {
		this.finalized = finalized;
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
	 * Returns true if priority is set to TaskPriority.Time
	 */
	@Transient
	public boolean isDueDateSelected() {
		if (getTaskPriority() == TaskPriority.TIME)
			return true;
		return false;
	}

	/**
	 * Sets if the given parameter is true TaskPriority.Time, if false
	 * TaskPriority.NONE
	 * 
	 * @param dueDate
	 */
	public void setDueDateSelected(boolean dueDate) {
		if (dueDate)
			setTaskPriority(TaskPriority.TIME);
		else
			setTaskPriority(TaskPriority.NONE);
	}

	/**
	 * Returns a report with the given type. If no matching record was found
	 * null will be returned.
	 * 
	 * @param type
	 * @return
	 */
	@Transient
	public PDFContainer getReport(String type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType().equals(type))
				return pdfContainer;
		}
		return null;
	}

	@Transient
	public PDFContainer getReportByName(String name) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getName().contains(name))
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
	public PDFContainer removeReport(String type) {
		for (PDFContainer pdfContainer : getAttachedPdfs()) {
			if (pdfContainer.getType().equals(type)) {
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
		// for (DiagnosisRevision diagnosisRevision : getReports()) {
		// if (diagnosisRevision.isMalign())
		// return true;
		// }
		return false;
	}

	/********************************************************
	 * Transient Getter/Setter
	 ********************************************************/

	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/
	/**
	 * Overwrites the {@link DiagnosisStatus} interfaces, and returns the status
	 * of the diagnoses.
	 */
	@Override
	@Transient
	public DiagnosisStatus getDiagnosisStatus() {
		DiagnosisStatus status = getDiagnosisContainer().getDiagnosisStatus();
		if (status == DiagnosisStatus.PERFORMED && isDiagnosisPhase())
			return DiagnosisStatus.STAY_IN_PHASE;

		return getDiagnosisContainer().getDiagnosisStatus();
	}

	/********************************************************
	 * Interface DiagnosisStatus
	 ********************************************************/

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/
	/**
	 * Returns the status of the staining process. Either it can return staining
	 * performed, staining needed, restaining needed (restaining is returned if
	 * at least one staining is marked as restaining).
	 */
	@Override
	@Transient
	public StainingStatus getStainingStatus() {
		// if empty return staining needed
		if (getSamples().isEmpty())
			return StainingStatus.STAINING_NEEDED;

		int level = StainingStatus.PERFORMED.getLevel();

		for (Sample sample : getSamples()) {

			if (sample.getStainingStatus().getLevel() > level)
				level = sample.getStainingStatus().getLevel();
		}

		if (StainingStatus.getStainingStatusByLevel(level) == StainingStatus.PERFORMED && isStainingPhase())
			return StainingStatus.STAY_IN_PHASE;

		return StainingStatus.getStainingStatusByLevel(level);
	}

	/********************************************************
	 * Interface StainingInfo
	 ********************************************************/

	/********************************************************
	 * Interface NotificationInfo
	 ********************************************************/

	/**
	 * Returns the status of the notification process
	 */
	@Transient
	@Override
	public NotificationStatus getNotificationStatus() {
		if (getNotificationCompletionDate() == 0 && isNotificationPhase())
			return NotificationStatus.NOTIFICATION_NEEDED;
		else if (getNotificationCompletionDate() != 0 && isNotificationPhase())
			return NotificationStatus.STAY_IN_PHASE;
		else if (getNotificationCompletionDate() != 0 && !isNotificationPhase())
			return NotificationStatus.PERFORMED;
		else
			return NotificationStatus.NOT_IN_PHASE;
	}

	/********************************************************
	 * Interface NotificationInfo
	 ********************************************************/

	/********************************************************
	 * Interface Parent
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
	 * Returns the parent task
	 */
	@Override
	@Transient
	public Task getTask() {
		return this;
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
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
	 * Interface Delete Able
	 ********************************************************/

	/********************************************************
	 * Interface SaveAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return "Task-ID: " + getTaskID() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface SaveAble
	 ********************************************************/

}
