package org.histo.action;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.MailType;
import org.histo.config.enums.SettingsTab;
import org.histo.config.enums.StaticList;
import org.histo.dao.GenericDAO;
import org.histo.dao.UtilDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.SettingsDAO;
import org.histo.dao.UserDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.HistoUser;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.patient.Patient;
import org.histo.model.transitory.json.LdapHandler;
import org.histo.ui.ListChooser;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.ui.transformer.DiagnosisPrototypeListTransformer;
import org.histo.util.SlideUtil;
import org.primefaces.event.ReorderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SettingsHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final int TAB_USER = 0;
	public static final int TAB_STAINING = 1;
	public static final int TAB_MATERIAL = 2;
	public static final int TAB_DIAGNOSIS = 3;
	public static final int TAB_PERSON = 4;
	public static final int TAB_STATIC_LISTS = 5;
	public static final int TAB_MISELLANEOUS = 6;
	public static final int TAB_LOG = 7;

	public static final int DIAGNOSIS_LIST = 0;
	public static final int DIAGNOSIS_EDIT = 1;
	public static final int DIAGNOSIS_TEXT_TEMPLATE = 2;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private CommenDataHandlerAction commenDataHandlerAction;

	/**
	 * Tabindex of settings dialog
	 */
	private int activeSettingsIndex = 0;

	/**
	 * Tabindex of the settings tab
	 */
	private SettingsTab userListTabIndex = SettingsTab.U_LIST;

	/**
	 * Tabindex of the settings tab
	 */
	private SettingsTab physicianTabIndex = SettingsTab.P_LIST;

	/**
	 * Tabindex of the material tab
	 */
	private SettingsTab materialTabIndex = SettingsTab.M_LIST;

	/**
	 * Tabindex of the static list tab
	 */
	private SettingsTab staticListTabIndex = SettingsTab.S_LIST;

	/********************************************************
	 * User
	 ********************************************************/

	/**
	 * List with all users of the program
	 */
	private List<HistoUser> users;

	/**
	 * Selected user for changing role
	 */
	private HistoUser selectedUser;

	/**
	 * for editing physicians from the user list
	 */
	private Physician selectedUserPhysician;
	/********************************************************
	 * User
	 ********************************************************/

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

	/********************************************************
	 * Material
	 ********************************************************/

	/**
	 * all materials
	 */
	private List<MaterialPreset> allAvailableMaterials;

	/**
	 * StainingPrototype for creating and editing
	 */
	private MaterialPreset editMaterial;

	/**
	 * original StainingPrototypeList for editing
	 */
	private MaterialPreset originalMaterial;

	/**
	 * List for selecting staining, this list contains all stainings. They can
	 * be choosen and added to the material
	 */
	private List<ListChooser<StainingPrototype>> stainingListChooserForMaterial;
	/********************************************************
	 * Material
	 ********************************************************/

	/********************************************************
	 * Standard Diagnosis
	 ********************************************************/
	/**
	 * List with all standard diagnoses
	 */
	private List<DiagnosisPreset> allAvailableDiagnosisPrototypes;

	/**
	 * Temp variable for creating and editing standardDiagnoses objects
	 */
	private DiagnosisPreset editDiagnosisPrototype;

	/**
	 * Original DiagnosisPreset for editing.
	 */
	private DiagnosisPreset originalDiagnosisPrototype;

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
	 * True if archived physicians should be display
	 */
	private boolean showArchivedPhysicians = false;

	/**
	 * Array of roles for that physicians should be shown.
	 */
	private ContactRole[] showPhysicianRoles;

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
	 * static lists
	 ********************************************************/
	/**
	 * Current static list to edit
	 */
	private StaticList selectedStaticList = StaticList.WARDS;

	/**
	 * Content of the current static list
	 */
	private List<ListItem> staticListContent;

	/**
	 * Is used for creating and editing static lists items
	 */
	private ListItem tmpListItem;

	/**
	 * If true archived object will be shown.
	 */
	private boolean showArchivedListItems;

	/********************************************************
	 * static lists
	 ********************************************************/

	/********************************************************
	 * General
	 ********************************************************/

	/**
	 * Show the adminSettigns Dialog and inits the used values
	 */

	public void prepareSettingsDialog() {
		prepareSettingsDialog(getActiveSettingsIndex());
	}

	public void prepareSettingsDialog(int activeTab) {
		setActiveSettingsIndex(activeTab);

		// init statings
		setShowStainingEdit(false);

		onSettingsTabChange();

		commenDataHandlerAction.setAssociatedRoles(Arrays.asList(ContactRole.values()));
		commenDataHandlerAction.setAssociatedRolesTransformer(
				new AssociatedRoleTransformer(commenDataHandlerAction.getAssociatedRoles()));

		mainHandlerAction.showDialog(Dialog.SETTINGS);
	}

	public void prepareSettingsDialog(SettingsTab settingsTab) {
		SettingsTab parentTab = settingsTab.getParent() != null ? settingsTab.getParent() : settingsTab;

		switch (parentTab) {
		case PHYSICIAN:
			setPhysicianTabIndex(settingsTab);
			break;
		default:
			break;
		}

		prepareSettingsDialog(settingsTab.getTabNumber());
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
		case TAB_USER:
			prepareUserList();
			break;
		case TAB_LOG:
			loadGeneralHistory();
			break;
		case TAB_STAINING:
			setAllAvailableStainings(settingsDAO.getAllStainingPrototypes());
			break;
		case TAB_MATERIAL:
			logger.debug("Selecting tag material");
			initMaterialPresets();
			// update stainings if selected
			if (getMaterialTabIndex() == SettingsTab.M_EDIT) {
				setStainingListChooserForMaterial(
						SlideUtil.getStainingListChooser(settingsDAO.getAllStainingPrototypes()));

				// bugfix, if material is null an diet tab is shown
				if (getEditMaterial() == null) {
					setEditMaterial(new MaterialPreset());
					setOriginalMaterial(null);
				}
			}
			break;
		case TAB_PERSON:
			preparePhysicianList();
			break;
		case TAB_DIAGNOSIS:
			updateAllDiagnosisPrototypes();
			break;
		case TAB_STATIC_LISTS:
			prepareStaticLists();
			break;
		default:
			break;
		}
	}

	/********************************************************
	 * General
	 ********************************************************/

	/********************************************************
	 * User
	 ********************************************************/
	public void prepareUserList() {
		setUsers(userDAO.loadAllUsers());
	}

	public void onChangeUserRole(HistoUser histoUser) {
		userHandlerAction.roleOfuserHasChanged(histoUser);
		mainHandlerAction.showDialog(Dialog.SETTINGS_USER_ROLE_CHANGE);
		setSelectedUser(histoUser);
	}

	public void informUserAboutChangedRole(HistoUser histoUser) {
		// sending mail to inform about unlocking request
		mainHandlerAction.getSettings().getMail().sendTempalteMail(mainHandlerAction.getSettings().getAdminMails(),
				MailType.Unlock, null, null);
	}

	/**
	 * prepares an edit dialog for editing user data, only avaliable for admins
	 * 
	 * @param physician
	 */
	public void prepareEditPhysicianFromUserList(Physician physician) {
		setSelectedUserPhysician(physician);
		setUserListTabIndex(SettingsTab.U_EDIT);
	}

	/**
	 * Saves an edited physician to the database
	 * 
	 * @param physician
	 */
	public void saveEditPhysicianFromUserList(Physician physician) {
		if (physician.hasNoAssociateRole())
			physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);

		genericDAO.save(physician,
				resourceBundle.get("log.settings.physician.physician.edit", physician.getPerson().getFullName()));
		discardTmpPhysicianFromUserList();
	}

	/**
	 * Shows the userlist aganin
	 */
	public void discardTmpPhysicianFromUserList() {
		genericDAO.refresh(getSelectedUserPhysician());

		setUserListTabIndex(SettingsTab.U_LIST);
		prepareUserList();
		setSelectedUserPhysician(null);
	}

	/********************************************************
	 * User
	 ********************************************************/

	/********************************************************
	 * Staining
	 ********************************************************/
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
			logger.debug("Creating new staining " + newStainingPrototype.getName());
			// case new, save
			getAllAvailableStainings().add(newStainingPrototype);
			genericDAO.save(newStainingPrototype,
					resourceBundle.get("log.settings.staining.new", newStainingPrototype.getName()));
			ListOrder.reOrderList(getAllAvailableStainings());
			genericDAO.save(getAllAvailableStainings(), resourceBundle.get("log.settings.staining.list.reoder"));
		} else {
			// case edit: update an save
			origStainingPrototype.update(newStainingPrototype);
			genericDAO.save(origStainingPrototype,
					resourceBundle.get("log.settings.material.update", origStainingPrototype.getName()));
		}
		discardChangesOfStainig();
	}

	/**
	 * Is fired if the list is reordered by the user via drag and drop
	 * 
	 * @param event
	 */
	public void onReorderStainingList(ReorderEvent event) {
		logger.debug("List order changed, moved staining from " + event.getFromIndex() + " to " + event.getToIndex());
		ListOrder.reOrderList(getAllAvailableStainings());
		genericDAO.save(getAllAvailableStainings(), resourceBundle.get("log.settings.staining.list.reoder"));
	}

	/**
	 * discards changes
	 */
	public void discardChangesOfStainig() {
		setShowStainingEdit(false);
		setOriginalStaining(null);
		setEditStaining(null);
	}

	/********************************************************
	 * Staining
	 ********************************************************/

	/********************************************************
	 * Material
	 ********************************************************/
	public void initMaterialPresets() {
		setAllAvailableMaterials(settingsDAO.getAllMaterialPresets());
		utilDAO.initStainingPrototypeList(getAllAvailableMaterials());
	}

	/**
	 * Prepares a new StainingListChooser for editing
	 */
	public void prepareNewMaterial() {
		setMaterialTabIndex(SettingsTab.M_EDIT);
		setEditMaterial(new MaterialPreset());
		setOriginalMaterial(null);
	}

	/**
	 * Shows the edit material form
	 * 
	 * @param stainingPrototype
	 */
	public void prepareEditMaterial(MaterialPreset material) {
		setMaterialTabIndex(SettingsTab.M_EDIT);
		setEditMaterial(new MaterialPreset(material));
		setOriginalMaterial(material);
		setStainingListChooserForMaterial(SlideUtil.getStainingListChooser(settingsDAO.getAllStainingPrototypes()));
	}

	/**
	 * Saves a material or creates a new one
	 * 
	 * @param newStainingPrototypeList
	 * @param origStainingPrototypeList
	 */
	public void saveMaterial(MaterialPreset newMaterial, MaterialPreset originalMaterial) {
		if (originalMaterial == null) {
			logger.debug("Creating new Material " + newMaterial.getName());
			// case new, save
			getAllAvailableMaterials().add(newMaterial);
			genericDAO.save(newMaterial, resourceBundle.get("log.settings.material.new", newMaterial.getName()));
			ListOrder.reOrderList(getAllAvailableMaterials());
			genericDAO.save(getAllAvailableMaterials(), resourceBundle.get("log.settings.material.list.reoder"));
		} else {
			logger.debug("Updating Material " + originalMaterial.getName());
			// case edit: update an save
			originalMaterial.update(newMaterial);
			genericDAO.save(originalMaterial,
					resourceBundle.get("log.settings.material.update", originalMaterial.getName()));
		}
		discardChangesOfMaterial();
	}

	public void prepareDeleteStainingList(MaterialPreset stainingPrototypeList) {
		setEditMaterial(stainingPrototypeList);
		setOriginalMaterial(null);
	}

	/**
	 * discards all changes for the stainingList
	 */
	public void discardChangesOfMaterial() {
		setMaterialTabIndex(SettingsTab.M_LIST);
		setOriginalMaterial(null);
		setEditMaterial(null);
	}

	/**
	 * show a list with all stanings for adding them to a material
	 */
	public void prepareAddStainingToMaterial() {
		setMaterialTabIndex(SettingsTab.M_ADD_STAINING);
		setStainingListChooserForMaterial(SlideUtil.getStainingListChooser(settingsDAO.getAllStainingPrototypes()));
	}

	/**
	 * Adds all selected staining prototypes to the material
	 * 
	 * @param stainingListChoosers
	 * @param stainingPrototypeList
	 */
	public void addStainingToMaterial(List<ListChooser<StainingPrototype>> stainingListChoosers,
			MaterialPreset stainingPrototypeList) {
		for (ListChooser<StainingPrototype> staining : stainingListChoosers) {
			if (staining.isChoosen()) {
				stainingPrototypeList.getStainingPrototypes().add(staining.getListItem());
			}
		}

		discardAddStainingToMaterial();
	}

	/**
	 * Removes a staining from a material
	 * 
	 * @param toRemove
	 * @param stainingPrototypeList
	 */
	public void removeStainingFromStainingList(StainingPrototype toRemove, MaterialPreset stainingPrototypeList) {
		stainingPrototypeList.getStainingPrototypes().remove(toRemove);
	}

	public void discardAddStainingToMaterial() {
		setMaterialTabIndex(SettingsTab.M_EDIT);
	}

	/**
	 * Is fired if the list is reordered by the user via drag and drop
	 * 
	 * @param event
	 */
	public void onReorderMaterialList(ReorderEvent event) {
		logger.debug("List order changed, moved material from " + event.getFromIndex() + " to " + event.getToIndex());
		ListOrder.reOrderList(getAllAvailableMaterials());
		genericDAO.save(getAllAvailableMaterials(), resourceBundle.get("log.settings.staining.list.reoder"));
	}

	/********************************************************
	 * Material
	 ********************************************************/

	/********************************************************
	 * Standard Diagnosis
	 ********************************************************/
	public void prepareNewDiagnosisPrototype() {
		setEditDiagnosisPrototype(new DiagnosisPreset());
		getEditDiagnosisPrototype().setExtendedDiagnosisText("");
		setOriginalDiagnosisPrototype(null);
		setDiagnosisIndex(DIAGNOSIS_EDIT);
	}

	public void prepareEditDiagnosisPrototype(DiagnosisPreset diagnosisPreset) {
		setEditDiagnosisPrototype(new DiagnosisPreset(diagnosisPreset));
		setOriginalDiagnosisPrototype(diagnosisPreset);
		setDiagnosisIndex(DIAGNOSIS_EDIT);
	}

	public void saveDiagnosisPrototype(DiagnosisPreset newDiagnosisPrototype, DiagnosisPreset origDiagnosisPrototype) {
		if (origDiagnosisPrototype == null) {

			// case new, save
			logger.debug("Creating new diagnosis " + newDiagnosisPrototype.getCategory());
			getAllAvailableDiagnosisPrototypes().add(newDiagnosisPrototype);
			genericDAO.save(newDiagnosisPrototype,
					resourceBundle.get("log.settings.diagnosis.new", newDiagnosisPrototype.getCategory()));
			ListOrder.reOrderList(getAllAvailableDiagnosisPrototypes());
			genericDAO.save(getAllAvailableDiagnosisPrototypes(),
					resourceBundle.get("log.settings.diagnosis.list.reoder"));
		} else {
			// case edit: update an save
			logger.debug("Updating  diagnosis " + origDiagnosisPrototype.getCategory());
			origDiagnosisPrototype.update(newDiagnosisPrototype);
			genericDAO.save(origDiagnosisPrototype,
					resourceBundle.get("log.settings.diagnosis.update", origDiagnosisPrototype.getCategory()));
		}
		discardDiagnosisPrototype();
	}

	/**
	 * Discards all changes of a diagnosisPrototype
	 */
	public void discardDiagnosisPrototype() {
		setDiagnosisIndex(DIAGNOSIS_LIST);
		setOriginalMaterial(null);
		setEditMaterial(null);
	}

	public void prepareEditDiagnosisPrototypeTemplate() {
		setDiagnosisIndex(DIAGNOSIS_TEXT_TEMPLATE);
	}

	public void discardEditDiagnosisPrototypeTemplate() {
		setDiagnosisIndex(DIAGNOSIS_EDIT);
	}

	/**
	 * Is fired if the list is reordered by the user via drag and drop
	 * 
	 * @param event
	 */
	public void onReorderDiagnosisList(ReorderEvent event) {
		logger.debug("List order changed, moved material from " + event.getFromIndex() + " to " + event.getToIndex());
		ListOrder.reOrderList(getAllAvailableMaterials());
		genericDAO.save(getAllAvailableMaterials(), resourceBundle.get("log.settings.diagnosis.list.reoder"));
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

		if (getShowPhysicianRoles() == null)
			setShowPhysicianRoles(new ContactRole[] { ContactRole.PRIVATE_PHYSICIAN, ContactRole.SURGEON,
					ContactRole.OTHER_PHYSICIAN, ContactRole.SIGNATURE });

		setPhysicianList(physicianDAO.getPhysicians(getShowPhysicianRoles(), isShowArchivedPhysicians()));
	}

	/**
	 * Shows the add external or ldap screen per default the ldap select screnn
	 * is used.
	 */
	public void prepareNewPhysician() {
		setTmpPhysician(new Physician());
		getTmpPhysician().setPerson(new Person());
		setPhysicianTabIndex(SettingsTab.P_ADD_LDPA);

		setLdapPhysicianSearchString("");
		setLdapPhysicianList(new ArrayList<Physician>());
	}

	/**
	 * Shows the gui for editing an existing physician
	 * 
	 * @param physician
	 */
	public void prepareEditPhysician(Physician physician) {
		setTmpPhysician(physician);
		setPhysicianTabIndex(SettingsTab.P_EDIT);
	}

	/**
	 * Opens the passed physician in the settingsDialog in order to edit the
	 * phone number, email or faxnumber.
	 * 
	 * @param contact
	 */
	public void prepareEditPhysicianFromExtern(Person person) {
		Physician result = physicianDAO.getPhysicianByPerson(person);
		if (result != null) {
			setTmpPhysician(result);
			setPhysicianTabIndex(SettingsTab.P_EDIT_EXTERN);
			setActiveSettingsIndex(SettingsHandlerAction.TAB_PERSON);
			prepareSettingsDialog();
		}
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

		try {
			logger.debug("Search for " + request.toString());

			LdapHandler connection = mainHandlerAction.getSettings().getLdap();

			// searching for physicians
			connection.openConnection();
			setLdapPhysicianList(connection.getListOfPhysicians(request.toString()));
			connection.closeConnection();

			setTmpLdapPhysician(null);

		} catch (NamingException | IOException e) {
			logger.error("NamingException: " + e.getMessage(), e);
			setLdapPhysicianList(null);
		}
	}

	/**
	 * Saves an edited physician to the database
	 * 
	 * @param physician
	 */
	public void saveEditPhysician(Physician physician) {
		if (physician.hasNoAssociateRole())
			physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);

		genericDAO.save(physician,
				resourceBundle.get("log.settings.physician.physician.edit", physician.getPerson().getFullName()));
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
		if (physician.hasNoAssociateRole())
			physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);

		genericDAO.save(physician, resourceBundle.get("log.settings.physician.privatePhysician.save",
				physician.getPerson().getFullName()));
		discardTmpPhysician();
	}

	/**
	 * 
	 * @param ldapPhysician
	 * @param editPhysician
	 */
	public void savePhysicianFromLdap(Physician ldapPhysician, HashSet<ContactRole> roles) {
		if (ldapPhysician == null) {
			discardTmpPhysician();
			return;
		}

		// removing id from the list
		ldapPhysician.setId(0);

		if (roles == null || roles.size() == 0)
			ldapPhysician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);
		else
			ldapPhysician.setAssociatedRoles(roles);

		// tje internal physician from ldap it might have been added before (if
		// the the physician is a user of this programm),
		// search fur unique uid
		Physician physicianFromDatabase = physicianDAO.loadPhysicianByUID(ldapPhysician.getUid());

		// undating the foud physician
		if (physicianFromDatabase != null) {
			physicianFromDatabase.copyIntoObject(ldapPhysician);

			physicianFromDatabase.setArchived(false);

			// overwriting roles
			physicianFromDatabase.setAssociatedRoles(roles);

			genericDAO.save(physicianFromDatabase,
					resourceBundle.get("log.settings.physician.ldap.update", ldapPhysician.getPerson().getFullName()));
			setTmpPhysician(physicianFromDatabase);
			discardTmpPhysician();
			return;
		}

		genericDAO.save(ldapPhysician,
				resourceBundle.get("log.settings.physician.ldap.save", ldapPhysician.getPerson().getFullName()));

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
						physician.getPerson().getFullName()));
		preparePhysicianList();
	}

	/**
	 * Clears the temporary variables and the the physician list to display
	 */
	public void discardTmpPhysician() {
		// if a physician is in database and changes should be discarded, so
		// refresh from database
		if ((getPhysicianTabIndex() == SettingsTab.P_EDIT || getPhysicianTabIndex() == SettingsTab.P_EDIT_EXTERN)
				&& getTmpPhysician().getId() != 0)
			genericDAO.refresh(getTmpPhysician());

		setTmpPhysician(null);
		setTmpLdapPhysician(null);

		if (getPhysicianTabIndex() != SettingsTab.P_EDIT_EXTERN) {
			// update physician list
			preparePhysicianList();
		} else {
			// if the edit was called externally close the dialog
			hideSettingsDialog();
		}

		setPhysicianTabIndex(SettingsTab.P_LIST);
	}

	/********************************************************
	 * Physician
	 ********************************************************/

	/********************************************************
	 * Static Lists
	 ********************************************************/
	public void prepareStaticLists() {
		logger.debug("Preparing list for " + getSelectedStaticList().toString());
		setStaticListContent(settingsDAO.getAllStaticListItems(getSelectedStaticList(), isShowArchivedListItems()));
		logger.debug("Found " + (getStaticListContent() == null ? "no" : getStaticListContent().size()) + " items");
	}

	public void prepareNewListItem() {
		setStaticListTabIndex(SettingsTab.S_EDIT);
		setTmpListItem(new ListItem());
	}

	public void prepareEditListItem(ListItem listItem) {
		setStaticListTabIndex(SettingsTab.S_EDIT);
		setTmpListItem(listItem);
	}

	public void saveListItem(ListItem item, StaticList type) {

		item.setListType(type);

		if (item.getId() == 0) {
			logger.debug("Creating new ListItem " + item.getValue() + " for " + type.toString());
			// case new, save
			getStaticListContent().add(item);
			genericDAO.save(item, resourceBundle.get("log.settings.staticList.new", item.getValue(), type.toString()));
			ListOrder.reOrderList(getStaticListContent());
			genericDAO.save(getStaticListContent(),
					resourceBundle.get("log.settings.staticList.list.reoder", type.toString()));
		} else {
			logger.debug("Updating ListItem " + item.getValue());
			// case edit: update an save
			genericDAO.save(item,
					resourceBundle.get("log.settings.staticList.update", item.getValue(), type.toString()));
		}

		discardChangeOfListItem();
	}

	public void discardChangeOfListItem() {
		discardChangesOfMaterial(null);
	}

	public void discardChangesOfMaterial(ListItem item) {
		if (item != null && item.getId() != 0)
			genericDAO.refresh(item);

		setStaticListTabIndex(SettingsTab.S_LIST);
		setTmpListItem(null);
	}

	public void archiveListItem(ListItem item, boolean archive) {
		item.setArchived(archive);
		if (archive)
			genericDAO.save(item, resourceBundle.get("log.settings.staticList.archive", item.getValue(),
					getSelectedStaticList().toString()));
		else
			genericDAO.save(item, resourceBundle.get("log.settings.staticList.dearchive", item.getValue(),
					getSelectedStaticList().toString()));

		// removing item from current list
		getStaticListContent().remove(item);
	}

	public void onReorderStaticLists(ReorderEvent event) {
		logger.debug("List order changed, moved static list item from " + event.getFromIndex() + " to "
				+ event.getToIndex());
		ListOrder.reOrderList(getStaticListContent());
		genericDAO.save(getStaticListContent(),
				resourceBundle.get("log.settings.staticList.list.reoder", getSelectedStaticList().toString()));
	}

	/********************************************************
	 * Static Lists
	 ********************************************************/

	/******************************************************** History ********************************************************/
	/**
	 * Loads the current history, for all events 100 entries. Shows the current
	 * history dialog.
	 */
	public void loadGeneralHistory() {
		// setCurrentHistory(utilDAO.getCurrentHistory(100));
	}

	/**
	 * Loads the current history for the given patient. Shows the current
	 * history dialog.
	 * 
	 * @param patient
	 */
	public void loadPatientHistory(Patient patient) {
		// setCurrentHistory(utilDAO.getCurrentHistoryForPatient(100,
		// patient));
	}

	/******************************************************** History ********************************************************/

	/******************************************************** miscellaneous ********************************************************/

	/**
	 * Sets a new list with all DiagnosisProtoypes and creates a corresponding
	 * transformer
	 */
	public void updateAllDiagnosisPrototypes() {
		setAllAvailableDiagnosisPrototypes(utilDAO.getAllDiagnosisPrototypes());
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

	public MaterialPreset getEditMaterial() {
		return editMaterial;
	}

	public void setEditMaterial(MaterialPreset editMaterial) {
		this.editMaterial = editMaterial;
	}

	public MaterialPreset getOriginalMaterial() {
		return originalMaterial;
	}

	public void setOriginalMaterial(MaterialPreset originalMaterial) {
		this.originalMaterial = originalMaterial;
	}

	public List<ListChooser<StainingPrototype>> getStainingListChooserForMaterial() {
		return stainingListChooserForMaterial;
	}

	public void setStainingListChooserForMaterial(List<ListChooser<StainingPrototype>> stainingListChooserForMaterial) {
		this.stainingListChooserForMaterial = stainingListChooserForMaterial;
	}

	public List<DiagnosisPreset> getAllAvailableDiagnosisPrototypes() {
		return allAvailableDiagnosisPrototypes;
	}

	public void setAllAvailableDiagnosisPrototypes(List<DiagnosisPreset> allAvailableDiagnosisPrototypes) {
		this.allAvailableDiagnosisPrototypes = allAvailableDiagnosisPrototypes;
	}

	public DiagnosisPreset getEditDiagnosisPrototype() {
		return editDiagnosisPrototype;
	}

	public void setEditDiagnosisPrototype(DiagnosisPreset editDiagnosisPrototype) {
		this.editDiagnosisPrototype = editDiagnosisPrototype;
	}

	public int getDiagnosisIndex() {
		return diagnosisIndex;
	}

	public void setDiagnosisIndex(int diagnosisIndex) {
		this.diagnosisIndex = diagnosisIndex;
	}

	public DiagnosisPreset getOriginalDiagnosisPrototype() {
		return originalDiagnosisPrototype;
	}

	public void setOriginalDiagnosisPrototype(DiagnosisPreset originalDiagnosisPrototype) {
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

	public SettingsTab getPhysicianTabIndex() {
		return physicianTabIndex;
	}

	public void setPhysicianTabIndex(SettingsTab physicianTabIndex) {
		this.physicianTabIndex = physicianTabIndex;
	}

	public HistoUser getSelectedUser() {
		return selectedUser;
	}

	public void setSelectedUser(HistoUser selectedUser) {
		this.selectedUser = selectedUser;
	}

	public SettingsTab getMaterialTabIndex() {
		return materialTabIndex;
	}

	public void setMaterialTabIndex(SettingsTab materialTabIndex) {
		this.materialTabIndex = materialTabIndex;
	}

	public StaticList getSelectedStaticList() {
		return selectedStaticList;
	}

	public void setSelectedStaticList(StaticList selectedStaticList) {
		this.selectedStaticList = selectedStaticList;
	}

	public List<ListItem> getStaticListContent() {
		return staticListContent;
	}

	public void setStaticListContent(List<ListItem> staticListContent) {
		this.staticListContent = staticListContent;
	}

	public SettingsTab getStaticListTabIndex() {
		return staticListTabIndex;
	}

	public void setStaticListTabIndex(SettingsTab staticListTabIndex) {
		this.staticListTabIndex = staticListTabIndex;
	}

	public ListItem getTmpListItem() {
		return tmpListItem;
	}

	public void setTmpListItem(ListItem tmpListItem) {
		this.tmpListItem = tmpListItem;
	}

	public boolean isShowArchivedListItems() {
		return showArchivedListItems;
	}

	public void setShowArchivedListItems(boolean showArchivedListItems) {
		this.showArchivedListItems = showArchivedListItems;
	}

	public SettingsTab getUserListTabIndex() {
		return userListTabIndex;
	}

	public void setUserListTabIndex(SettingsTab userListTabIndex) {
		this.userListTabIndex = userListTabIndex;
	}

	public Physician getSelectedUserPhysician() {
		return selectedUserPhysician;
	}

	public void setSelectedUserPhysician(Physician selectedUserPhysician) {
		this.selectedUserPhysician = selectedUserPhysician;
	}

	public boolean isShowArchivedPhysicians() {
		return showArchivedPhysicians;
	}

	public void setShowArchivedPhysicians(boolean showArchivedPhysicians) {
		this.showArchivedPhysicians = showArchivedPhysicians;
	}

	public ContactRole[] getShowPhysicianRoles() {
		return showPhysicianRoles;
	}

	public void setShowPhysicianRoles(ContactRole[] showPhysicianRoles) {
		this.showPhysicianRoles = showPhysicianRoles;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
