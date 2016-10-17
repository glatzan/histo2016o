package org.histo.action;

import java.util.ArrayList;
import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.SettingsTab;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.UserDAO;
import org.histo.model.DiagnosisPrototype;
import org.histo.model.HistoUser;
import org.histo.model.History;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.patient.Patient;
import org.histo.model.transitory.PhysicianRoleOptions;
import org.histo.ui.StainingListChooser;
import org.histo.ui.transformer.DiagnosisPrototypeListTransformer;
import org.histo.util.ResourceBundle;
import org.histo.util.SlideUtil;
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
	public static final int TAB_PERSON = 4;
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
	public static final int PHYSICIAN_ADD_EXTER = 2;
	public static final int PHYSICIAN_ADD_LDPA = 3;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private HelperDAO helperDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	/**
	 * List with all users of the program
	 */
	private List<HistoUser> users;

	/**
	 * Current History
	 */
	private List<History> currentHistory;

	/**
	 * Tabindex of settings dialog
	 */
	private int activeSettingsIndex = 0;

	/**
	 * Tabindex of the settings tab
	 */
	private SettingsTab physicianTabIndex = SettingsTab.PHYSICIAN_LIST;

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
	private List<MaterialPreset> allAvailableMaterials;

	/**
	 * used in manageStaingsList dialog to show overview or single stainingList
	 */
	private int stainingListIndex;

	/**
	 * StainingPrototype for creating and editing
	 */
	private MaterialPreset editStainingList;

	/**
	 * original StainingPrototypeList for editing
	 */
	private MaterialPreset originalStainingList;

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
	 * Physician
	 ********************************************************/

	/**
	 * containing options for the physician list
	 */
	private PhysicianRoleOptions physicianRoleOptions;

	/**
	 * List containing all physicians known in the histo database
	 */
	private List<Physician> physicianList;

	/**
	 * Used for creating new or for editing existing physicians
	 */
	private Physician tmpPhysician;

	/**
	 * List containing all physicians available from ldap
	 */
	private List<Physician> ldapPhysicianList;

	/**
	 * Used for selecting a physician from the ldap list
	 */
	private Physician tmpLdapPhysician;

	/**
	 * String is used for searching for internal physicians
	 */
	private String ldapPhysicianSearchString;

	/********************************************************
	 * Physician
	 ********************************************************/

	/********************************************************
	 * General
	 ********************************************************/

	/**
	 * Show the adminSettigns Dialog and inits the used values
	 */

	public void prepareSettingsDialog() {
		// init users
		setUsers(userDAO.loadAllUsers());

		// init statings
		setShowStainingEdit(false);

		preparePhysicianList();

		mainHandlerAction.showDialog(Dialog.SETTINGS);
	}

	/**
	 * Hides the adminSettings Dialog
	 */
	public void hideSettingsDialog() {
		mainHandlerAction.hideDialog(Dialog.SETTINGS);
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
			setAllAvailableMaterials(helperDAO.getAllStainingLists());
			helperDAO.initStainingPrototypeList(getAllAvailableMaterials());
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

	/********************************************************
	 * General
	 ********************************************************/

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
			// log.info("Neue F�rbung erstellt: " +
			// newStainingPrototype.asGson());
		} else {
			// case edit: update an save
			// log.info("F�rbung ver�nder, Original: " +
			// origStainingPrototype.asGson() + " Neu:"
			// + newStainingPrototype.asGson());
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
		setEditStainingList(new MaterialPreset());
		setOriginalStainingList(null);
	}

	/**
	 * Shows the edit staininglist form
	 * 
	 * @param stainingPrototype
	 */
	public void prepareEditStainingList(MaterialPreset stainingPrototypeList) {
		setStainingListIndex(STAINING_LIST_EDIT);
		setEditStainingList(new MaterialPreset(stainingPrototypeList));
		setOriginalStainingList(stainingPrototypeList);
		setStainingListChooserForStainingList(SlideUtil.getStainingListChooser(helperDAO.getAllStainings()));
	}

	/**
	 * Saves a staininglist form or creats a new one
	 * 
	 * @param newStainingPrototypeList
	 * @param origStainingPrototypeList
	 */
	public void saveStainigList(MaterialPreset newStainingPrototypeList, MaterialPreset origStainingPrototypeList) {
		if (origStainingPrototypeList == null) {
			// case new, save
			getAllAvailableMaterials().add(newStainingPrototypeList);
			genericDAO.save(newStainingPrototypeList);
			genericDAO.save(getAllAvailableMaterials());
			// log.info("Neue F�rbeliste erstellt: " +
			// newStainingPrototypeList.asGson());
		} else {
			// case edit: update an save
			// log.info("F�rbungsliste ver�nder, Original: " +
			// origStainingPrototypeList.asGson() + " Neu:"
			// + newStainingPrototypeList.asGson());
			origStainingPrototypeList.update(newStainingPrototypeList);
			genericDAO.save(origStainingPrototypeList);
		}
		discardChangesOfStainigList();
	}

	public void prepareDeleteStainingList(MaterialPreset stainingPrototypeList) {
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
			MaterialPreset stainingPrototypeList) {
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
	public void removeStainingFromStainingList(StainingPrototype toRemove, MaterialPreset stainingPrototypeList) {
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
			// log.info("Neue Diagnose erstellt: " +
			// newDiagnosisPrototype.asGson());
		} else {
			// case edit: update an save
			// log.info("Diagnose ver�nder, Original: " +
			// origDiagnosisPrototype.asGson() + " Neu:"
			// + newDiagnosisPrototype.asGson());
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

	/********************************************************
	 * Physician
	 ********************************************************/
	/**
	 * Shows all added Physicians (ROLE: Surgeon, PrivatePhysician, Other)
	 */
	public void preparePhysicianList() {
		if (getPhysicianRoleOptions() == null)
			setPhysicianRoleOptions(new PhysicianRoleOptions());

		List<ContactRole> contactRoles = new ArrayList<ContactRole>();

		if (getPhysicianRoleOptions().isSurgeon())
			contactRoles.add(ContactRole.SURGEON);
		if (getPhysicianRoleOptions().isPrivatePhysician())
			contactRoles.add(ContactRole.PRIVATE_PHYSICIAN);
		if (getPhysicianRoleOptions().isOther())
			contactRoles.add(ContactRole.OTHER);

		setPhysicianList(physicianDAO.getPhysicians(contactRoles, getPhysicianRoleOptions().isArchived()));
	}

	/**
	 * Shows the add external or ldap screen per default the ldap select screnn
	 * is used.
	 */
	public void prepareNewPhysician() {
		setTmpPhysician(new Physician());
		setPhysicianTabIndex(SettingsTab.PHYSICIAN_ADD_LDPA);
	}

	/**
	 * Shows the gui for editing an existing physician
	 * 
	 * @param physician
	 */
	public void prepareEditPhysician(Physician physician) {
		setTmpPhysician(physician);
		setPhysicianTabIndex(SettingsTab.PHYSICIAN_EDIT);
	}

	/**
	 * Generates an ldap search filter (?(xxx)....) and offers the result list.
	 * The result list is a physician list with minimal details. Before adding
	 * an clinic physician a ldap fetch for more details has to be done
	 * 
	 * @param name
	 */
	public void searchForPhysician(String name) {
		// removing multiple spaces an commas and replacing them with one space,
		// splitting the whole thing into an array
		String[] arr = name.replaceAll("[ ,]+", " ").split(" ");
		StringBuffer request = new StringBuffer("(&");
		for (int i = 0; i < arr.length; i++) {
			request.append("(cn=*" + arr[i] + "*)");
		}
		request.append(")");

		List<Physician> listP = UserUtil.getPhysiciansFromLDAP(request.toString());

		setLdapPhysicianList(listP);
	}

	/**
	 * Saves an edited physician to the database
	 * 
	 * @param physician
	 */
	public void saveEditPhysician(Physician physician) {
		if (physician.getDefaultContactRole() == ContactRole.NONE)
			physician.setDefaultContactRole(ContactRole.OTHER);

		genericDAO.save(physician,
				resourceBundle.get("log.settings.physician.physician.edit", physician.getFullName()));
		discardTmpPhysician();
	}

	/**
	 * Saves a physician to the database, if no role was selected
	 * ContactRole.Other will be set per default.
	 * 
	 * @param physician
	 */
	public void saveNewPrivatePhysician(Physician physician) {
		// always set role to miscellaneous if no other role was selected
		if (physician.getDefaultContactRole() == ContactRole.NONE)
			physician.setDefaultContactRole(ContactRole.OTHER);

		genericDAO.save(physician,
				resourceBundle.get("log.settings.physician.privatePhysician.save", physician.getFullName()));
		discardTmpPhysician();
	}

	/**
	 * 
	 * @param ldapPhysician
	 * @param editPhysician
	 */
	public void savePhysicianFromLdap(Physician ldapPhysician, ContactRole role) {
		if (ldapPhysician == null)
			return;

		// removing id from the list
		ldapPhysician.setId(0);

		if (role == ContactRole.NONE)
			ldapPhysician.setDefaultContactRole(ContactRole.OTHER);
		else
			ldapPhysician.setDefaultContactRole(role);

		// tje internal physician from ldap it might have been added before (if
		// the the physician is a user of this programm),
		// search fur unique uid
		Physician physicianFromDatabase = physicianDAO.loadPhysicianByUID(ldapPhysician.getUid());

		// undating the foud physician
		if (physicianFromDatabase != null) {
			UserUtil.updatePhysicianData(physicianFromDatabase, ldapPhysician);
			ldapPhysician = physicianFromDatabase;

			genericDAO.save(ldapPhysician,
					resourceBundle.get("log.settings.physician.ldap.update", ldapPhysician.getFullName()));
			return;
		}

		genericDAO.save(ldapPhysician,
				resourceBundle.get("log.settings.physician.ldap.save", ldapPhysician.getFullName()));

		discardTmpPhysician();
	}

	/**
	 * Archvies or dearchvies physicians depending on the given parameters.
	 * 
	 * @param physician
	 * @param archive
	 */
	public void archivePhysician(Physician physician, boolean archive) {
		physician.setArchived(archive);
		genericDAO.save(physician,
				resourceBundle.get(archive ? "log.settings.physician.archived" : "log.settings.physician.archived.undo",
						physician.getFullName()));
		preparePhysicianList();
	}

	/**
	 * Clears the temporary variables and the the physician list to display
	 */
	public void discardTmpPhysician() {
		// if a physician was edited remove all chagnes
		if (getPhysicianTabIndex() == SettingsTab.PHYSICIAN_EDIT && getTmpPhysician().getId() != 0)
			genericDAO.refresh(getTmpPhysician());

		setTmpPhysician(null);
		setTmpLdapPhysician(null);

		// update physician list
		preparePhysicianList();
		setPhysicianTabIndex(SettingsTab.PHYSICIAN_LIST);
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
	public List<HistoUser> getUsers() {
		return users;
	}

	public void setUsers(List<HistoUser> users) {
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

	public List<MaterialPreset> getAllAvailableMaterials() {
		return allAvailableMaterials;
	}

	public void setAllAvailableMaterials(List<MaterialPreset> allAvailableMaterials) {
		this.allAvailableMaterials = allAvailableMaterials;
	}

	public int getStainingListIndex() {
		return stainingListIndex;
	}

	public void setStainingListIndex(int stainingListIndex) {
		this.stainingListIndex = stainingListIndex;
	}

	public MaterialPreset getEditStainingList() {
		return editStainingList;
	}

	public void setEditStainingList(MaterialPreset editStainingList) {
		this.editStainingList = editStainingList;
	}

	public MaterialPreset getOriginalStainingList() {
		return originalStainingList;
	}

	public void setOriginalStainingList(MaterialPreset originalStainingList) {
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

	public List<Physician> getPhysicianList() {
		return physicianList;
	}

	public void setPhysicianList(List<Physician> physicianList) {
		this.physicianList = physicianList;
	}

	public Physician getTmpPhysician() {
		return tmpPhysician;
	}

	public void setTmpPhysician(Physician tmpPhysician) {
		this.tmpPhysician = tmpPhysician;
	}

	public List<Physician> getLdapPhysicianList() {
		return ldapPhysicianList;
	}

	public void setLdapPhysicianList(List<Physician> ldapPhysicianList) {
		this.ldapPhysicianList = ldapPhysicianList;
	}

	public String getLdapPhysicianSearchString() {
		return ldapPhysicianSearchString;
	}

	public void setLdapPhysicianSearchString(String ldapPhysicianSearchString) {
		this.ldapPhysicianSearchString = ldapPhysicianSearchString;
	}

	public Physician getTmpLdapPhysician() {
		return tmpLdapPhysician;
	}

	public void setTmpLdapPhysician(Physician tmpLdapPhysician) {
		this.tmpLdapPhysician = tmpLdapPhysician;
	}

	public PhysicianRoleOptions getPhysicianRoleOptions() {
		return physicianRoleOptions;
	}

	public void setPhysicianRoleOptions(PhysicianRoleOptions physicianRoleOptions) {
		this.physicianRoleOptions = physicianRoleOptions;
	}

	public SettingsTab getPhysicianTabIndex() {
		return physicianTabIndex;
	}

	public void setPhysicianTabIndex(SettingsTab physicianTabIndex) {
		this.physicianTabIndex = physicianTabIndex;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
