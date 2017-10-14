package org.histo.model.patient;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
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
import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.Eye;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.TaskPriority;
import org.histo.config.hibernate.RootAware;
import org.histo.model.Accounting;
import org.histo.model.AssociatedContact;
import org.histo.model.Council;
import org.histo.model.FavouriteList;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.ui.StainingTableChooser;
import org.histo.util.TimeUtil;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "task_sequencegenerator", sequenceName = "task_sequence")
public class Task implements Parent<Patient>, LogAble, PatientRollbackAble, HasDataList, HasID, RootAware<Patient> {

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
	 * Station�r/ambulant/Extern
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
	private Eye eye = Eye.UNKNOWN;

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
	 * The date of the finalization.
	 */
	private long finalizationDate = 0;

	/**
	 * False if the task can't be edited
	 */
	private boolean editable = true;

	/**
	 * True if the task can't is completed
	 */
	private boolean finalized;

	/**
	 * Liste aller Personen die �ber die Diangose informiert werden sollen.
	 */
	private List<AssociatedContact> associatedContacts;

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

	/**
	 * List of all favorite Lists in which the task is listed
	 */
	private List<FavouriteList> favouriteLists;

	private Accounting accounting;

	/********************************************************
	 * Transient Variables
	 ********************************************************/
	/**
	 * Die Ausgew�hlte Probe
	 */
	private Sample selectedSample;

	/**
	 * Currently selected task in table form, transient, used for gui
	 */
	private ArrayList<StainingTableChooser<?>> stainingTableChoosers;

	/**
	 * If set to true, this task is shown in the navigation column on the left hand
	 * side, however there are actions to perform or not.
	 */
	private boolean active;

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

