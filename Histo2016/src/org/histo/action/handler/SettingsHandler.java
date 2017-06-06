package org.histo.action.handler;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.histo.action.UserHandlerAction;
import org.histo.config.enums.Role;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.histo.settings.ClinicJsonHandler;
import org.histo.settings.LdapHandler;
import org.histo.settings.PrinterSettings;
import org.histo.settings.ProgramSettings;
import org.histo.settings.Version;
import org.histo.settings.VersionContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.StreamUtils;
import org.histo.util.interfaces.FileHandlerUtil;
import org.histo.util.printer.ClinicPrinter;
import org.histo.util.printer.ClinicPrinterDummy;
import org.histo.util.printer.LabelPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

@Component
@Scope(value = "session")
public class SettingsHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final String PROGRAM_SETTINGS = "classpath:settings/general.json";
	public static final String PRINTER_SETTINGS = "classpath:settings/cupsServer.json";
	public static final String LABEL_PRINTER_SETTINGS = "classpath:settings/labelPrinter.json";
	public static final String LDAP_SETTINGS = "classpath:settings/ldap.json";
	public static final String VERSION_SETTINGS = "classpath:settings/version.json";
	public static final String CLINIC_BACKEND_SETTINGS = "classpath:settings/clinicBackend.json";
	public static final String PREDEFINED_ROLE_SETTINGS = "classpath:settings/predefinedRoleSettings.json";
	public static final String VERSIONS_INFO = "classpath:settings/version.json";

	@Autowired
	private UserHandlerAction userHandlerAction;

	private ProgramSettings programSettings;
	private PrinterSettings printerSettings;

	/**
	 * The current version of the program
	 */
	private String currentVersion;

	/**
	 * Selected ClinicPrinter to print the document
	 */
	private ClinicPrinter selectedPrinter;

	/**
	 * List of clinicla pritners
	 */
	private List<ClinicPrinter> printerList;

	/**
	 * Transformer for printerList
	 */
	private DefaultTransformer<ClinicPrinter> printerListTransformer;

	/**
	 * Selected label pirnter
	 */
	private LabelPrinter selectedLabelPrinter;

	/**
	 * List of labelprinters
	 */
	private List<LabelPrinter> labelPrinterList;

	/**
	 * Transformer for labelprinters
	 */
	private DefaultTransformer<LabelPrinter> labelPrinterListTransformer;

	/**
	 * Object for handeling ldap connections
	 */
	private LdapHandler ldapHandler;

	/**
	 * Container for providing version information
	 */
	private VersionContainer versionContainer;

	/**
	 * Handler for json request to daniel's clinic backend
	 */
	private ClinicJsonHandler clinicJsonHandler;

	/**
	 * List of predefined role settings
	 */
	private List<PredefinedRoleSettings> predefinedRoleSettings;

	public void initBean() {
		Gson gson = new Gson();

		logger.debug("Loading general settings");
		programSettings = gson.fromJson(FileHandlerUtil.getContentOfFile(PROGRAM_SETTINGS), ProgramSettings.class);

		logger.debug("Current Version");
		Version[] versions = Version.factroy(SettingsHandler.VERSIONS_INFO);
		// setting current version
		if (versions != null && versions.length > 0) {
			setCurrentVersion(versions[0].getVersion());
		}

		logger.debug("Loading CUPS Printers");
		printerSettings = gson.fromJson(FileHandlerUtil.getContentOfFile(PRINTER_SETTINGS), PrinterSettings.class);

		setPrinterList(loadCupsPrinters(printerSettings));
		setPrinterListTransformer(new DefaultTransformer<ClinicPrinter>(getPrinterList()));

		logger.debug("Loading Lable Printers");
		Type listType = new TypeToken<ArrayList<LabelPrinter>>() {
		}.getType();
		setLabelPrinterList(gson.fromJson(FileHandlerUtil.getContentOfFile(LABEL_PRINTER_SETTINGS), listType));
		setLabelPrinterListTransformer(new DefaultTransformer<LabelPrinter>(getLabelPrinterList()));

		updateSelectedPrinters();

		logger.debug("Loading LDAP Handler");
		ldapHandler = gson.fromJson(FileHandlerUtil.getContentOfFile(LDAP_SETTINGS), LdapHandler.class);

		logger.debug("Loading clinic backend handler");
		clinicJsonHandler = gson.fromJson(FileHandlerUtil.getContentOfFile(CLINIC_BACKEND_SETTINGS),
				ClinicJsonHandler.class);

		logger.debug("Loading predefinde role settings");
		listType = new TypeToken<ArrayList<PredefinedRoleSettings>>() {
		}.getType();
		setPredefinedRoleSettings(gson.fromJson(FileHandlerUtil.getContentOfFile(PREDEFINED_ROLE_SETTINGS), listType));
	}

	public void updateSelectedPrinters() {

		if (userHandlerAction.getCurrentUser().getPreferedPrinter() == null) {
			// dummy printer is allways there
			setSelectedPrinter(getPrinterList().get(0));
			userHandlerAction.getCurrentUser().setPreferedPrinter(getSelectedPrinter().getName());
		} else {
			ClinicPrinter printer = getPrinterByName(userHandlerAction.getCurrentUser().getPreferedPrinter());
			// if printer was found set it
			if (printer != null) {
				logger.debug("Settings printer " + printer.getName() + " as selected printer");
				setSelectedPrinter(printer);
			} else {
				// TODO search for printer in the same room
				setSelectedPrinter(getPrinterList().get(0));
			}
		}

		if (userHandlerAction.getCurrentUser().getPreferedLabelPritner() == null) {
			setSelectedLabelPrinter(getLabelPrinterList().get(0));
			userHandlerAction.getCurrentUser()
					.setPreferedLabelPritner(Long.toString(getSelectedLabelPrinter().getId()));
		} else {
			LabelPrinter labelPrinter = getLabelPrinterByID(
					userHandlerAction.getCurrentUser().getPreferedLabelPritner());

			if (labelPrinter != null) {
				logger.debug("Settings printer " + labelPrinter.getName() + " as selected printer");
				setSelectedLabelPrinter(labelPrinter);
			} else {
				// TODO serach for pritner in the same room
				setSelectedLabelPrinter(getLabelPrinterList().get(0));
			}
		}
	}

	private List<ClinicPrinter> loadCupsPrinters(PrinterSettings settings) {
		ArrayList<ClinicPrinter> result = new ArrayList<>();
		CupsClient cupsClient;

		if (!programSettings.isOffline()) {
			try {
				cupsClient = new CupsClient(settings.getCupsHost(), settings.getCupsPost());
				List<CupsPrinter> cupsPrinter = cupsClient.getPrinters();
				int i = 0;
				// transformin into clinicprinters
				for (CupsPrinter p : cupsPrinter) {
					result.add(new ClinicPrinter(i, p, settings));
					i++;
				}

			} catch (Exception e) {
				logger.error("Retriving printers failed" + e);
			}
		}

		if (result.size() == 0)
			result.add(new ClinicPrinterDummy(0));

		return result;
	}

	private ClinicPrinter getPrinterByName(String name) {
		for (ClinicPrinter clinicPrinter : getPrinterList()) {
			if (clinicPrinter.getName().equals(name))
				return clinicPrinter;
		}
		return null;
	}

	private LabelPrinter getLabelPrinterByID(String id) {

		for (LabelPrinter labelPrinter : getLabelPrinterList()) {
			if (labelPrinter.getId() == Long.valueOf(id)) {
				return labelPrinter;
			}
		}
		return null;
	}

	public PredefinedRoleSettings getRoleSettingsForRole(Role role) {
		try {
			return getPredefinedRoleSettings().stream().filter(p -> p.getRole() == role)
					.collect(StreamUtils.singletonCollector());
		} catch (IllegalStateException e) {
			// settings not found returning empty one
			return new PredefinedRoleSettings();
		}
	}

	// ************************ Getter/Setter ************************
	public ClinicPrinter getSelectedPrinter() {
		return selectedPrinter;
	}

	public void setSelectedPrinter(ClinicPrinter selectedPrinter) {
		this.selectedPrinter = selectedPrinter;
	}

	public List<ClinicPrinter> getPrinterList() {
		return printerList;
	}

	public void setPrinterList(List<ClinicPrinter> printerList) {
		this.printerList = printerList;
	}

	public DefaultTransformer<ClinicPrinter> getPrinterListTransformer() {
		return printerListTransformer;
	}

	public void setPrinterListTransformer(DefaultTransformer<ClinicPrinter> printerListTransformer) {
		this.printerListTransformer = printerListTransformer;
	}

	public LabelPrinter getSelectedLabelPrinter() {
		return selectedLabelPrinter;
	}

	public void setSelectedLabelPrinter(LabelPrinter selectedLabelPrinter) {
		this.selectedLabelPrinter = selectedLabelPrinter;
	}

	public List<LabelPrinter> getLabelPrinterList() {
		return labelPrinterList;
	}

	public void setLabelPrinterList(List<LabelPrinter> labelPrinterList) {
		this.labelPrinterList = labelPrinterList;
	}

	public DefaultTransformer<LabelPrinter> getLabelPrinterListTransformer() {
		return labelPrinterListTransformer;
	}

	public void setLabelPrinterListTransformer(DefaultTransformer<LabelPrinter> labelPrinterListTransformer) {
		this.labelPrinterListTransformer = labelPrinterListTransformer;
	}

	public LdapHandler getLdapHandler() {
		return ldapHandler;
	}

	public void setLdapHandler(LdapHandler ldapHandler) {
		this.ldapHandler = ldapHandler;
	}

	public VersionContainer getVersionContainer() {
		return versionContainer;
	}

	public void setVersionContainer(VersionContainer versionContainer) {
		this.versionContainer = versionContainer;
	}

	public ClinicJsonHandler getClinicJsonHandler() {
		return clinicJsonHandler;
	}

	public void setClinicJsonHandler(ClinicJsonHandler clinicJsonHandler) {
		this.clinicJsonHandler = clinicJsonHandler;
	}

	public List<PredefinedRoleSettings> getPredefinedRoleSettings() {
		return predefinedRoleSettings;
	}

	public void setPredefinedRoleSettings(List<PredefinedRoleSettings> predefinedRoleSettings) {
		this.predefinedRoleSettings = predefinedRoleSettings;
	}

	public String getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(String currentVersion) {
		this.currentVersion = currentVersion;
	}

	public ProgramSettings getProgramSettings() {
		return programSettings;
	}

	public void setProgramSettings(ProgramSettings programSettings) {
		this.programSettings = programSettings;
	}
	
}
