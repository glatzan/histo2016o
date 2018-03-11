package org.histo.action.handler;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.adaptors.ClinicJsonHandler;
import org.histo.adaptors.FaxHandler;
import org.histo.adaptors.LdapHandler;
import org.histo.adaptors.MailHandler;
import org.histo.adaptors.printer.ClinicPrinter;
import org.histo.adaptors.printer.CupsPrinterLoader;
import org.histo.adaptors.printer.LabelPrinter;
import org.histo.adaptors.printer.PrinterForRoomHandler;
import org.histo.model.transitory.settings.DefaultDocuments;
import org.histo.model.transitory.settings.DefaultNotificationSettings;
import org.histo.model.transitory.settings.PrinterSettings;
import org.histo.model.transitory.settings.ProgramSettings;
import org.histo.model.transitory.settings.Version;
import org.histo.model.transitory.settings.VersionContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.FileUtil;
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
	public static final String SETTINGS_DEFAULT_DOCUMENTS = "defaultDocuments";
	public static final String SETTINGS_DEFAULT_NOTIFICATION = "defaultNotification";
	public static final String SETTINGS_LDAP = "ldapSettings";
	public static final String SETTINGS_MAIL = "mail";
	public static final String SETTINGS_FAX = "fax";
	public static final String SETTINGS_CUPS_SERVER = "cupsServer";
	public static final String SETTINGS_LABLE_PRINTERS = "labelPrinters";
	public static final String SETTINGS_CLINIC_BACKEND = "clinicBackend";
	public static final String SETTINGS_PRINTER_FOR_ROOM = "findPrinterForRoomBackend";

	public static final String VERSIONS_INFO = "classpath:settings/version.txt";
	public static final String MAIL_TEMPLATES = "classpath:settings/mailTemplates.json";
	public static final String PRINT_TEMPLATES = "classpath:settings/printTempaltes.json";
	/**
	 * Default program settings
	 */
	private ProgramSettings programSettings;

	/**
	 * List of default documents
	 */
	private DefaultDocuments defaultDocuments;

	/**
	 * Printer settings
	 */
	private PrinterSettings printerSettings;

	/**
	 * List with default notification options for contact roles
	 */
	private DefaultNotificationSettings defaultNotificationSettings;

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
	 * Gets a pritner ip for the room from which the page was loaded
	 */
	private PrinterForRoomHandler printerForRoomHandler;

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
		JsonObject o = parser.parse(FileUtil.getContentOfFile(PROGRAM_SETTINGS)).getAsJsonObject();

		programSettings = gson.fromJson(o.get(SETTINGS_GENERAL), ProgramSettings.class);

		defaultNotificationSettings = gson.fromJson(o.get(SETTINGS_DEFAULT_NOTIFICATION),
				DefaultNotificationSettings.class);

		mailHandler = gson.fromJson(o.get(SETTINGS_MAIL), MailHandler.class);

		faxHandler = gson.fromJson(o.get(SETTINGS_FAX), FaxHandler.class);

		printerSettings = gson.fromJson(o.get(SETTINGS_CUPS_SERVER), PrinterSettings.class);

		defaultDocuments = gson.fromJson(o.get(SETTINGS_DEFAULT_DOCUMENTS), DefaultDocuments.class);

		setPrinterList(new CupsPrinterLoader().loadCupsPrinters(printerSettings));

		setPrinterListTransformer(new DefaultTransformer<ClinicPrinter>(getPrinterList()));

		Type listType = new TypeToken<ArrayList<LabelPrinter>>() {
		}.getType();
		setLabelPrinterList(gson.fromJson(o.get(SETTINGS_LABLE_PRINTERS), listType));
		setLabelPrinterListTransformer(new DefaultTransformer<LabelPrinter>(getLabelPrinterList()));

		ldapHandler = gson.fromJson(o.get(SETTINGS_LDAP), LdapHandler.class);

		clinicJsonHandler = gson.fromJson(o.get(SETTINGS_CLINIC_BACKEND), ClinicJsonHandler.class);

		printerForRoomHandler = gson.fromJson(o.get(SETTINGS_PRINTER_FOR_ROOM), PrinterForRoomHandler.class);
		
		List<Version> versions = Version.factroy(VERSIONS_INFO);
		// setting current version
		if (versions != null && versions.size() > 0) {
			setCurrentVersion(versions.get(0).getVersion());
		}

	}

	/**
	 * Searches the loaded clinical printers for the given printer. If found the
	 * printer from the global list will be returned. This will prevent problems if
	 * something has changed.
	 * 
	 * If no printer was found the first printer will be returned (DummyPrinter)
	 * 
	 * @param clinicPrinter
	 * @return
	 */
	public ClinicPrinter isPrinterValid(long id) {
		for (ClinicPrinter printer : getPrinterList()) {
			if (printer.getId() == id)
				return printer;
		}

		return getPrinterList().get(0);
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
