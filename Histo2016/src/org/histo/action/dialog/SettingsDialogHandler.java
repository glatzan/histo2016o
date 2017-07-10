package org.histo.action.dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.histo.action.CommonDataHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.LogDAO;
import org.histo.dao.OrganizationDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.UserDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.Contact;
import org.histo.model.DiagnosisPreset;
import org.histo.model.FavouriteList;
import org.histo.model.FavouriteListItem;
import org.histo.model.HistoUser;
import org.histo.model.ListItem;
import org.histo.model.Log;
import org.histo.model.MaterialPreset;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.patient.Patient;
import org.histo.settings.LdapHandler;
import org.histo.ui.ListChooser;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.histo.ui.transformer.DefaultTransformer;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class SettingsDialogHandler extends AbstractDialog {

	private static Logger logger = Logger.getLogger("org.histo");

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
	private SettingsHandler settingsHandler;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private OrganizationDAO organizationDAO;

	@Autowired
	private LogDAO logDAO;

	/**
	 * Tabindex of settings dialog
	 */
	@Getter
	@Setter
	private int activeSettingsIndex = 0;

	@Getter
	@Setter
	public AbstractSettingsTab[] tabs = new AbstractSettingsTab[] { new HistoUserTab(), new DiagnosisTab(),
			new MaterialTab(), new StainingTab(), new StaticListTab(), new FavouriteListTab(),
			new PhysicianSettingsTab(), new OrganizationTab(), new LogTab() };

	/********************************************************
	 * General
	 ********************************************************/

	public void initAndPrepareBean() {
		if (initBean(getActiveSettingsIndex()))
			prepareDialog();
	}

	public void initAndPrepareBean(String tabName) {
		if (initBean(tabName))
			prepareDialog();
	}

	public boolean initBean(String tabName) {
		int tabNumber = 0;

		for (int i = 0; i < tabs.length; i++)
			if (tabs[i].getTabName().equals(tabName)) {
				tabNumber = i;
				break;
			}

		return initBean(tabNumber);
	}

	public boolean initBean(int activeTab) {

		if (activeTab >= 0 && activeTab < getTabs().length) {
			setActiveSettingsIndex(activeTab);

			onSettingsTabChange(null);
		} else {
			return false;
		}

		super.initBean(task, Dialog.SETTINGS);

		return true;
	}

	public void onSettingsTabChange(TabChangeEvent event) {
		if (getActiveSettingsIndex() >= 0 && getActiveSettingsIndex() < getTabs().length) {
			logger.debug("Updating Tab with index " + getActiveSettingsIndex());
			getTabs()[getActiveSettingsIndex()].updateData();
		}
	}

	public AbstractSettingsTab getTab(String tabName) {
		for (AbstractSettingsTab abstractSettingsTab : tabs) {
			if (abstractSettingsTab.getTabName().equals(tabName))
				return abstractSettingsTab;
		}

		return null;
	}

	/**
	 * Show the adminSettigns Dialog and inits the used values
	 */

	public void prepareSettingsDialog() {
		prepareSettingsDialog(getActiveSettingsIndex());
	}

	// public void prepareSettingsDialog(SettingsTab settingsTab) {
	// // SettingsTab parentTab = settingsTab.getParent() != null ?
	// // settingsTab.getParent() : settingsTab;
	// //
	// // switch (parentTab) {
	// // case PHYSICIAN:
	// // setPhysicianTabIndex(settingsTab);
	// // break;
	// // default:
	// // break;
	// // }
	// //
	// // prepareSettingsDialog(settingsTab.getTabNumber());
	// }

	public void prepareSettingsDialog(int activeTab) {
		super.initBean(null, Dialog.SETTINGS);

		mainHandlerAction.showDialog(Dialog.SETTINGS);
	}

	/********************************************************
	 * General
	 ********************************************************/

	/********************************************************
	 * History
	 ********************************************************/
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

	/********************************************************
	 * History
	 ********************************************************/

	@Getter
	@Setter
	public abstract class AbstractSettingsTab {

		public abstract String getCenterView();

		public abstract void updateData();

		protected String name;

		protected String viewID;

		protected String tabName;
	}

	public static enum HistoUserPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class HistoUserTab extends AbstractSettingsTab {

		private HistoUserPage page;

		private List<HistoUser> users;

		private HistoUser selectedUser;

		private List<ContactRole> allRoles;

		public HistoUserTab() {
			setTabName("HistoUserTab");
			setName("dialog.settings.user");
			setViewID("histoUser");
			setPage(HistoUserPage.LIST);
			setAllRoles(Arrays.asList(ContactRole.values()));
		}

		public void updateData() {
			switch (page) {
			case EDIT:
				break;
			default:
				setUsers(userDAO.loadAllUsers());
				break;
			}
		}

		public void onChangeUserRole(HistoUser histoUser) {
			try {
				userHandlerAction.roleOfuserHasChanged(histoUser);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * prepares an edit dialog for editing user data, only avaliable for
		 * admins
		 * 
		 * @param physician
		 */
		public void prepareEditHistoUser(HistoUser histoUser) {
			setSelectedUser(histoUser);
			setPage(HistoUserPage.EDIT);

			updateData();
		}

		/**
		 * Saves an edited physician to the database
		 * 
		 * @param physician
		 */
		public void saveHistoUser() {
			try {
				if (getSelectedUser().getPhysician().hasNoAssociateRole())
					getSelectedUser().getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);

				genericDAO.saveDataRollbackSave(getSelectedUser().getPhysician(),
						resourceBundle.get("log.settings.physician.physician.edit",
								getSelectedUser().getPhysician().getPerson().getFullName()));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Shows the userlist aganin
		 */
		public void discardHistoUser() {
			genericDAO.reset(getSelectedUser());
			setSelectedUser(null);
			setPage(HistoUserPage.LIST);

			updateData();
		}

		@Override
		public String getCenterView() {
			if (getPage() == HistoUserPage.EDIT)
				return "histoUser/userEdit.xhtml";
			else
				return "histoUser/userList.xhtml";
		}

	}

	public enum DiagnosisPage {
		LIST, EDIT, EDIT_TEXT_TEMPLATE;
	}

	@Getter
	@Setter
	public class DiagnosisTab extends AbstractSettingsTab {

		private DiagnosisPage page;

		private List<DiagnosisPreset> diagnosisPresets;

		private DefaultTransformer<DiagnosisPreset> diagnosisPresetsTransformer;

		private DiagnosisPreset selectedDiagnosisPreset;

		private boolean newDiagnosisPreset;

		private ContactRole[] allRoles;

		public DiagnosisTab() {
			setTabName("DiagnosisTab");
			setName("dialog.settings.diagnosis");
			setViewID("diagnoses");
			setPage(DiagnosisPage.LIST);

			setAllRoles(new ContactRole[] { ContactRole.FAMILY_PHYSICIAN, ContactRole.PATIENT, ContactRole.SURGEON,
					ContactRole.PRIVATE_PHYSICIAN, ContactRole.RELATIVES });
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
			case EDIT_TEXT_TEMPLATE:
				if (getSelectedDiagnosisPreset() != null && getSelectedDiagnosisPreset().getId() != 0)
					setSelectedDiagnosisPreset(
							genericDAO.get(DiagnosisPreset.class, getSelectedDiagnosisPreset().getId()));
				break;
			default:
				setDiagnosisPresets(utilDAO.getAllDiagnosisPrototypes());
				break;
			}

		}

		public void prepareNewDiagnosisPreset() {
			prepareEditDiagnosisPreset(new DiagnosisPreset());
		}

		public void prepareEditDiagnosisPreset(DiagnosisPreset diagnosisPreset) {
			setSelectedDiagnosisPreset(diagnosisPreset);
			setPage(DiagnosisPage.EDIT);

			setNewDiagnosisPreset(diagnosisPreset.getId() == 0 ? true : false);

			updateData();
		}

		public void saveDiagnosisPreset() {
			try {
				if (getSelectedDiagnosisPreset().getId() == 0) {

					// case new, save
					logger.debug("Creating new diagnosis " + getSelectedDiagnosisPreset().getCategory());

					getDiagnosisPresets().add(getSelectedDiagnosisPreset());

					genericDAO.saveDataRollbackSave(getSelectedDiagnosisPreset(), resourceBundle
							.get("log.settings.diagnosis.new", getSelectedDiagnosisPreset().getCategory()));

					ListOrder.reOrderList(getDiagnosisPresets());

					genericDAO.saveListRollbackSave(getDiagnosisPresets(),
							resourceBundle.get("log.settings.diagnosis.list.reoder"));

				} else {

					System.out.println(getSelectedDiagnosisPreset().getIndexInList());
					// case edit: update an save
					logger.debug("Updating diagnosis " + getSelectedDiagnosisPreset().getCategory());

					genericDAO.saveDataRollbackSave(getSelectedDiagnosisPreset(), resourceBundle
							.get("log.settings.diagnosis.update", getSelectedDiagnosisPreset().getCategory()));
				}

				discardDiagnosisPreset();

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Discards all changes of a diagnosisPrototype
		 */
		public void discardDiagnosisPreset() {
			if (getSelectedDiagnosisPreset().getId() != 0)
				genericDAO.reset(getSelectedDiagnosisPreset());
			setPage(DiagnosisPage.LIST);
			setSelectedDiagnosisPreset(null);

			updateData();
		}

		public void prepareEditDiagnosisPresetTemplate() {
			setPage(DiagnosisPage.EDIT_TEXT_TEMPLATE);
		}

		public void discardEditDiagnosisPresetTemplate() {
			setPage(DiagnosisPage.EDIT);
		}

		/**
		 * Is fired if the list is reordered by the user via drag and drop
		 *
		 * @param event
		 */
		public void onReorderList(ReorderEvent event) {

			try {
				logger.debug("List order changed, moved material from " + event.getFromIndex() + " to "
						+ event.getToIndex());

				ListOrder.reOrderList(getDiagnosisPresets());

				genericDAO.saveListRollbackSave(getDiagnosisPresets(),
						resourceBundle.get("log.settings.diagnosis.list.reoder"));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "diagnosis/diagnosisEdit.xhtml";
			case EDIT_TEXT_TEMPLATE:
				return "diagnosis/diagnosisEditTemplate.xhtml";
			default:
				return "diagnosis/diagnosisList.xhtml";
			}
		}
	}

	public enum MaterialPage {
		LIST, EDIT, ADD_STAINING;
	}

	@Getter
	@Setter
	public class MaterialTab extends AbstractSettingsTab {

		public MaterialPage page;

		private List<MaterialPreset> allMaterialList;

		/**
		 * StainingPrototype for creating and editing
		 */
		private MaterialPreset editMaterial;

		/**
		 * List for selecting staining, this list contains all stainings. They
		 * can be choosen and added to the material
		 */
		private List<ListChooser<StainingPrototype>> stainingListChooserForMaterial;

		private boolean newMaterial;

		public MaterialTab() {
			setTabName("MaterialTab");
			setName("dialog.settings.materials");
			setViewID("material");
			setPage(MaterialPage.LIST);
		}

		@Override
		public void updateData() {

			switch (getPage()) {
			case EDIT:
			case ADD_STAINING:
				setStainingListChooserForMaterial(utilDAO.getAllStainingPrototypes().stream()
						.map(p -> new ListChooser<StainingPrototype>(p)).collect(Collectors.toList()));
				break;
			default:
				setAllMaterialList(utilDAO.getAllMaterialPresets(true));
				break;
			}
		}

		/**
		 * Prepares a new StainingListChooser for editing
		 */
		public void prepareNewMaterial() {
			prepareEditMaterial(new MaterialPreset());
		}

		/**
		 * Shows the edit material form
		 *
		 * @param stainingPrototype
		 */
		public void prepareEditMaterial(MaterialPreset material) {
			setPage(MaterialPage.EDIT);
			setEditMaterial(material);

			setNewMaterial(material.getId() == 0 ? true : false);

			updateData();
		}

		/**
		 * Saves a material or creates a new one
		 *
		 * @param newStainingPrototypeList
		 * @param origStainingPrototypeList
		 */
		public void saveMaterial() {
			try {
				if (getEditMaterial().getId() == 0) {
					logger.debug("Creating new Material " + getEditMaterial().getName());
					// case new, save+
					getAllMaterialList().add(getEditMaterial());

					genericDAO.saveDataRollbackSave(getEditMaterial(),
							resourceBundle.get("log.settings.material.new", getEditMaterial().getName()));

					ListOrder.reOrderList(getAllMaterialList());

					genericDAO.saveListRollbackSave(getAllMaterialList(),
							resourceBundle.get("log.settings.material.list.reoder"));

				} else {
					logger.debug("Updating Material " + getEditMaterial().getName());
					// case edit: update an save
					genericDAO.saveDataRollbackSave(getEditMaterial(),
							resourceBundle.get("log.settings.material.update", getEditMaterial().getName()));
				}
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * discards all changes for the stainingList
		 */
		public void discardMaterial() {
			if (getEditMaterial().getId() != 0)
				genericDAO.reset(getEditMaterial());
			setPage(MaterialPage.LIST);
			setEditMaterial(null);

			updateData();
		}

		/**
		 * show a list with all stanings for adding them to a material
		 */
		public void prepareAddStainingToMaterial() {
			setPage(MaterialPage.ADD_STAINING);
		}

		/**
		 * Adds all selected staining prototypes to the material
		 *
		 * @param stainingListChoosers
		 * @param stainingPrototypeList
		 */
		public void addStainingToMaterial() {
			getStainingListChooserForMaterial().forEach(p -> {
				if (p.isChoosen()) {
					getEditMaterial().getStainingPrototypes().add(p.getListItem());
				}
			});
		}

		/**
		 * Removes a staining from a material
		 *
		 * @param toRemove
		 * @param stainingPrototypeList
		 */
		public void removeStainingFromStainingList(StainingPrototype toRemove) {
			getEditMaterial().getStainingPrototypes().remove(toRemove);
		}

		public void discardAddStainingToMaterial() {
			setPage(MaterialPage.EDIT);
		}

		/**
		 * Is fired if the list is reordered by the user via drag and drop
		 *
		 * @param event
		 */
		public void onReorderList(ReorderEvent event) {
			try {
				logger.debug("List order changed, moved material from " + event.getFromIndex() + " to "
						+ event.getToIndex());
				ListOrder.reOrderList(getAllMaterialList());

				genericDAO.saveListRollbackSave(getAllMaterialList(),
						resourceBundle.get("log.settings.staining.list.reoder"));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "material/materialEdit.xhtml";
			case ADD_STAINING:
				return "material/materialAddStaining.xhtml";
			default:
				return "material/materialList.xhtml";
			}
		}
	}

	public enum StainingPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class StainingTab extends AbstractSettingsTab {

		private StainingPage page;

		/**
		 * A List with all staings
		 */
		private List<StainingPrototype> allStainingsList;

		/**
		 * StainingPrototype for creating and editing
		 */
		private StainingPrototype editStaining;

		private boolean newStaining;

		public StainingTab() {
			setTabName("StainingTab");
			setName("dialog.settings.stainings");
			setViewID("staining");
			setPage(StainingPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				setAllStainingsList(utilDAO.getAllStainingPrototypes());
				break;
			}

		}

		/**
		 * Prepares a new Staining for editing
		 */
		public void prepareNewStaining() {
			prepareEditStaining(new StainingPrototype());
		}

		/**
		 * Shows the edit staining form
		 *
		 * @param stainingPrototype
		 */
		public void prepareEditStaining(StainingPrototype stainingPrototype) {
			setEditStaining(stainingPrototype);
			setNewStaining(stainingPrototype.getId() == 0 ? true : false);
			setPage(StainingPage.EDIT);
		}

		/**
		 * Saves or creats a new stainingprototype
		 *
		 * @param newStainingPrototype
		 * @param origStainingPrototype
		 */
		public void saveStainig() {
			try {
				if (getEditStaining().getId() == 0) {
					logger.debug("Creating new staining " + getEditStaining().getName());
					// case new, save
					getAllStainingsList().add(getEditStaining());

					genericDAO.saveDataRollbackSave(getEditStaining(),
							resourceBundle.get("log.settings.staining.new", getEditStaining().getName()));

					ListOrder.reOrderList(getAllStainingsList());

					genericDAO.saveListRollbackSave(getAllStainingsList(),
							resourceBundle.get("log.settings.staining.list.reoder"));
				} else {
					genericDAO.saveDataRollbackSave(getEditStaining(),
							resourceBundle.get("log.settings.material.update", getEditStaining().getName()));
				}

			} catch (

			CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Is fired if the list is reordered by the user via drag and drop
		 *
		 * @param event
		 */
		public void onReorderList(ReorderEvent event) {
			try {
				logger.debug("List order changed, moved staining from " + event.getFromIndex() + " to "
						+ event.getToIndex());
				ListOrder.reOrderList(getAllStainingsList());
				genericDAO.saveListRollbackSave(getAllStainingsList(),
						resourceBundle.get("log.settings.staining.list.reoder"));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * discards changes
		 */
		public void discardStainig() {
			if (getEditStaining().getId() != 0)
				genericDAO.reset(getEditStaining());
			setPage(StainingPage.LIST);
			setEditStaining(null);

			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "staining/stainingsEdit.xhtml";
			default:
				return "staining/stainingsList.xhtml";
			}
		}

	}

	public enum StaticListPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class StaticListTab extends AbstractSettingsTab {

		private StaticListPage page;

		/**
		 * Current static list to edit
		 */
		private ListItem.StaticList selectedStaticList = ListItem.StaticList.WARDS;

		/**
		 * Content of the current static list
		 */
		private List<ListItem> staticListContent;

		/**
		 * Is used for creating and editing static lists items
		 */
		private ListItem editListItem;

		/**
		 * If true archived object will be shown.
		 */
		private boolean showArchivedListItems;

		private boolean newListItem;

		public StaticListTab() {
			setTabName("StaticListTab");
			setName("dialog.settings.staticLists");
			setViewID("staticLists");
			setPage(StaticListPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				loadStaticList();
				break;
			}
		}

		public void loadStaticList() {
			setStaticListContent(utilDAO.getAllStaticListItems(getSelectedStaticList(), isShowArchivedListItems()));
		}

		public void prepareNewListItem() {
			prepareEditListItem(new ListItem());
		}

		public void prepareEditListItem(ListItem listItem) {
			setEditListItem(listItem);
			setPage(StaticListPage.EDIT);
			setNewListItem(listItem.getId() == 0 ? true : false);
		}

		public void saveListItem() {
			try {

				if (getEditListItem().getId() == 0) {

					getEditListItem().setListType(getSelectedStaticList());

					logger.debug("Creating new ListItem " + getEditListItem().getValue() + " for "
							+ getSelectedStaticList().toString());
					// case new, save
					getStaticListContent().add(getEditListItem());

					genericDAO.saveDataRollbackSave(getEditListItem(), resourceBundle.get("log.settings.staticList.new",
							getEditListItem().getValue(), getSelectedStaticList().toString()));

					ListOrder.reOrderList(getStaticListContent());

					genericDAO.saveListRollbackSave(getStaticListContent(), resourceBundle
							.get("log.settings.staticList.list.reoder", getSelectedStaticList().toString()));
				} else {
					logger.debug("Updating ListItem " + getEditListItem().getValue());
					// case edit: update an save

					genericDAO.saveDataRollbackSave(getEditListItem(),
							resourceBundle.get("log.settings.staticList.update", getEditListItem().getValue(),
									getSelectedStaticList().toString()));
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardListItem() {
			if (getEditListItem().getId() != 0)
				genericDAO.reset(getEditListItem());

			setEditListItem(null);
			setPage(StaticListPage.LIST);

			updateData();
		}

		public void archiveListItem(ListItem item, boolean archive) {
			try {
				item.setArchived(archive);
				if (archive) {
					genericDAO.saveDataRollbackSave(item, resourceBundle.get("log.settings.staticList.archive",
							item.getValue(), getSelectedStaticList().toString()));
				} else {
					genericDAO.saveDataRollbackSave(item, resourceBundle.get("log.settings.staticList.dearchive",
							item.getValue(), getSelectedStaticList().toString()));
				}

				// removing item from current list
				getStaticListContent().remove(item);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void onReorderList(ReorderEvent event) {
			try {
				ListOrder.reOrderList(getStaticListContent());

				genericDAO.saveListRollbackSave(getStaticListContent(),
						resourceBundle.get("log.settings.staticList.list.reoder", getSelectedStaticList().toString()));
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "staticLists/staticListsEdit.xhtml";
			default:
				return "staticLists/staticLists.xhtml";
			}
		}

	}

	public enum PhysicianSettingsPage {
		LIST, EDIT, EDIT_EXTERN, ADD_EXTERN, ADD_LDAP;
	}

	@Getter
	@Setter
	public class PhysicianSettingsTab extends AbstractSettingsTab {

		/**
		 * Tabindex of the settings tab
		 */
		private PhysicianSettingsPage page;

		/**
		 * True if archived physicians should be display
		 */
		private boolean showArchivedPhysicians;

		/**
		 * Array of roles for that physicians should be shown.
		 */
		private ContactRole[] showPhysicianRoles;

		/**
		 * All available roles
		 */
		private List<ContactRole> allRoles;

		/**
		 * List containing all physicians known in the histo database
		 */
		private List<Physician> physicianList;

		/**
		 * Used for creating new or for editing existing physicians
		 */
		private Physician tmpPhysician;

		/**
		 * True if new physician
		 */
		private boolean newPhysician;

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

		public PhysicianSettingsTab() {
			setTabName("PhysicianSettingsTab");
			setName("dialog.settings.persons");
			setViewID("persons");
			setPage(PhysicianSettingsPage.LIST);
			setShowArchivedPhysicians(false);
			setAllRoles(Arrays.asList(ContactRole.values()));
			setShowPhysicianRoles(new ContactRole[] { ContactRole.PRIVATE_PHYSICIAN, ContactRole.SURGEON,
					ContactRole.OTHER_PHYSICIAN, ContactRole.SIGNATURE });
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				setPhysicianList(physicianDAO.getPhysicians(getShowPhysicianRoles(), isShowArchivedPhysicians()));
				break;
			}
		}

		/**
		 * Shows the add external or ldap screen per default the ldap select
		 * screnn is used.
		 */
		public void prepareNewPhysician() {
			setTmpPhysician(new Physician());
			getTmpPhysician().setPerson(new Person(new Contact()));
			setPage(PhysicianSettingsPage.ADD_LDAP);
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
			setPage(PhysicianSettingsPage.EDIT);
		}

		/**
		 * Opens the passed physician in the settingsDialog in order to edit the
		 * phone number, email or faxnumber.
		 *
		 * @param associatedContact
		 */
		public void prepareEditPhysicianFromExtern(Person person) {
			// Physician result = physicianDAO.getPhysicianByPerson(person);
			// if (result != null) {
			// setTmpPhysician(result);
			// setPhysicianTabIndex(SettingsTab.P_EDIT_EXTERN);
			// setActiveSettingsIndex(SettingsTab.PHYSICIAN.getTabNumber());
			// prepareSettingsDialog();
			// }
		}

		/**
		 * Generates an ldap search filter (?(xxx)....) and offers the result
		 * list. The result list is a physician list with minimal details.
		 * Before adding an clinic physician a ldap fetch for more details has
		 * to be done
		 *
		 * @param name
		 */
		public void searchForPhysician(String name) {
			// removing multiple spaces an commas and replacing them with one
			// space,
			// splitting the whole thing into an array
			String[] arr = name.replaceAll("[ ,]+", " ").split(" ");
			StringBuffer request = new StringBuffer("(&");
			for (int i = 0; i < arr.length; i++) {
				request.append("(cn=*" + arr[i] + "*)");
			}
			request.append(")");

			try {
				logger.debug("Search for " + request.toString());

				LdapHandler connection = settingsHandler.getLdapHandler();

				// searching for physicians
				connection.openConnection();
				setLdapPhysicianList(connection.getListOfPhysicians(request.toString()));
				connection.closeConnection();

				if (getLdapPhysicianList().size() == 1)
					setTmpLdapPhysician(getLdapPhysicianList().get(0));
				else
					setTmpLdapPhysician(null);

			} catch (NamingException | IOException e) {
				setLdapPhysicianList(null);
				// TODO to many results
			}
		}

		/**
		 * Saves a physician to the database, if no role was selected
		 * ContactRole.Other will be set per default.
		 *
		 * @param physician
		 */
		public void savePhysician() {
			try {

				// always set role to miscellaneous if no other role was
				// selected
				if (getTmpPhysician().hasNoAssociateRole())
					getTmpPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);

				//
				if (getTmpPhysician().getId() == 0) {
					genericDAO.saveDataRollbackSave(getTmpPhysician(),
							resourceBundle.get("log.settings.physician.privatePhysician.save",
									getTmpPhysician().getPerson().getFullName()));
				} else {
					physicianDAO.save(getTmpPhysician(), "log.settings.physician.physician.edit",
							new Object[] { getTmpPhysician().getPerson().getFullName() });
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void savePhysicianFromLdap() {
			try {

				if (getTmpLdapPhysician() == null) {
					return;
				}

				// removing id from the list
				getTmpLdapPhysician().setId(0);

				if (getTmpPhysician().getAssociatedRoles() == null
						|| getTmpPhysician().getAssociatedRoles().size() == 0)
					getTmpLdapPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);
				else
					getTmpLdapPhysician().setAssociatedRoles(getTmpPhysician().getAssociatedRoles());

				// saving organisation if new
				for (Organization organization : getTmpLdapPhysician().getPerson().getOrganizsations()) {
					if (organization.getId() == 0) {
						logger.debug("Organization " + organization.toString() + " not found, creating new one");
						organizationDAO.save(organization, "log.organization.created",
								new Object[] { organization.toString() });
					}
				}

				// the internal physician from ldap it might have been added
				// before (if the the physician is a user of this program),
				// search fur unique uid
				Physician physicianFromDatabase = physicianDAO.loadPhysicianByUID(getTmpLdapPhysician().getUid());

				// undating the foud physician
				if (physicianFromDatabase != null) {
					logger.debug("Physician found updating");
					physicianFromDatabase.copyIntoObject(getTmpLdapPhysician());

					physicianFromDatabase.setArchived(false);

					// overwriting roles
					physicianFromDatabase.setAssociatedRoles(getTmpPhysician().getAssociatedRoles());

					physicianDAO.save(physicianFromDatabase, resourceBundle.get("log.settings.physician.ldap.update",
							getTmpLdapPhysician().getPerson().getFullName()));

					setTmpPhysician(physicianFromDatabase);

				} else {
					logger.debug("Physician not found, creating new phyisician");
					physicianDAO.save(getTmpLdapPhysician(), resourceBundle.get("log.settings.physician.ldap.save",
							getTmpLdapPhysician().getPerson().getFullName()));
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Archvies or dearchvies physicians depending on the given parameters.
		 *
		 * @param physician
		 * @param archive
		 */
		public void archivePhysician(Physician physician, boolean archive) {
			try {
				physician.setArchived(archive);
				genericDAO.saveDataRollbackSave(physician,
						resourceBundle.get(
								archive ? "log.settings.physician.archived" : "log.settings.physician.archived.undo",
								physician.getPerson().getFullName()));

				updateData();
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Clears the temporary variables and the the physician list to display
		 */
		public void discardTmpPhysician() {
			setTmpPhysician(null);
			setTmpLdapPhysician(null);
			setPage(PhysicianSettingsPage.LIST);

			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
			case EDIT_EXTERN:
				return "physician/physicianEdit.xhtml";
			case ADD_EXTERN:
				return "physician/physicianNewExtern.xhtml";
			case ADD_LDAP:
				return "physician/physicianNewLdap.xhtml";
			default:
				return "physician/physicianList.xhtml";
			}
		}

	}

	public enum FavouriteListPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class FavouriteListTab extends AbstractSettingsTab {

		private FavouriteListPage page;

		/**
		 * Array containing all favourite listis
		 */
		private List<FavouriteList> favouriteLists;

		/**
		 * Temporaray faourite list
		 */
		private FavouriteList tmpFavouriteList;

		/**
		 * True if new favouriteList should be created
		 */
		private boolean newFavouriteList;

		public FavouriteListTab() {
			setTabName("FavouriteListTab");
			setName("dialog.settings.favouriteList");
			setViewID("favouriteLists");
			setPage(FavouriteListPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				if (getTmpFavouriteList().getId() != 0)
					setTmpFavouriteList(favouriteListDAO.getFavouriteList(getTmpFavouriteList().getId(), true));
				break;
			default:
				setFavouriteLists(favouriteListDAO.getAllFavouriteLists());
				break;
			}

		}

		public void prepareNewFavouriteList() {
			FavouriteList newList = new FavouriteList();
			newList.setItems(new ArrayList<FavouriteListItem>());
			newList.setDefaultList(false);
			newList.setEditAble(true);
			newList.setGlobal(true);
			prepareEditFavouriteList(newList);
		}

		public void prepareEditFavouriteList(FavouriteList favouriteList) {
			setTmpFavouriteList(favouriteList);
			setPage(FavouriteListPage.EDIT);
			setNewFavouriteList(favouriteList.getId() == 0 ? true : false);
			updateData();
		}

		public void saveFavouriteList() {
			try {
				// saving new list
				if (getTmpFavouriteList().getId() == 0) {
					genericDAO.saveDataRollbackSave(getTmpFavouriteList(), "log.settings.favouriteList.new",
							new Object[] { getTmpFavouriteList().toString() });
				} else {
					// updating old list
					genericDAO.saveDataRollbackSave(getTmpFavouriteList(), "log.settings.favouriteList.edit",
							new Object[] { getTmpFavouriteList().toString() });
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardFavouriteList() {
			setTmpFavouriteList(null);
			setPage(FavouriteListPage.LIST);
			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "favouriteLists/favouriteListEdit.xhtml";
			default:
				return "favouriteLists/favouriteList.xhtml";
			}
		}

	}

	public enum OrganizationTabPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class OrganizationTab extends AbstractSettingsTab {

		private OrganizationTabPage page;

		private List<Organization> organizations;

		private Organization selectedOrganization;

		private boolean newOrganization;

		public OrganizationTab() {
			setTabName("OrganizationTab");
			setName("dialog.settings.organization");
			setViewID("organizations");
			setPage(OrganizationTabPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				setOrganizations(organizationDAO.getOrganizations());
				break;
			}

		}

		public void prepareNewOrganization() {
			Organization organization = new Organization(new Contact());
			prepareEditOrganization(organization);
		}

		public void prepareEditOrganization(Organization organization) {
			setSelectedOrganization(organization);
			setPage(OrganizationTabPage.EDIT);
			setNewOrganization(organization.getId() == 0 ? true : false);
			updateData();
		}

		public void saveOrganization() {
			try {
				organizationDAO.save(getSelectedOrganization(),
						getSelectedOrganization().getId() == 0 ? "log.organization.save" : "log.organization.created",
						new Object[] { getSelectedOrganization().getName() });
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardOrganization() {
			setSelectedOrganization(null);
			setPage(OrganizationTabPage.LIST);

			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "organization/organizationEdit.xhtml";
			default:
				return "organization/organizationLists.xhtml";
			}
		}

	}

	@Getter
	@Setter
	public class LogTab extends AbstractSettingsTab {

		private int logsPerPull;

		private int selectedLogPage;

		private List<Log> logs;

		private int maxLogPages;

		public LogTab() {
			setTabName("LogTab");
			setName("dialog.settings.log");
			setViewID("logs");

			setLogsPerPull(50);
			setSelectedLogPage(1);
		}

		@Override
		public void updateData() {
			int maxPages = logDAO.countTotalLogs();
			int pagesCount = (int) Math.ceil((double) maxPages / logsPerPull);

			setMaxLogPages(pagesCount);

			setLogs(logDAO.getLogs(getLogsPerPull(), getSelectedLogPage() - 1));
		}

		@Override
		public String getCenterView() {
			// TODO Auto-generated method stub
			return "log/log.xhtml";
		}
	}

	public class AdminTab extends AbstractSettingsTab {

		public AdminTab() {
			setTabName("AdminTab");
			setName("dialog.settings.admin");
			setViewID("admin");
		}

		@Override
		public String getCenterView() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub

		}

	}
}
