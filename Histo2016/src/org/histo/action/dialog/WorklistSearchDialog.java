package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractTabDialog.AbstractTab;
import org.histo.action.dialog.UserSettingsDialog.FavouriteListTab;
import org.histo.action.dialog.UserSettingsDialog.GeneralTab;
import org.histo.action.dialog.UserSettingsDialog.PrinterTab;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Eye;
import org.histo.config.enums.WorklistSearchFilter;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.patient.Patient;
import org.histo.ui.FavouriteListContainer;
import org.histo.worklist.Worklist;
import org.histo.worklist.search.WorklistFavouriteSearch;
import org.histo.worklist.search.WorklistSimpleSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class WorklistSearchDialog extends AbstractTabDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianDAO physicianDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	private SimpleSearchTab simpleSearchTab;
	private FavouriteSearchTab favouriteSearchTab;
	private ExtendedSearchTab extendedSearchTab;

	public WorklistSearchDialog() {
		setSimpleSearchTab(new SimpleSearchTab());
		setFavouriteSearchTab(new FavouriteSearchTab());
		setExtendedSearchTab(new ExtendedSearchTab());

		tabs = new AbstractTab[] { simpleSearchTab, favouriteSearchTab, extendedSearchTab };
	}

	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.WORKLIST_SEARCH);

		for (int i = 0; i < tabs.length; i++) {
			tabs[i].initTab();
		}

		if (activeIndex >= 0 && activeIndex < getTabs().length) {
			onTabChange(null);
		}

		return true;
	}

	@Getter
	@Setter
	public class SimpleSearchTab extends AbstractTab {

		private WorklistSimpleSearch worklistSearch;

		public SimpleSearchTab() {
			setTabName("SimpleSearchTab");
			setName("dialog.worklistsearch.simple");
			setViewID("simpleSearch");
			setCenterInclude("include/simpleSearch.xhtml");
		}

		public boolean initTab() {
			setWorklistSearch(new WorklistSimpleSearch());
			return true;
		}

		@Override
		public void updateData() {
		}

	}

	@Getter
	@Setter
	public class FavouriteSearchTab extends AbstractTab {

		private WorklistFavouriteSearch worklistSearch;

		private List<FavouriteListContainer> containers;

		private FavouriteListContainer selectedContainer;

		public FavouriteSearchTab() {
			setTabName("FavouriteSearchTab");
			setName("dialog.worklistsearch.favouriteList");
			setViewID("favouriteListSearch");
			setCenterInclude("include/favouriteSearch.xhtml");
		}

		public boolean initTab() {
			setWorklistSearch(new WorklistFavouriteSearch());
			return true;
		}

		@Override
		public void updateData() {

			List<FavouriteList> list = favouriteListDAO.getFavouriteListsForUser(userHandlerAction.getCurrentUser(),
					false, true, true, true, true);

			containers = list.stream().map(p -> new FavouriteListContainer(p, userHandlerAction.getCurrentUser()))
					.collect(Collectors.toList());

			
			if(selectedContainer != null) {
				List<Patient> patient = favouriteListDAO.getPatientFromFavouriteList(selectedContainer.getFavouriteList());
				System.out.println(patient.size());
			}
		}

		public void selectAsWorklist() {
			
			if(selectedContainer != null) {
				List<Patient> patient = favouriteListDAO.getPatientFromFavouriteList(selectedContainer.getFavouriteList());
				System.out.println(patient.size());
			}
			
			if (selectedContainer != null) {
//				worklistSearch.setFavouriteList(selectedContainer.getFavouriteList());
//				worklistViewHandlerAction.addWorklist(worklistSearch, "Default", true);
			}
		}
	}

	public class ExtendedSearchTab extends AbstractTab {

		public ExtendedSearchTab() {
			setTabName("ExtendedSearchTab");
			setName("dialog.worklistsearch.extended");
			setViewID("extendedSearch");
			setCenterInclude("include/extendedSearch.xhtml");
		}

		@Override
		public void updateData() {
		}

	}

	public Worklist extendedSearch() {

		logger.debug("Calling extended search");

		// List<Patient> result =
		// patientDao.getPatientByCriteria(getExtendedSearchData());

		// Worklist worklist = new Worklist("search", result, false,
		// userHandlerAction.getCurrentUser().getDefaultWorklistSortOrder(),
		// userHandlerAction.getCurrentUser().isWorklistAutoUpdate());
		//
		// worklist.setShowActiveTasksExplicit(true);

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
