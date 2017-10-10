package org.histo.action.handler;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.cups4j.CupsClient;
import org.cups4j.CupsPrinter;
import org.histo.config.enums.Role;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.histo.settings.ClinicJsonHandler;
import org.histo.settings.DefaultNotificationSettings;
import org.histo.settings.LdapHandler;
import org.histo.settings.PrinterSettings;
import org.histo.settings.ProgramSettings;
import org.histo.settings.Version;
import org.histo.settings.VersionContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.StreamUtils;
import org.histo.util.fax.FaxHandler;
import org.histo.util.interfaces.FileHandlerUtil;
import org.histo.util.mail.MailHandler;
import org.histo.util.printer.ClinicPrinter;
import org.histo.util.printer.ClinicPrinterDummy;
import org.histo.util.printer.LabelPrinter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "singleton")
@Getter
@Setter
public class GlobalSettings {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final String HISTO_BASE_URL = "/Histo2016";
	public static final String HISTO_LOGIN_PAGE = "/login.xhtml";

	public static final String PROGRAM_SETTINGS = "classpath:settings/general.json";
	public static final String SETTINGS_GENERAL = "generalSettings";
	public static final String SETTINGS_DEFAULT_NOTIFICATION = "defaultNotification";
	public static final String SETTINGS_LDAP = "ldapSettings";
	public static final String SETTINGS_MAIL = "mail";
	public static final String SETTINGS_FAX = "fax";
	public static final String SETTINGS_CUPS_SERVER = "cupsServer";
	public static final String SETTINGS_LABLE_PRINTERS = "labelPrinters";
	public static final String SETTINGS_CLINIC_BACKEND = "clinicBackend";
	public static final String SETTINGS_PREDEFINED_ROLES = "predefinedRoles";

	public static final String VERSIONS_INFO = "classpath:settings/version.json";
	public static final String MAIL_TEMPLATES = "classpath:settings/mailTemplates.json";
	public static final String PRINT_TEMPLATES = "classpath:settings/printTempaltes.json";
	/**
	 * Default program settings
	 */
	private ProgramSettings programSettings;

	/**
	 * Printer settings
	 */
	private PrinterSettings printerSettings;

	/**
	 * List with default notification options for contact roles
	 */
	private DefaultNotificationSettings defaultNotificationSettings;

	/**
	 * List of predefined role settings
	 */
	private List<PredefinedRoleSettings> predefinedRoleSettings;

	/**
	 * List of clinicla pritners
	 */
	private List<ClinicPrinter> printerList;

	/**
	 * Transformer for printerList
	 */
	private DefaultTransformer<ClinicPrinter> printerListTransformer;

	/**
	 * List of labelprinters
	 */
	private List<LabelPrinter> labelPrinterList;

	/**
	 * Transformer for labelprinters
	 */
	private DefaultTransformer<LabelPrinter> labelPrinterListTransformer;

	/**
	 * Object for handling mails
	 */
	private MailHandler mailHandler;

	/**
	 * Object for handling mails
	 */
	private FaxHandler faxHandler;

	/**
	 * Object for handeling ldap connections
	 */
	private LdapHandler ldapHandler;

	/**
	 * Handler for json request to daniel's clinic backend
	 */
	private ClinicJsonHandler clinicJsonHandler;

	/**
	 * The current version of the program
	 */
	private String currentVersion;

	/**
	 * Container for providing version information
	 */
	private VersionContainer versionContainer;

	
	@PostConstruct
	public void initBean() {
		Gson gson = new Gson();

		JsonParser parser = new JsonParser();
		JsonObject o = parser.parse(FileHandlerUtil.getContentOfFile(PROGRAM_SETTINGS)).getAsJsonObject();

		programSettings = gson.fromJson(o.get(SETTINGS_GENERAL), ProgramSettings.class);

		defaultNotificationSettings = gson.fromJson(o.get(SETTINGS_DEFAULT_NOTIFICATION),
				DefaultNotificationSettings.class);

		mailHandler = gson.fromJson(o.get(SETTINGS_MAIL), MailHandler.class);

		faxHandler = gson.fromJson(o.get(SETTINGS_FAX), FaxHandler.class);

		printerSettings = gson.fromJson(o.get(SETTINGS_CUPS_SERVER), PrinterSettings.class);

		setPrinterList(loadCupsPrinters(printerSettings));

		setPrinterListTransformer(new DefaultTransformer<ClinicPrinter>(getPrinterList()));

		Type listType = new TypeToken<ArrayList<LabelPrinter>>() {
		}.getType();
		setLabelPrinterList(gson.fromJson(o.get(SETTINGS_LABLE_PRINTERS), listType));
		setLabelPrinterListTransformer(new DefaultTransformer<LabelPrinter>(getLabelPrinterList()));

		ldapHandler = gson.fromJson(o.get(SETTINGS_LDAP), LdapHandler.class);

		clinicJsonHandler = gson.fromJson(o.get(SETTINGS_CLINIC_BACKEND), ClinicJsonHandler.class);

		listType = new TypeToken<ArrayList<PredefinedRoleSettings>>() {
		}.getType();
		setPredefinedRoleSettings(gson.fromJson(o.get(SETTINGS_PREDEFINED_ROLES), listType));

		Version[] versions = Version.factroy(VERSIONS_INFO);
		// setting current version
		if (versions != null && versions.length > 0) {
			setCurrentVersion(versions[0].getVersion());
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

	public PredefinedRoleSettings getRoleSettingsForRole(Role role) {
		try {
			return getPredefinedRoleSettings().stream().filter(p -> p.getRole() == role)
					.collect(StreamUtils.singletonCollector());
		} catch (IllegalStateException e) {
			// settings not found returning empty one
			return new PredefinedRoleSettings();
		}
	}

	public ClinicPrinter getPrinterByName(String name) {
		for (ClinicPrinter clinicPrinter : getPrinterList()) {
			if (clinicPrinter.getName().equals(name))
				return clinicPrinter;
		}
		return null;
	}

	public LabelPrinter getLabelPrinterByID(String id) {

		for (LabelPrinter labelPrinter : getLabelPrinterList()) {
			if (labelPrinter.getId() == Long.valueOf(id)) {
				return labelPrinter;
			}
		}
		return null;
	}
}