package org.histo.action;

import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.UserDAO;
import org.histo.model.DiagnosisPrototype;
import org.histo.model.History;
import org.histo.model.Patient;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.StainingPrototypeList;
import org.histo.model.UserAcc;
import org.histo.model.UserRole;
import org.histo.ui.DiagnosisPrototypeListTransformer;
import org.histo.ui.StainingListChooser;
import org.histo.util.Log;
import org.histo.util.SlideUtil;
import org.histo.util.UserAccRoleHolder;
import org.histo.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SettingsHandlerAction {

	public static final int TAB_USER = 0;
	public static final int TAB_STAINING = 1;
	public static final int TAB_STAINING_LIST = 2;
	public static final int TAB_DIAGNOSIS = 3;
	public static final int TAB_PERSON = 5;
	public static final int TAB_MISELLANEOUS = 5;
	public static final int TAB_LOG = 6;

	public static final int STAINING_LIST_LIST = 0;
	public static final int STAINING_LIST_EDIT = 1;
	public static final int STAINING_LIST_ADD_STAINING = 2;

	public static final int DIAGNOSIS_LIST = 0;
	public static final int DIAGNOSIS_EDIT = 1;
	public static final int DIAGNOSIS_TEXT_TEMPLATE = 2;

	public static final int PHYSICIAN_LIST = 0;
	public static final int PHYSICIAN_EDIT = 1;

	@Autowired
	private HelperHandlerAction helper;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private HelperDAO helperDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private Log log;

	/**
	 * List with all users of the program
	 */
	private List<UserAccRoleHolder> users;

	/**
	 * Current History
	 */
	private List<History> currentHistory;

	/**
	 * Tabindex of settings dialog
	 */
	private int activeSettingsIndex = 0;

	/******************************************************** Staining ********************************************************/
	/**
	 * A List with all staings
	 */
	private List<StainingPrototype> allAvailableStainings;

	/**
	 * used in manageStaings dialog to show overview or single staining
	 */
	private boolean showStainingEdit;

	/**
	 * StainingPrototype for creating and editing
	 */
	private StainingPrototype editStaining;

	/**
	 * original StainingPrototype for editing
	 */
	private StainingPrototype originalStaining;
	/******************************************************** Staining ********************************************************/

	/******************************************************** StainingListChooser ********************************************************/
	/**
	 * all staininglists
	 */
	private List<StainingPrototypeList> allAvailableStainingLists;

	/**
	 * used in manageStaingsList dialog to show overview or single stainingList
	 */
	private int stainingListIndex;

	/**
	 * StainingPrototype for creating and editing
	 */
	private StainingPrototypeList editStainingList;

	/**
	 * original StainingPrototypeList for editing
	 */
	private StainingPrototypeList originalStainingList;

	/**
	 * List for selecting staining, this list contains all stainings. They can
	 * be choosen and added to the staininglist
	 */
	private List<StainingListChooser> stainingListChooserForStainingList;
	/******************************************************** StainingListChooser ********************************************************/

	/********************************************************
	 * Standard Diagnosis
	 ********************************************************/
	/**
	 * List with all standard diagnoses
	 */
	private List<DiagnosisPrototype> allAvailableDiagnosisPrototypes;

	/**
	 * Temp variable for creating and editing standardDiagnoses objects
	 */
	private DiagnosisPrototype editDiagnosisPrototype;

	/**
	 * Original DiagnosisPrototype for editing.
	 */
	private DiagnosisPrototype originalDiagnosisPrototype;

	/**
	 * index which diagnosis inputmask should be rendered.
	 */
	private int diagnosisIndex = 0;

	/**
	 * Transformer for selectOnMenu elements.
	 */
	private DiagnosisPrototypeListTransformer diagnosisPrototypeListTransformer;
	/********************************************************
	 * Standard Diagnosis
	 ********************************************************/

	/********************************************************
	 * Person Diagnosis
	 ********************************************************/
	private List<Physician> physicians;

	private Physician editPhysician;

	private int physicianIndex = 0;

	private boolean personSurgeon = true;

	private boolean personExtern = true;

	private boolean personOther = true;

	/********************************************************
	 * Person Diagnosis
	 ********************************************************/

	/**
	 * Show the adminSettigns Dialog and inits the used values
	 */
	public void prepareSettingsDialog() {
		// custom header Element
		HashMap<String, Object> options = new HashMap<String, Object>();
		helper.showDialog(HistoSettings.dialog(HistoSettings.DIALOG_SETTINGS), 1024, 500, false, false, true, options);

		// init users
		setUsers(UserUtil.getUserAndRoles(userDAO.loadAllUsers()));

		// init statings
		setShowStainingEdit(false);
	}

	/**
	 * Hides the adminSettings Dialog
	 */
	public void hideSettingsDialog() {
		helper.hideDialog(HistoSettings.dialog(HistoSettings.DIALOG_SETTINGS));
	}

	/**
	 * Method performed on every tab change of the settings dialog
	 */
	public void onSettingsTabChange() {
		switch (getActiveSettingsIndex()) {
		case TAB_LOG:
			loadGeneralHistory();
			break;
		case TAB_STAINING:
			setAllAvailableStainings(helperDAO.getAllStainings());
			break;
		case TAB_STAINING_LIST:
			setAllAvailableStainingLists(helperDAO.getAllStainingLists());
			helperDAO.initStainingPrototypeList(getAllAvailableStainingLists());
			// update statinglist if selected
			if (getStainingListIndex() == STAINING_LIST_EDIT)
				setStainingListChooserForStainingList(SlideUtil.getStainingListChooser(helperDAO.getAllStainings()));
			break;
		case TAB_PERSON:
			preparePhysicianList();
			break;
		case TAB_DIAGNOSIS:
			updateAllDiagnosisPrototypes();
			break;
		default:
			break;
		}
	}

	/******************************************************** User ********************************************************/
	/**
	 * Changes user role to new Role
	 * 
	 * @param user
	 * @param role
	 */
	public void setRoleForUser(UserAcc user, String role) {
		setRoleForUser(user, UserUtil.createRole(role));
	}

	/**
	 * Changes user role to new Role
	 * 
	 * @param user
	 * @param role
	 */
	public void setRoleForUser(UserAcc user, UserRole role) {
		UserRole oldRole = user.getRole();
		user.setRole(role);
		user.getAuthorities().add(role);
		genericDAO.save(role);
		genericDAO.save(user);
		genericDAO.delete(oldRole);
		log.info("Benutzer Rechte geändert. Benutzer: " + user.getUsername() + ", alte Rolle: " + oldRole.getName()
				+ ", neue Rolle: " + role.getName());
	}

	/******************************************************** User ********************************************************/

	/******************************************************** Staining ********************************************************/
	/**
	 * Prepares a new Staining for editing
	 */
	public void prepareNewStaining() {
		setShowStainingEdit(true);
		setEditStaining(new StainingPrototype());
		setOriginalStaining(null);
	}

	/**
	 * Shows the edit staining form
	 * 
	 * @param stainingPrototype
	 */
	public void prepareEditStaining(StainingPrototype stainingPrototype) {
		setShowStainingEdit(true);
		setEditStaining(new StainingPrototype(stainingPrototype));
		setOriginalStaining(stainingPrototype);
	}

	/**
	 * Saves or creats a new stainingprototype
	 * 
	 * @param newStainingPrototype
	 * @param origStainingPrototype
	 */
	public void saveStainig(StainingPrototype newStainingPrototype, StainingPrototype origStainingPrototype) {
		if (origStainingPrototype == null) {
			// case new, save
			getAllAvailableStainings().add(newStainingPrototype);
			genericDAO.save(getAllAvailableStainings());
			log.info("Neue Färbung erstellt: " + newStainingPrototype.asGson());
		} else {
			// case edit: update an save
			log.info("Färbung veränder, Original: " + origStainingPrototype.asGson() + " Neu:"
					+ newStainingPrototype.asGson());
			origStainingPrototype.update(newStainingPrototype);
			genericDAO.save(origStainingPrototype);
		}
		discardChangesOfStainig();
	}

	/**
	 * discards changes
	 */
	public void discardChangesOfStainig() {
		setShowStainingEdit(false);
		setOriginalStaining(null);
		setEditStaining(null);
	}

	/******************************************************** Staining ********************************************************/

	/******************************************************** StainingListChooser ********************************************************/
	/**
	 * Prepares a new StainingListChooser for editing
	 */
	public void prepareNewStainingList() {
		setStainingListIndex(STAINING_LIST_EDIT);
		setEditStainingList(new StainingPrototypeList());
		setOriginalStainingList(null);
	}

	/**
	 * Shows the edit staininglist form
	 * 
	 * @param stainingPrototype
	 */
	public void prepareEditStainingList(StainingPrototypeList stainingPrototypeList) {
		setStainingListIndex(STAINING_LIST_EDIT);
		setEditStainingList(new StainingPrototypeList(stainingPrototypeList));
		setOriginalStainingList(stainingPrototypeList);
		setStainingListChooserForStainingList(SlideUtil.getStainingListChooser(helperDAO.getAllStainings()));
	}

	/**
	 * Saves a staininglist form or creats a new one
	 * 
	 * @param newStainingPrototypeList
	 * @param origStainingPrototypeList
	 */
	public void saveStainigList(StainingPrototypeList newStainingPrototypeList,
			StainingPrototypeList origStainingPrototypeList) {
		if (origStainingPrototypeList == null) {
			// case new, save
			getAllAvailableStainingLists().add(newStainingPrototypeList);
			genericDAO.save(newStainingPrototypeList);
			genericDAO.save(getAllAvailableStainingLists());
			log.info("Neue Färbeliste erstellt: " + newStainingPrototypeList.asGson());
		} else {
			// case edit: update an save
			log.info("Färbungsliste veränder, Original: " + origStainingPrototypeList.asGson() + " Neu:"
					+ newStainingPrototypeList.asGson());
			origStainingPrototypeList.update(newStainingPrototypeList);
			genericDAO.save(origStainingPrototypeList);
		}
		discardChangesOfStainigList();
	}

	public void prepareDeleteStainingList(StainingPrototypeList stainingPrototypeList) {
		setEditStainingList(stainingPrototypeList);
		setOriginalStainingList(null);
	}

	/**
	 * discards all changes for the stainingList
	 */
	public void discardChangesOfStainigList() {
		setStainingListIndex(STAINING_LIST_LIST);
		setOriginalStainingList(null);
		setEditStainingList(null);
	}

	/**
	 * show a list with all stanings for adding them to a staininglist
	 */
	public void prepareAddStainingToStainingList() {
		setStainingListIndex(STAINING_LIST_ADD_STAINING);
		setStainingListChooserForStainingList(SlideUtil.getStainingListChooser(helperDAO.getAllStainings()));
	}

	/**
	 * Adds all selected staining prototypes to the staininglist
	 * 
	 * @param stainingListChoosers
	 * @param stainingPrototypeList
	 */
	public void addStainingToStainingList(List<StainingListChooser> stainingListChoosers,
			StainingPrototypeList stainingPrototypeList) {
		for (StainingListChooser staining : stainingListChoosers) {
			if (staining.isChoosen()) {
				stainingPrototypeList.getStainingPrototypes().add(staining.getStainingPrototype());
			}
		}

		discardAddStainingToStainingList();
	}

	/**
	 * Removes a staining from a staininglist
	 * 
	 * @param toRemove
	 * @param stainingPrototypeList
	 */
	public void removeStainingFromStainingList(StainingPrototype toRemove,
			StainingPrototypeList stainingPrototypeList) {
		stainingPrototypeList.getStainingPrototypes().remove(toRemove);
	}

	public void discardAddStainingToStainingList() {
		setStainingListIndex(STAINING_LIST_EDIT);
	}

	/******************************************************** StainingListChooser ********************************************************/

	/********************************************************
	 * Standard Diagnosis
	 ********************************************************/
	public void prepareNewDiagnosisPrototype() {
		setEditDiagnosisPrototype(new DiagnosisPrototype());
		setOriginalDiagnosisPrototype(null);
		setDiagnosisIndex(DIAGNOSIS_EDIT);
	}

	public void prepareEditDiagnosisPrototype(DiagnosisPrototype diagnosisPrototype) {
		setEditDiagnosisPrototype(new DiagnosisPrototype(diagnosisPrototype));
		setOriginalDiagnosisPrototype(diagnosisPrototype);
		setDiagnosisIndex(DIAGNOSIS_EDIT);
	}

	public void saveDiagnosisPrototype(DiagnosisPrototype newDiagnosisPrototype,
			DiagnosisPrototype origDiagnosisPrototype) {
		if (origDiagnosisPrototype == null) {
			// case new, save
			getAllAvailableDiagnosisPrototypes().add(newDiagnosisPrototype);
			genericDAO.save(newDiagnosisPrototype);
			genericDAO.save(getAllAvailableDiagnosisPrototypes());
			log.info("Neue Diagnose erstellt: " + newDiagnosisPrototype.asGson());
		} else {
			// case edit: update an save
			log.info("Diagnose veränder, Original: " + origDiagnosisPrototype.asGson() + " Neu:"
					+ newDiagnosisPrototype.asGson());
			origDiagnosisPrototype.update(newDiagnosisPrototype);
			genericDAO.save(origDiagnosisPrototype);
		}
		discardDiagnosisPrototype();
	}

	/**
	 * Discards all changes of a diagnosisPrototype
	 */
	public void discardDiagnosisPrototype() {
		setDiagnosisIndex(DIAGNOSIS_LIST);
		setOriginalStainingList(null);
		setEditStainingList(null);
	}

	public void prepareEditDiagnosisPrototypeTemplate() {
		setDiagnosisIndex(DIAGNOSIS_TEXT_TEMPLATE);
	}

	public void discardEditDiagnosisPrototypeTemplate() {
		setDiagnosisIndex(DIAGNOSIS_EDIT);
	}

	/********************************************************
	 * Standard Diagnosis
	 ********************************************************/

	/******************************************************** Physician ********************************************************/
	public void preparePhysicianList() {
		setPhysicians(physicianDAO.getPhysicians(isPersonSurgeon(), isPersonExtern(), isPersonOther()));

	}

	public void prepareNewPhysician() {
		setEditPhysician(new Physician());
		setPhysicianIndex(PHYSICIAN_EDIT);
	}

	public void prepareEditPhysician(Physician physician) {
		setEditPhysician(physician);
		setPhysicianIndex(PHYSICIAN_EDIT);
	}

	public void savePhysician(Physician physician) {
		if (!physician.isRoleSurgeon() && !physician.isRoleClinicDoctor() && !physician.isRoleResidentDoctor())
			physician.setRoleMiscellaneous(true);

		genericDAO.save(physician);
		discardEditPhysician();
	}

	public void discardEditPhysician() {
		if (getEditPhysician().getId() != 0)
			genericDAO.refresh(getEditPhysician());
		setEditPhysician(null);
		preparePhysicianList();
		setPhysicianIndex(PHYSICIAN_LIST);
	}

	/******************************************************** Physician ********************************************************/

	/******************************************************** History ********************************************************/
	/**
	 * Loads the current history, for all events 100 entries. Shows the current
	 * history dialog.
	 */
	public void loadGeneralHistory() {
		setCurrentHistory(helperDAO.getCurrentHistory(100));
	}

	/**
	 * Loads the current history for the given patient. Shows the current
	 * history dialog.
	 * 
	 * @param patient
	 */
	public void loadPatientHistory(Patient patient) {
		setCurrentHistory(helperDAO.getCurrentHistoryForPatient(100, patient));
	}

	/******************************************************** History ********************************************************/

	/******************************************************** miscellaneous ********************************************************/

	/**
	 * Sets a new list with all DiagnosisProtoypes and creates a corresponding
	 * transformer
	 */
	public void updateAllDiagnosisPrototypes() {
		System.out.println("updating diagnosis");
		setAllAvailableDiagnosisPrototypes(helperDAO.getAllDiagnosisPrototypes());
		setDiagnosisPrototypeListTransformer(
				new DiagnosisPrototypeListTransformer(getAllAvailableDiagnosisPrototypes()));
	}

	/******************************************************** miscellaneous ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public List<UserAccRoleHolder> getUsers() {
		return users;
	}

	public void setUsers(List<UserAccRoleHolder> users) {
		this.users = users;
	}

	public int getActiveSettingsIndex() {
		return activeSettingsIndex;
	}

	public void setActiveSettingsIndex(int activeSettingsIndex) {
		this.activeSettingsIndex = activeSettingsIndex;
	}

	public List<History> getCurrentHistory() {
		return currentHistory;
	}

	public void setCurrentHistory(List<History> currentHistory) {
		this.currentHistory = currentHistory;
	}

	public List<StainingPrototype> getAllAvailableStainings() {
		return allAvailableStainings;
	}

	public void setAllAvailableStainings(List<StainingPrototype> allAvailableStainings) {
		this.allAvailableStainings = allAvailableStainings;
	}

	public boolean isShowStainingEdit() {
		return showStainingEdit;
	}

	public void setShowStainingEdit(boolean showStainingEdit) {
		this.showStainingEdit = showStainingEdit;
	}

	public StainingPrototype getEditStaining() {
		return editStaining;
	}

	public void setEditStaining(StainingPrototype editStaining) {
		this.editStaining = editStaining;
	}

	public StainingPrototype getOriginalStaining() {
		return originalStaining;
	}

	public void setOriginalStaining(StainingPrototype originalStaining) {
		this.originalStaining = originalStaining;
	}

	public List<StainingPrototypeList> getAllAvailableStainingLists() {
		return allAvailableStainingLists;
	}

	public void setAllAvailableStainingLists(List<StainingPrototypeList> allAvailableStainingLists) {
		this.allAvailableStainingLists = allAvailableStainingLists;
	}

	public int getStainingListIndex() {
		return stainingListIndex;
	}

	public void setStainingListIndex(int stainingListIndex) {
		this.stainingListIndex = stainingListIndex;
	}

	public StainingPrototypeList getEditStainingList() {
		return editStainingList;
	}

	public void setEditStainingList(StainingPrototypeList editStainingList) {
		this.editStainingList = editStainingList;
	}

	public StainingPrototypeList getOriginalStainingList() {
		return originalStainingList;
	}

	public void setOriginalStainingList(StainingPrototypeList originalStainingList) {
		this.originalStainingList = originalStainingList;
	}

	public List<StainingListChooser> getStainingListChooserForStainingList() {
		return stainingListChooserForStainingList;
	}

	public void setStainingListChooserForStainingList(List<StainingListChooser> stainingListChooserForStainingList) {
		this.stainingListChooserForStainingList = stainingListChooserForStainingList;
	}

	public List<DiagnosisPrototype> getAllAvailableDiagnosisPrototypes() {
		return allAvailableDiagnosisPrototypes;
	}

	public void setAllAvailableDiagnosisPrototypes(List<DiagnosisPrototype> allAvailableDiagnosisPrototypes) {
		this.allAvailableDiagnosisPrototypes = allAvailableDiagnosisPrototypes;
	}

	public DiagnosisPrototype getEditDiagnosisPrototype() {
		return editDiagnosisPrototype;
	}

	public void setEditDiagnosisPrototype(DiagnosisPrototype editDiagnosisPrototype) {
		this.editDiagnosisPrototype = editDiagnosisPrototype;
	}

	public int getDiagnosisIndex() {
		return diagnosisIndex;
	}

	public void setDiagnosisIndex(int diagnosisIndex) {
		this.diagnosisIndex = diagnosisIndex;
	}

	public DiagnosisPrototype getOriginalDiagnosisPrototype() {
		return originalDiagnosisPrototype;
	}

	public void setOriginalDiagnosisPrototype(DiagnosisPrototype originalDiagnosisPrototype) {
		this.originalDiagnosisPrototype = originalDiagnosisPrototype;
	}

	public DiagnosisPrototypeListTransformer getDiagnosisPrototypeListTransformer() {
		return diagnosisPrototypeListTransformer;
	}

	public void setDiagnosisPrototypeListTransformer(
			DiagnosisPrototypeListTransformer diagnosisPrototypeListTransformer) {
		this.diagnosisPrototypeListTransformer = diagnosisPrototypeListTransformer;
	}

	public List<Physician> getPhysicians() {
		return physicians;
	}

	public void setPhysicians(List<Physician> physicians) {
		this.physicians = physicians;
	}

	public int getPhysicianIndex() {
		return physicianIndex;
	}

	public void setPhysicianIndex(int physicianIndex) {
		this.physicianIndex = physicianIndex;
	}

	public boolean isPersonSurgeon() {
		return personSurgeon;
	}

	public void setPersonSurgeon(boolean personSurgeon) {
		this.personSurgeon = personSurgeon;
	}

	public boolean isPersonExtern() {
		return personExtern;
	}

	public void setPersonExtern(boolean personExtern) {
		this.personExtern = personExtern;
	}

	public boolean isPersonOther() {
		return personOther;
	}

	public void setPersonOther(boolean personOther) {
		this.personOther = personOther;
	}

	public Physician getEditPhysician() {
		return editPhysician;
	}

	public void setEditPhysician(Physician editPhysician) {
		this.editPhysician = editPhysician;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