	@Transient
	public void updateAllNames() {
		updateAllNames(useAutoNomenclature, false);
	}

	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		getSamples().stream().forEach(p -> p.updateAllNames(useAutoNomenclature, ignoreManuallyNamedItems));
	}

	@Transient
	public AssociatedContact getPrimarySurgeon() {
		return getPrimaryContact(ContactRole.SURGEON);
	}

	@Transient
	public AssociatedContact getPrimaryPrivatePhysician() {
		return getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
	}

	/**
	 * Returns a associatedContact marked als primary with the given role.
	 * 
	 * @param contactRole
	 * @return
	 */
	@Transient
	public AssociatedContact getPrimaryContact(ContactRole contactRole) {
		for (AssociatedContact associatedContact : associatedContacts) {
			if (associatedContact.getRole() == contactRole)
				return associatedContact;
		}

		return null;
	}

	/**
	 * Creates linear list of all slides of the given task. The StainingTableChosser
	 * is used as holder class in order to offer an option to select the slides by
	 * clicking on a checkbox. Archived elements will not be shown if showArchived
	 * is false.
	 */
	@Transient
	public final void generateSlideGuiList() {
		generateSlideGuiList(false);
	}

	/**
	 * Creates linear list of all slides of the given task. The StainingTableChosser
	 * is used as holder class in order to offer an option to select the slides by
	 * clicking on a checkbox. Archived elements will not be shown if showArchived
	 * is false.
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

			StainingTableChooser<Sample> sampleChooser = new StainingTableChooser<Sample>(sample, even);
			getStainingTableRows().add(sampleChooser);

			for (Block block : sample.getBlocks()) {
				// skips archived blocks

				StainingTableChooser<Block> blockChooser = new StainingTableChooser<Block>(block, even);
				getStainingTableRows().add(blockChooser);
				sampleChooser.addChild(blockChooser);

				for (Slide staining : block.getSlides()) {
					// skips archived sliedes

					StainingTableChooser<Slide> stainingChooser = new StainingTableChooser<Slide>(staining, even);
					getStainingTableRows().add(stainingChooser);
					blockChooser.addChild(stainingChooser);
				}
			}

			even = !even;
		}
	}

	@Transient
	public boolean isListedInFavouriteList(PredefinedFavouriteList... predefinedFavouriteLists) {
		long[] arr = new long[predefinedFavouriteLists.length];
		int i = 0;
		for (PredefinedFavouriteList predefinedFavouriteList : predefinedFavouriteLists) {
			arr[i] = predefinedFavouriteList.getId();
			i++;
		}

		return isListedInFavouriteList(arr);
	}

	@Transient
	public boolean isListedInFavouriteList(long... idArr) {
		for (long id : idArr) {
			if (getFavouriteLists().stream().anyMatch(p -> p.getId() == id))
				return true;
		}
		return false;
	}

	@Override
	@Transient
	public String toString() {
		return "ID: " + getId() + ", Task ID: " + getTaskID();
	}

	@Transient
	public boolean isActiveOrActionPending() {
		return isActiveOrActionPending(false);
	}

	/**
	 * Returns true if the task is marked as active or an action is pending. If
	 * activeOnly is true only the active attribute of the task will be evaluated.
	 * 
	 * @param task
	 * @return
	 */
	@Transient
	public boolean isActiveOrActionPending(boolean activeOnly) {
		if (activeOnly)
			return isActive();

		if (isActive())
			return true;

		if (isListedInFavouriteList(PredefinedFavouriteList.StainingList, PredefinedFavouriteList.ReStainingList,
				PredefinedFavouriteList.StayInStainingList, PredefinedFavouriteList.DiagnosisList,
				PredefinedFavouriteList.ReDiagnosisList, PredefinedFavouriteList.StayInDiagnosisList,
				PredefinedFavouriteList.NotificationList, PredefinedFavouriteList.StayInNotificationList))
			return true;

		return false;
	}

	@Transient
	@Override
	public boolean equals(Object obj) {

		if (obj instanceof Task && ((Task) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}

	@Transient
	public boolean containsContact(Person person) {
		if (getContacts() != null)
			return getContacts().stream().anyMatch(p -> p.getPerson().equals(person));
		return false;
	}

	@Transient
	public boolean containsContact(AssociatedContact associatedContact) {
		if (getContacts() != null)
			return getContacts().stream().anyMatch(p -> p.equals(associatedContact));
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

	// TODO fetch lazy
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "task")
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("id ASC")
	@NotAudited
	public List<AssociatedContact> getContacts() {
		if (associatedContacts == null)
			associatedContacts = new ArrayList<AssociatedContact>();
		return associatedContacts;
	}

	public void setContacts(List<AssociatedContact> associatedContacts) {
		this.associatedContacts = associatedContacts;
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

	@Column
	public String getTaskID() {
		return taskID;
	}

	public void setTaskID(String taskID) {
		this.taskID = taskID;
	}

	@Column
	public long getDateOfReceipt() {
		return dateOfReceipt;
	}

	public void setDateOfReceipt(long dateOfReceipt) {
		this.dateOfReceipt = dateOfReceipt;
	}

	@Column
	public long getDueDate() {
		return dueDate;
	}

	public void setDueDate(long dueDate) {
		this.dueDate = dueDate;
	}

	@Column
	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Column
	public long getDateOfSugery() {
		return dateOfSugery;
	}

	public void setDateOfSugery(long dateOfSugery) {
		this.dateOfSugery = dateOfSugery;
	}

	@Column
	public byte getTypeOfOperation() {
		return typeOfOperation;
	}

	public void setTypeOfOperation(byte typeOfOperation) {
		this.typeOfOperation = typeOfOperation;
	}

	@Column
	@Type(type = "text")
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

	@Column
	public long getStainingCompletionDate() {
		return stainingCompletionDate;
	}

	public void setStainingCompletionDate(long stainingCompletionDate) {
		this.stainingCompletionDate = stainingCompletionDate;
	}

	@Column
	public long getDiagnosisCompletionDate() {
		return diagnosisCompletionDate;
	}

	public void setDiagnosisCompletionDate(long diagnosisCompletionDate) {
		this.diagnosisCompletionDate = diagnosisCompletionDate;
	}

	@Column
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

	@Column
	public long getNotificationCompletionDate() {
		return notificationCompletionDate;
	}

	public void setNotificationCompletionDate(long notificationCompletionDate) {
		this.notificationCompletionDate = notificationCompletionDate;
	}

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "task")
	@OrderBy("dateOfRequest DESC")
	@Fetch(value = FetchMode.SUBSELECT)
	public List<Council> getCouncils() {
		return councils;
	}

	public void setCouncils(List<Council> councils) {
		this.councils = councils;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@NotAudited
	public List<FavouriteList> getFavouriteLists() {
		return favouriteLists;
	}

	public void setFavouriteLists(List<FavouriteList> favouriteLists) {
		this.favouriteLists = favouriteLists;
	}

	@OneToOne(fetch = FetchType.LAZY)
	public Accounting getAccounting() {
		return accounting;
	}

	public void setAccounting(Accounting accounting) {
		this.accounting = accounting;
	}

	@OneToOne(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	public DiagnosisContainer getDiagnosisContainer() {
		return diagnosisContainer;
	}

	public void setDiagnosisContainer(DiagnosisContainer diagnosisContainer) {
		this.diagnosisContainer = diagnosisContainer;
	}

	@Column
	public boolean isUseAutoNomenclature() {
		return useAutoNomenclature;
	}

	public void setUseAutoNomenclature(boolean useAutoNomenclature) {
		this.useAutoNomenclature = useAutoNomenclature;
	}

	@Column
	public boolean isFinalized() {
		return finalized;
	}

	public void setFinalized(boolean finalized) {
		this.finalized = finalized;
	}

	@Column

	public long getFinalizationDate() {
		return finalizationDate;
	}

	public void setFinalizationDate(long finalizationDate) {
		this.finalizationDate = finalizationDate;
	}

	@Column
	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
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
	public ArrayList<StainingTableChooser<?>> getStainingTableRows() {
		return stainingTableChoosers;
	}

	public void setStainingTableRows(ArrayList<StainingTableChooser<?>> stainingTableChoosers) {
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
	 * Returns a report with the given type. If no matching record was found null
	 * will be returned.
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

	/********************************************************
	 * Transient Getter/Setter
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
	 * �berschreibt Methode aus dem Interface StainingTreeParent
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
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return "Task-ID: " + getTaskID() + " (" + getId() + ")";
	}

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/

	@Override
	@Transient
	public String getDatalistIdentifier() {
		return "interface.hasDataList.task";
	}

	@Override
	@Transient
	public Patient root() {
		return parent;
	}
}
