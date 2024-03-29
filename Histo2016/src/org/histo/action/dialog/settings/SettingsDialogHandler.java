package org.histo.action.dialog.settings;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractTabDialog;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.LogDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Organization;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.log.Log;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.histo.service.PhysicianService;
import org.histo.service.UserService;
import org.histo.service.dao.GroupDao;
import org.histo.service.dao.OrganizationDao;
import org.histo.service.dao.PhysicianDao;
import org.histo.service.dao.UserDao;
import org.histo.ui.ListChooser;
import org.histo.ui.transformer.DefaultTransformer;
import org.primefaces.event.ReorderEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class SettingsDialogHandler extends AbstractTabDialog {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final int DIAGNOSIS_LIST = 0;
	public static final int DIAGNOSIS_EDIT = 1;
	public static final int DIAGNOSIS_TEXT_TEMPLATE = 2;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserDao userDao;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserService userService;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GroupDao groupDao;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianDao physicianDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianService physicianService;
	
	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private OrganizationDao organizationDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private LogDAO logDAO;

	private ProgramParentTab programParentTab;
	private HistoUserTab histoUserTab;
	private HistoGroupTab histoGroupTab;
	private DiagnosisTab diagnosisTab;
	private MaterialTab materialTab;
	private StainingTab stainingTab;
	private StaticListTab staticListTab;
	private FavouriteListTab favouriteListTab;
	private PersonParentTab personParentTab;
	private PhysicianSettingsTab physicianSettingsTab;
	private OrganizationTab organizationTab;
	private LogTab logTab;

	public SettingsDialogHandler() {
		setProgramParentTab(new ProgramParentTab());
		setHistoUserTab(new HistoUserTab());
		setHistoGroupTab(new HistoGroupTab());
		setDiagnosisTab(new DiagnosisTab());
		setMaterialTab(new MaterialTab());
		setStainingTab(new StainingTab());
		setStaticListTab(new StaticListTab());
		setFavouriteListTab(new FavouriteListTab());
		setPersonParentTab(new PersonParentTab());
		setPhysicianSettingsTab(new PhysicianSettingsTab());
		setOrganizationTab(new OrganizationTab());
		setLogTab(new LogTab());

		tabs = new AbstractTab[] { programParentTab, histoUserTab, histoGroupTab, diagnosisTab, materialTab,
				stainingTab, staticListTab, favouriteListTab, personParentTab, physicianSettingsTab, organizationTab,
				logTab };
	}

	public void initAndPrepareBean() {
		initBean("");
		prepareDialog();
	}

	public void initAndPrepareBean(String tabName) {
		if (initBean(tabName))
			prepareDialog();
	}

	public boolean initBean(String tabName) {
		super.initBean(null, Dialog.SETTINGS);

		AbstractTab foundTab = null;

		for (int i = 0; i < tabs.length; i++) {
			tabs[i].initTab();
			if (tabs[i].getTabName().equals(tabName))
				foundTab = tabs[i];
		}

		if (tabName == null || foundTab == null)
			onTabChange(tabs[1]);
		else {
			onTabChange(foundTab);
		}

		return true;
	}

	public class ProgramParentTab extends AbstractTab {
		public ProgramParentTab() {
			setTabName("ProgrammParentTab");
			setName("dialog.settings.programmParent");
			setViewID("programmParentTab");
			setDisabled(true);
		}

	}

	@Getter
	@Setter
	public class HistoUserTab extends AbstractTab {

		private List<HistoUser> users;

		private List<HistoGroup> groups;

		private DefaultTransformer<HistoGroup> groupTransformer;

		private boolean showArchived;

		public HistoUserTab() {
			setTabName("HistoUserTab");
			setName("dialog.settings.user");
			setViewID("histoUser");
			setCenterInclude("include/userList.xhtml");
			setParentTab(programParentTab);
		}

		public boolean initTab() {
			setShowArchived(false);
			return true;
		}

		public void updateData() {
			setUsers(userDao.list(!showArchived));
			setGroups(groupDao.list(true));
			setGroupTransformer(new DefaultTransformer<HistoGroup>(getGroups()));
		}

		public void onChangeUserGroup(HistoUser histoUser) {
			try {
				userHandlerAction.groupOfUserHasChanged(histoUser);
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void addHistoUser(Physician physician) {
			try {
				if (physician != null) {
					userService.addUser(physician);
					updateData();
				}
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

	}

	@Getter
	@Setter
	public class HistoGroupTab extends AbstractTab {

		private List<HistoGroup> groups;

		private boolean showArchived;

		public HistoGroupTab() {
			setTabName("HistoGroupTab");
			setName("dialog.settings.group");
			setViewID("histoGroupTab");
			setCenterInclude("include/groupList.xhtml");
			setParentTab(programParentTab);
		}

		public boolean initTab() {
			setShowArchived(false);
			return true;
		}

		public void updateData() {
			setGroups(groupDao.list(!showArchived));
		}

	}

	public enum DiagnosisPage {
		LIST, EDIT, EDIT_TEXT_TEMPLATE;
	}

	@Getter
	@Setter
	public class DiagnosisTab extends AbstractTab {

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

					genericDAO.save(getSelectedDiagnosisPreset(), resourceBundle.get("log.settings.diagnosis.new",
							getSelectedDiagnosisPreset().getCategory()));

					ListOrder.reOrderList(getDiagnosisPresets());

					genericDAO.saveCollection(getDiagnosisPresets(), "log.settings.diagnosis.list.reoder");

				} else {

					// case edit: update an save
					logger.debug("Updating diagnosis " + getSelectedDiagnosisPreset().getCategory());

					genericDAO.save(getSelectedDiagnosisPreset(), resourceBundle.get("log.settings.diagnosis.update",
							getSelectedDiagnosisPreset().getCategory()));
				}

				discardDiagnosisPreset();

			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Discards all changes of a diagnosisPrototype
		 */
		public void discardDiagnosisPreset() {
			if (getSelectedDiagnosisPreset().getId() != 0)
				genericDAO.refresh(getSelectedDiagnosisPreset());
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

				genericDAO.saveCollection(getDiagnosisPresets(), "log.settings.diagnosis.list.reoder");

			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterInclude() {
			switch (getPage()) {
			case EDIT:
				return "include/diagnosisEdit.xhtml";
			case EDIT_TEXT_TEMPLATE:
				return "include/diagnosisEditTemplate.xhtml";
			default:
				return "include/diagnosisList.xhtml";
			}
		}
	}

	public enum MaterialPage {
		LIST, EDIT, ADD_STAINING;
	}

	@Getter
	@Setter
	public class MaterialTab extends AbstractTab {

		public MaterialPage page;

		private List<MaterialPreset> allMaterialList;

		/**
		 * StainingPrototype for creating and editing
		 */
		private MaterialPreset editMaterial;

		/**
		 * List for selecting staining, this list contains all stainings. They can be
		 * choosen and added to the material
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

					genericDAO.save(getEditMaterial(),
							resourceBundle.get("log.settings.material.new", getEditMaterial().getName()));

					ListOrder.reOrderList(getAllMaterialList());

					genericDAO.saveCollection(getAllMaterialList(), "log.settings.material.list.reoder");

				} else {
					logger.debug("Updating Material " + getEditMaterial().getName());
					// case edit: update an save
					genericDAO.save(getEditMaterial(),
							resourceBundle.get("log.settings.material.update", getEditMaterial().getName()));
				}
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * discards all changes for the stainingList
		 */
		public void discardMaterial() {
			if (getEditMaterial().getId() != 0)
				genericDAO.refresh(getEditMaterial());
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

				genericDAO.saveCollection(getAllMaterialList(), "log.settings.staining.list.reoder");

			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterInclude() {
			switch (getPage()) {
			case EDIT:
				return "include/materialEdit.xhtml";
			case ADD_STAINING:
				return "include/materialAddStaining.xhtml";
			default:
				return "include/materialList.xhtml";
			}
		}
	}

	@Getter
	@Setter
	public class StainingTab extends AbstractTab {

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
			setCenterInclude("include/stainingsList.xhtml");
		}

		@Override
		public void updateData() {
			setAllStainingsList(utilDAO.getAllStainingPrototypes());
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
				genericDAO.saveCollection(getAllStainingsList(), "log.settings.staining.list.reoder");

			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

	}

	public enum StaticListPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class StaticListTab extends AbstractTab {

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

					genericDAO.save(getEditListItem(), resourceBundle.get("log.settings.staticList.new",
							getEditListItem().getValue(), getSelectedStaticList().toString()));

					ListOrder.reOrderList(getStaticListContent());

					genericDAO.saveCollection(getStaticListContent(), "log.settings.staticList.list.reoder",
							new Object[] { getSelectedStaticList().toString() });
				} else {
					logger.debug("Updating ListItem " + getEditListItem().getValue());
					// case edit: update an save

					genericDAO.save(getEditListItem(), resourceBundle.get("log.settings.staticList.update",
							getEditListItem().getValue(), getSelectedStaticList().toString()));
				}

			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardListItem() {
			if (getEditListItem().getId() != 0)
				genericDAO.refresh(getEditListItem());

			setEditListItem(null);
			setPage(StaticListPage.LIST);

			updateData();
		}

		public void archiveListItem(ListItem item, boolean archive) {
			try {
				item.setArchived(archive);
				if (archive) {
					genericDAO.save(item, resourceBundle.get("log.settings.staticList.archive", item.getValue(),
							getSelectedStaticList().toString()));
				} else {
					genericDAO.save(item, resourceBundle.get("log.settings.staticList.dearchive", item.getValue(),
							getSelectedStaticList().toString()));
				}

				// removing item from current list
				getStaticListContent().remove(item);
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void onReorderList(ReorderEvent event) {
			try {
				ListOrder.reOrderList(getStaticListContent());

				genericDAO.saveCollection(getStaticListContent(), "log.settings.staticList.list.reoder",
						new Object[] { getSelectedStaticList().toString() });
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterInclude() {
			switch (getPage()) {
			case EDIT:
				return "include/staticListsEdit.xhtml";
			default:
				return "include/staticLists.xhtml";
			}
		}

	}

	@Getter
	@Setter
	public class PersonParentTab extends AbstractTab {
		public PersonParentTab() {
			setTabName("PersonParentTab");
			setName("dialog.settings.personParent");
			setViewID("personParentTab");
			setCenterInclude("include/physicianList.xhtml");
			setDisabled(true);
		}
	}

	@Getter
	@Setter
	public class PhysicianSettingsTab extends AbstractTab {

		/**
		 * True if archived physicians should be display
		 */
		private boolean showArchived;

		/**
		 * List containing all physicians known in the histo database
		 */
		private List<Physician> physicianList;

		/**
		 * All roles to display
		 */
		private List<ContactRole> allRoles;

		/**
		 * Array of roles for that physicians should be shown.
		 */
		private ContactRole[] showPhysicianRoles;

		public PhysicianSettingsTab() {
			setTabName("PhysicianSettingsTab");
			setName("dialog.settings.persons");
			setViewID("persons");
			setCenterInclude("include/physicianList.xhtml");
			setParentTab(personParentTab);

			setShowArchived(false);
			setAllRoles(Arrays.asList(ContactRole.values()));

			setShowPhysicianRoles(ContactRole.values());
		}

		@Override
		public boolean initTab() {
			setShowArchived(false);
			return true;
		}

		@Override
		public void updateData() {
			setPhysicianList(physicianDao.list(getShowPhysicianRoles(), !showArchived));
		}

		public void addPhysician(Physician physician) {
			try {
				if (physician != null) {
					physicianService.addOrMergePhysician(physician);
				}
			} catch (HistoDatabaseInconsistentVersionException e) {
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
				physicianService.archivePhysician(physician, archive);
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Updates the data of the physician with data from the clinic backend
		 */
		public void updateDataFromLdap(Physician physician) {
			try {
				physicianService.ldapUpdate(physician);
			} catch (HistoDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}
	}

	@Getter
	@Setter
	public class OrganizationTab extends AbstractTab {

		private List<Organization> organizations;

		private Organization selectedOrganization;

		private boolean showArchived;

		public OrganizationTab() {
			setTabName("OrganizationTab");
			setName("dialog.settings.organization");
			setViewID("organizations");
			setCenterInclude("include/organizationLists.xhtml");
			setParentTab(personParentTab);
		}

		@Override
		public boolean initTab() {
			setShowArchived(false);
			return true;
		}

		@Override
		public void updateData() {
			setOrganizations(organizationDao.list(!showArchived));
		}

	}

	@Getter
	@Setter
	public class FavouriteListTab extends AbstractTab {

		/**
		 * Array containing all favourite listis
		 */
		private List<FavouriteList> favouriteLists;

		public FavouriteListTab() {
			setTabName("FavouriteListTab");
			setName("dialog.settings.favouriteList");
			setViewID("favouriteLists");
			setCenterInclude("include/favouriteList.xhtml");
		}

		@Override
		public void updateData() {
			setFavouriteLists(favouriteListDAO.getAllFavouriteLists(true));

		}

	}

	@Getter
	@Setter
	public class LogTab extends AbstractTab {

		private int logsPerPull;

		private int selectedLogPage;

		private List<Log> logs;

		private int maxLogPages;

		public LogTab() {
			setTabName("LogTab");
			setName("dialog.settings.log");
			setViewID("logs");
			setCenterInclude("include/log.xhtml");

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

	}

	public class AdminTab extends AbstractTab {

		public AdminTab() {
			setTabName("AdminTab");
			setName("dialog.settings.admin");
			setViewID("admin");
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub

		}

	}
}
