package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.histo.action.UserHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Eye;
import org.histo.config.enums.Month;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.StaticList;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.util.TimeUtil;
import org.histo.worklist.Worklist;
import org.histo.worklist.search.WorklistSearchBasic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class WorklistSearchDialogHandler extends AbstractDialog {

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private UtilDAO utilDAO;

	@Getter
	@Setter
	private WorklistSearchBasic worklistSearchBasic;

	private boolean initialized;

	private WorklistSearchFilter filterIndex;

	private ExtendedSearchData extendedSearchData;

	private List<MaterialPreset> materials;

	private List<Physician> surgeons;

	private List<Physician> privatePhysicians;

	private List<Physician> sigantures;

	private List<ListItem> caseHistoryList;

	private List<DiagnosisPreset> diagnoses;

	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.WORKLIST_SEARCH);

		
		
		// init only on first init
		if (!initialized) {
			
			setWorklistSearchBasic(new WorklistSearchBasic());
			
			setFilterIndex(WorklistSearchFilter.ADDED_TO_WORKLIST);


			setFilterIndex(WorklistSearchFilter.ADDED_TO_WORKLIST);

			setMaterials(settingsDAO.getAllMaterialPresets());

			setSurgeons(physicianDAO.getPhysicians(ContactRole.SURGEON, false));

			setPrivatePhysicians(physicianDAO.getPhysicians(ContactRole.PRIVATE_PHYSICIAN, false));

			setSigantures(physicianDAO.getPhysicians(ContactRole.SIGNATURE, false));

			setCaseHistoryList(settingsDAO.getAllStaticListItems(StaticList.CASE_HISTORY));

			setDiagnoses(utilDAO.getAllDiagnosisPrototypes());

			initialized = true;
		}

		setExtendedSearchData(new ExtendedSearchData());
		return true;
	}

	public Worklist extendedSearch() {

		logger.debug("Calling extended search");

		List<Patient> result = patientDao.getPatientByCriteria(getExtendedSearchData());

//		Worklist worklist = new Worklist("search", result, false,
//				userHandlerAction.getCurrentUser().getDefaultWorklistSortOrder(),
//				userHandlerAction.getCurrentUser().isWorklistAutoUpdate());
//
//		worklist.setShowActiveTasksExplicit(true);

		return null;
		// Worklist worklist = new Worklist("search", pat);
		//
		// System.out.println(test.size());

		// private String surgeon;
		// private String privatePhysician;
		// private String siganture;
		// private Eye eye;
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
	}

	// ************************ Getter/Setter ************************

	public WorklistSearchFilter getFilterIndex() {
		return filterIndex;
	}

	public void setFilterIndex(WorklistSearchFilter filterIndex) {
		this.filterIndex = filterIndex;
	}

	public ExtendedSearchData getExtendedSearchData() {
		return extendedSearchData;
	}

	public void setExtendedSearchData(ExtendedSearchData extendedSearchData) {
		this.extendedSearchData = extendedSearchData;
	}

	public List<MaterialPreset> getMaterials() {
		return materials;
	}

	public void setMaterials(List<MaterialPreset> materials) {
		this.materials = materials;
	}

	public List<Physician> getSigantures() {
		return sigantures;
	}

	public void setSigantures(List<Physician> sigantures) {
		this.sigantures = sigantures;
	}

	public List<Physician> getSurgeons() {
		return surgeons;
	}

	public void setSurgeons(List<Physician> surgeons) {
		this.surgeons = surgeons;
	}

	public List<Physician> getPrivatePhysicians() {
		return privatePhysicians;
	}

	public void setPrivatePhysicians(List<Physician> privatePhysicians) {
		this.privatePhysicians = privatePhysicians;
	}

	public List<ListItem> getCaseHistoryList() {
		return caseHistoryList;
	}

	public void setCaseHistoryList(List<ListItem> caseHistoryList) {
		this.caseHistoryList = caseHistoryList;
	}

	public List<DiagnosisPreset> getDiagnoses() {
		return diagnoses;
	}

	public void setDiagnoses(List<DiagnosisPreset> diagnoses) {
		this.diagnoses = diagnoses;
	}

	public class ExtendedSearchData {
		private String name;
		private String surename;
		private Date birthday;
		private Person.Gender gender;

		private String material;
		private String caseHistory;
		private String surgeon;
		private String privatePhysician;
		private String siganture;
		private Eye eye = Eye.UNKNOWN;

		private Date patientAdded;
		private Date patientAddedTo;

		private Date taskCreated;
		private Date taskCreatedTo;

		private Date stainingCompleted;
		private Date stainingCompletedTo;

		private Date diagnosisCompleted;
		private Date diagnosisCompletedTo;

		private Date dateOfReceipt;
		private Date dateOfReceiptTo;

		private Date dateOfSurgery;
		private Date dateOfSurgeryTo;

		private String diagnosis;
		private String category;
		private String malign;

		public String getDiagnosis() {
			return diagnosis;
		}

		public void setDiagnosis(String diagnosis) {
			this.diagnosis = diagnosis;
		}

		public String getCategory() {
			return category;
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public String getMalign() {
			return malign;
		}

		public void setMalign(String malign) {
			this.malign = malign;
		}

		public Date getPatientAdded() {
			return patientAdded;
		}

		public void setPatientAdded(Date patientAdded) {
			this.patientAdded = patientAdded;
		}

		public Date getPatientAddedTo() {
			return patientAddedTo;
		}

		public void setPatientAddedTo(Date patientAddedTo) {
			this.patientAddedTo = patientAddedTo;
		}

		public Date getTaskCreated() {
			return taskCreated;
		}

		public void setTaskCreated(Date taskCreated) {
			this.taskCreated = taskCreated;
		}

		public Date getTaskCreatedTo() {
			return taskCreatedTo;
		}

		public void setTaskCreatedTo(Date taskCreatedTo) {
			this.taskCreatedTo = taskCreatedTo;
		}

		public Date getStainingCompleted() {
			return stainingCompleted;
		}

		public void setStainingCompleted(Date stainingCompleted) {
			this.stainingCompleted = stainingCompleted;
		}

		public Date getStainingCompletedTo() {
			return stainingCompletedTo;
		}

		public void setStainingCompletedTo(Date stainingCompletedTo) {
			this.stainingCompletedTo = stainingCompletedTo;
		}

		public Date getDiagnosisCompleted() {
			return diagnosisCompleted;
		}

		public void setDiagnosisCompleted(Date diagnosisCompleted) {
			this.diagnosisCompleted = diagnosisCompleted;
		}

		public Date getDiagnosisCompletedTo() {
			return diagnosisCompletedTo;
		}

		public void setDiagnosisCompletedTo(Date diagnosisCompletedTo) {
			this.diagnosisCompletedTo = diagnosisCompletedTo;
		}

		public Date getDateOfReceipt() {
			return dateOfReceipt;
		}

		public void setDateOfReceipt(Date dateOfReceipt) {
			this.dateOfReceipt = dateOfReceipt;
		}

		public Date getDateOfReceiptTo() {
			return dateOfReceiptTo;
		}

		public void setDateOfReceiptTo(Date dateOfReceiptTo) {
			this.dateOfReceiptTo = dateOfReceiptTo;
		}

		public Date getDateOfSurgery() {
			return dateOfSurgery;
		}

		public void setDateOfSurgery(Date dateOfSurgery) {
			this.dateOfSurgery = dateOfSurgery;
		}

		public Date getDateOfSurgeryTo() {
			return dateOfSurgeryTo;
		}

		public void setDateOfSurgeryTo(Date dateOfSurgeryTo) {
			this.dateOfSurgeryTo = dateOfSurgeryTo;
		}

		public String getMaterial() {
			return material;
		}

		public void setMaterial(String material) {
			this.material = material;
		}

		public String getCaseHistory() {
			return caseHistory;
		}

		public void setCaseHistory(String caseHistory) {
			this.caseHistory = caseHistory;
		}

		public String getSurgeon() {
			return surgeon;
		}

		public void setSurgeon(String surgeon) {
			this.surgeon = surgeon;
		}

		public String getPrivatePhysician() {
			return privatePhysician;
		}

		public void setPrivatePhysician(String privatePhysician) {
			this.privatePhysician = privatePhysician;
		}

		public String getSiganture() {
			return siganture;
		}

		public void setSiganture(String siganture) {
			this.siganture = siganture;
		}

		public Eye getEye() {
			return eye;
		}

		public void setEye(Eye eye) {
			this.eye = eye;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSurename() {
			return surename;
		}

		public void setSurename(String surename) {
			this.surename = surename;
		}

		public Date getBirthday() {
			return birthday;
		}

		public void setBirthday(Date birthday) {
			this.birthday = birthday;
		}

		public Person.Gender getGender() {
			return gender;
		}

		public void setGender(Person.Gender gender) {
			this.gender = gender;
		}

	}
}
