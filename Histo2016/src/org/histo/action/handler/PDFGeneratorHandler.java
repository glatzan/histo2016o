package org.histo.action.handler;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.Gender;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.Signature;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.printing.PrintTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.nixosoft.jlr.JLRConverter;
import de.nixosoft.jlr.JLRGenerator;

@Component
@Scope(value = "session")
public class PDFGeneratorHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Lazy
	private ResourceBundle resourceBundle;

	@Autowired
	@Lazy
	private MainHandlerAction mainHandlerAction;

	public PDFContainer generatePDFForReport(Patient patient, Task task, PrintTemplate printTemplate) {
		return generatePDFForReport(patient, task, printTemplate, null);
	}

	public PDFContainer generateSendReport(PrintTemplate printTemplate, Patient patient) {
		PDFGenerator generator = new PDFGenerator(printTemplate);
		
		return generator.generatePDF();
	}

	public PDFContainer generatePDFForReport(Patient patient, Task task, PrintTemplate printTemplate,
			Person toSendAddress) {

		File workingDirectory = new File(
				HistoSettings.getAbsolutePath(mainHandlerAction.getSettings().getWorkingDirectory()));

		File output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

		logger.debug("TemplateUtil File: " + HistoSettings.getAbsolutePath(printTemplate.getFile()));

		// loading tex file
		File template = new File(HistoSettings.getAbsolutePath(printTemplate.getFile()));

		File processedTex = new File(workingDirectory.getAbsolutePath() + File.separator + "tmp.tex");

		JLRConverter converter = new JLRConverter(workingDirectory);

		replacePatientData(converter, patient);

		replaceAddressData(converter, toSendAddress);

		if (printTemplate.getDocumentTyp() == DocumentType.U_REPORT
				|| printTemplate.getDocumentTyp() == DocumentType.U_REPORT_EMTY) {
			replaceUReportData(converter, task);
		}

		if (printTemplate.getDocumentTyp() == DocumentType.DIAGNOSIS_REPORT
				|| printTemplate.getDocumentTyp() == DocumentType.DIAGNOSIS_REPORT_EXTERN) {
			replaceReportData(converter, task);
			replaceSignature(converter, task);
		}

		try {

			if (!converter.parse(template, processedTex)) {
				logger.error(converter.getErrorMessage());
			}

			JLRGenerator pdfGen = new JLRGenerator();

			if (!pdfGen.generate(processedTex, output, workingDirectory)) {
				logger.error(pdfGen.getErrorMessage());
				return null;
			}

			File test = pdfGen.getPDF();
			byte[] data = readContentIntoByteArray(test);

			return new PDFContainer(printTemplate.getDocumentTyp(),
					"_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf", data);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

		// ArrayList<ArrayList<String>> services = new
		// ArrayList<ArrayList<String>>();
		//
		// ArrayList<String> subservice1 = new ArrayList<String>();
		// ArrayList<String> subservice2 = new ArrayList<String>();
		// ArrayList<String> subservice3 = new ArrayList<String>();
		//
		// subservice1.add("Software");
		// subservice1.add("50");
		// subservice2.add("Hardware1");
		// subservice2.add("500");
		// subservice3.add("Hardware2");
		// subservice3.add("850");
		//
		// services.add(subservice1);
		// services.add(subservice2);
		// services.add(subservice3);
		//
		// converter.replace("services", services);
		//
		// System.out.println(converter.parse(template, invoice1));
		//
		// converter.replace("Number", "2");
		// converter.replace("CustomerName", "Mike Mueller");
		// converter.replace("CustomerStreet", "Prenzlauer Berg 12");
		// converter.replace("CustomerZip", "10405 Berlin");
		//
		// services = new ArrayList<ArrayList<String>>();
		//
		// subservice1 = new ArrayList<String>();
		// subservice2 = new ArrayList<String>();
		// subservice3 = new ArrayList<String>();
		//
		// subservice1.add("Software");
		// subservice1.add("150");
		// subservice2.add("Hardware");
		// subservice2.add("500");
		// subservice3.add("Test");
		// subservice3.add("350");
		//
		// services.add(subservice1);
		// services.add(subservice2);
		// services.add(subservice3);
		//
		// converter.replace("services", services);
		//
		// converter.parse(template, invoice2);
		//
		// if (!converter.parse(template, invoice2)) {
		// System.out.println(converter.getErrorMessage());
		// }

	}

	public final void replacePatientData(JLRConverter converter, Patient patient) {
		converter.replace("patName", patient.getPerson().getName());
		converter.replace("patSurName", patient.getPerson().getSurname());
		converter.replace("patAddress", patient.getPerson().getStreet());
		converter.replace("patPlz", patient.getPerson().getPostcode());
		converter.replace("patCity", patient.getPerson().getTown());
		converter.replace("piz", patient.getPiz());
	}

	public final void replaceAddressData(JLRConverter converter, Person person) {
		if (person != null) {
			logger.debug("Replacing address for " + person.getFullName());
			// name +
			converter.replace("addName",
					(person.getGender() == Gender.FEMALE ? resourceBundle.get("pdf.address.female")
							: resourceBundle.get("pdf.address.male")) + " "
							+ (!person.getTitle().isEmpty() ? (person.getTitle() + " ") : "") + person.getName());
			converter.replace("addSurName", person.getSurname());
			converter.replace("addAddress", person.getStreet());
			converter.replace("addPlz", person.getPostcode());
			converter.replace("addCity", person.getTown());
			converter.replace("addSubject", "");
		} else {
			logger.debug("No Address provided");
			converter.replace("addName", resourceBundle.get("pdf.address.none"));
			converter.replace("addSurName", "");
			converter.replace("addAddress", "");
			converter.replace("addPlz", "");
			converter.replace("addCity", "");
			converter.replace("addSubject", "");
		}
	}

	public final void replaceUReportData(JLRConverter converter, Task task) {
		converter.replace("taskNumber", task.getTaskID());
		converter.replace("eDate", mainHandlerAction.date(task.getDateOfReceipt()));
	}

	public final void replaceReportData(JLRConverter converter, Task task) {
		converter.replace("eDate", mainHandlerAction.date(task.getDateOfReceipt()));
		converter.replace("taskNumber", task.getTaskID());
		converter.replace("ward", task.getWard());
		converter.replace("eye", resourceBundle.get("enum.eye." + task.getEye()));
		converter.replace("history", task.getCaseHistory());
		converter.replace("samples", task.getSamples());
		converter.replace("diagnosisRevisions", task.getDiagnosisContainer().getDiagnosisRevisions());

		Contact tmpPhysician = task.getPrimaryContact(ContactRole.SURGEON);
		converter.replace("surgeon", tmpPhysician == null ? "" : tmpPhysician.getPerson().getFullNameAndTitle());
		tmpPhysician = task.getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
		converter.replace("privatePhysician",
				tmpPhysician == null ? "" : tmpPhysician.getPerson().getFullNameAndTitle());
		converter.replace("insurancePrivate", task.getPatient().isPrivateInsurance());

	}

	public final void replaceSignature(JLRConverter converter, Task task) {
		converter.replace("sigantureDate", mainHandlerAction.date(task.getDiagnosisContainer().getSignatureDate()));

		Signature signature = task.getDiagnosisContainer().getSignatureOne();
		if (signature != null && signature.getPhysician() != null) {
			converter.replace("signatureOne", signature.getPhysician().getPerson().getFullNameAndTitle());
			converter.replace("signatureOneRole", signature.getRole());
		}

		signature = task.getDiagnosisContainer().getSignatureTwo();
		if (signature != null && signature.getPhysician() != null) {
			converter.replace("sigantureTwo", signature.getPhysician().getPerson().getFullNameAndTitle());
			converter.replace("signatureTwoRole", signature.getRole());
		}
	}

	/**
	 * Fills a simple PDF with the values given in the hashmap.
	 * 
	 * @param printTemplate
	 * @param replacements
	 * @return
	 */
	public PDFContainer generateSimplePDF(PrintTemplate printTemplate, HashMap<String, String> replacements) {
		return generateSimplePDF(null, printTemplate, replacements);
	}

	/**
	 * Generates a simple PDF using the template an the given hashmap to replace
	 * all datafields. If Patient is given, the patient datafield will be
	 * replace automatically
	 * 
	 * @param printTemplate
	 * @param replacements
	 * @return
	 */
	public PDFContainer generateSimplePDF(Patient patient, PrintTemplate printTemplate,
			HashMap<String, String> replacements) {
		mainHandlerAction.getSettings();
		File workingDirectory = new File(
				HistoSettings.getAbsolutePath(mainHandlerAction.getSettings().getWorkingDirectory()));

		File output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

		logger.debug("TemplateUtil File: " + HistoSettings.getAbsolutePath(printTemplate.getFile()));

		// loading tex file
		File template = new File(HistoSettings.getAbsolutePath(printTemplate.getFile()));

		File processedTex = new File(workingDirectory.getAbsolutePath() + File.separator + "tmp.tex");

		JLRConverter converter = new JLRConverter(workingDirectory);

		if (patient != null)
			replacePatientData(converter, patient);

		for (Map.Entry<String, String> entry : replacements.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();

			converter.replace(key, value);
		}

		try {

			if (!converter.parse(template, processedTex)) {
				logger.error(converter.getErrorMessage());
			}

			JLRGenerator pdfGen = new JLRGenerator();

			if (!pdfGen.generate(processedTex, output, workingDirectory)) {
				logger.error(pdfGen.getErrorMessage());
			}

			File test = pdfGen.getPDF();
			byte[] data = readContentIntoByteArray(test);

			return new PDFContainer(printTemplate.getDocumentTyp(),
					"_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf", data);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public PDFContainer generatePdfForTemplate(Task task, PrintTemplate tempalte, long dateOfReport,
			ContactRole addressPhysicianRole, Physician externalPhysician, Physician signingPhysician) {
		// PDFContainer result = null;
		//
		// switch (template.getType()) {
		// case "COUNCIL":
		// if (task.getCouncil() != null) {
		// result = generatePdf(task, template,
		// task.getCouncil().getDateOfRequest(),
		// task.getCouncil().getCouncilPhysician(),
		// task.getCouncil().getPhysicianRequestingCouncil());
		// }
		// break;
		// case "INTERNAL":
		// result = generatePdf(task, template,
		// task.getReport().getSignatureDate(), null, null);
		// break;
		// default:
		// Physician addressPhysician = null;
		// if (addressPhysicianRole == ContactRole.FAMILY_PHYSICIAN
		// || addressPhysicianRole == ContactRole.PRIVATE_PHYSICIAN) {
		// Contact tmp = task.getPrimaryContact(addressPhysicianRole);
		// addressPhysician = (tmp == null ? null : tmp.getPhysician());
		// } else {
		// addressPhysician = externalPhysician;
		// }
		//
		// result = generatePdf(task, template, dateOfReport, addressPhysician,
		// signingPhysician);
		// break;
		// }
		// TODO: rework
		// return result;
		return null;
	}

	public PDFContainer generatePdf(Task task, PrintTemplate template, long dateOfReport, Physician addressPhysician,
			Physician signingPhysician) {
		return generatePdf(task, template, dateOfReport, addressPhysician, signingPhysician, null);
	}

	public PDFContainer generatePdf(Task task, PrintTemplate template, long dateOfReport, Physician addressPhysician,
			Physician signingPhysician, HashMap<String, String> additionalFields) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfReader pdfReader = getPdfFile(template.getFile());
		PdfStamper pdf = getPdfStamper(pdfReader, out);

		// header
		// populateReportHead(pdf, task, dateOfReport);
		//
		// // address
		// populateReportAddress(pdf, addressPhysician);

		// body
		populateBody(pdf, task, dateOfReport);

		// signature
		if (signingPhysician != null)
			populateSingleSignature(pdf, signingPhysician);

		// barcodes
		// drawBarCodes(task, template, pdfReader, pdf);

		// additional fields for special pdfs
		if (additionalFields != null) {
			for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
				String key = entry.getKey();
				String value = entry.getValue();
				setStamperField(pdf, key, value);
			}
		}

		pdf.setFormFlattening(true);

		closePdf(pdfReader, pdf);

		// String pdfName = (template.isNameAsResources() ?
		// resourceBundle.get(template.getName()) : template.getName())
		// + "_" + task.getPatient().getPiz();

		// return new PDFContainer(template.getType(),
		// pdfName + "_" +
		// mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_")
		// + ".pdf",
		// out.toByteArray());
		return null;
	}

	public final void populateSingleSignature(PdfStamper stamper, Physician physician) {
		setStamperField(stamper, "B_SIGANTURE", physician.getPerson().getFullName());
	}

	public final void populateBody(PdfStamper stamper, Task task, long dateOfReport) {

		// List<Sample> samples = task.getSamples();
		//
		// StringBuffer material = new StringBuffer();
		// StringBuffer diagonsisList = new StringBuffer();
		// StringBuffer reDiagonsisList = new StringBuffer();
		//
		// for (Sample sample : samples) {
		// material.append(sample.getSampleID() + " " + sample.getMaterial() +
		// "\r\n");
		// diagonsisList
		// .append(sample.getSampleID() + " " +
		// sample.getLastRelevantDiagnosis().getDiagnosis() + "\r\n");
		//
		// if (sample.getDiagnosisStatus() ==
		// DiagnosisStatus.RE_DIAGNOSIS_NEEDED)
		// reDiagonsisList.append(sample.getSampleID() + " "
		// + sample.getLastRelevantDiagnosis().getDiagnosisRevisionText() +
		// "\r\n");
		// }
		//
		// setStamperField(stamper, "B_DATE",
		// mainHandlerAction.date(dateOfReport));
		// setStamperField(stamper, "B_Name",
		// task.getParent().getPerson().getName() + ", " +
		// task.getParent().getPerson().getSurname());
		// setStamperField(stamper, "B_Birthday",
		// resourceBundle.get("pdf.birthday") + " "
		// +
		// mainHandlerAction.date(task.getParent().getPerson().getBirthday()));
		//
		// setStamperField(stamper, "B_SAMPLES", material.toString());
		// setStamperField(stamper, "B_EDATE",
		// mainHandlerAction.date(task.getDateOfSugeryAsDate()));
		// setStamperField(stamper, "B_TASK_NUMBER", task.getTaskID());
		// setStamperField(stamper, "B_PIZ", task.getParent().getPiz().isEmpty()
		// ? "" : task.getParent().getPiz());
		//
		// setStamperField(stamper, "B_EYE", resourceBundle.get("enum.eye." +
		// task.getEye().toString()));
		// setStamperField(stamper, "B_HISTORY", task.getCaseHistory());
		// setStamperField(stamper, "B_INSURANCE_NORMAL",
		// task.getPatient().isPrivateInsurance() ? "0" : "1");
		// setStamperField(stamper, "B_INSURANCE_PRIVATE",
		// task.getPatient().isPrivateInsurance() ? "1" : "0");
		// setStamperField(stamper, "B_WARD", task.getWard());
		// setStamperField(stamper, "B_MALIGN", task.isMalign() ? "1" : "0");
		//
		// Contact privatePhysician =
		// task.getPrimaryContact(ContactRole.PRIVATE_PHYSICIAN);
		// Contact surgeon = task.getPrimaryContact(ContactRole.SURGEON);
		//
		// setStamperField(stamper, "B_PRIVATE_PHYSICIAN",
		// privatePhysician == null ? "" :
		// privatePhysician.getPhysician().getPerson().getFullName());
		// setStamperField(stamper, "B_SURGEON", surgeon == null ? "" :
		// surgeon.getPhysician().getPerson().getFullName());
		// setStamperField(stamper, "B_DIAGNOSIS", diagonsisList.toString());
		// setStamperField(stamper, "B_DATE",
		// mainHandlerAction.date(dateOfReport));
		//
		// if (task.getCouncil() != null &&
		// task.getCouncil().getCouncilPhysician() != null) {
		// setStamperField(stamper, "B_COUNCIL",
		// task.getCouncil().getCouncilPhysician().getPerson().getFullName());
		// setStamperField(stamper, "B_TEXT",
		// task.getCouncil().getCouncilText());
		// setStamperField(stamper, "B_APPENDIX",
		// task.getCouncil().getAttachment());
		// }
		//
		// if (task.getDiagnosisStatus() ==
		// DiagnosisStatus.RE_DIAGNOSIS_NEEDED) {
		// setStamperField(stamper, "B_RE_DIAGNOSIS", "1");
		// setStamperField(stamper, "B_RE_DIAGNOSIS_TEXT",
		// reDiagonsisList.toString());
		// }
		//
		// if (task.getReport() != null) {
		//
		// setStamperField(stamper, "B_HISTOLOGICAL_RECORD",
		// task.getReport().getHistologicalRecord());
		//
		// if (task.getReport().getPhysicianToSign().getPhysician() != null) {
		// setStamperField(stamper, "S_PHYSICIAN",
		// task.getReport().getPhysicianToSign().getPhysician().getPerson().getFullName());
		// setStamperField(stamper, "S_PHYSICIAN_ROLE",
		// task.getReport().getPhysicianToSign().getRole());
		// }
		//
		// if (task.getReport().getConsultantToSign().getPhysician() != null) {
		// setStamperField(stamper, "S_CONSULTANT",
		// task.getReport().getConsultantToSign().getPhysician().getPerson().getFullName());
		// setStamperField(stamper, "S_CONSULTANT_ROLE",
		// task.getReport().getConsultantToSign().getRole());
		// }
		// }
		// TODO: rework
	}

	private static byte[] readContentIntoByteArray(File file) {
		FileInputStream fileInputStream = null;
		byte[] bFile = new byte[(int) file.length()];
		try {
			// convert file into array of bytes
			fileInputStream = new FileInputStream(file);
			fileInputStream.read(bFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return bFile;
	}

	/**
	 * Loads a PdfReader from a file
	 * 
	 * @param path
	 * @return
	 */
	public static final PdfReader getPdfFile(String path) {
		PdfReader pdfTemplate = null;
		ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext();

		Resource resource = appContext.getResource(path);

		try {
			pdfTemplate = new PdfReader(resource.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return pdfTemplate;
	}

	/**
	 * Needs an output stream and a pdfReader and returens a new pdf stamper.
	 * 
	 * @param reader
	 * @param out
	 * @return
	 */
	public static final PdfStamper getPdfStamper(PdfReader reader, ByteArrayOutputStream out) {
		PdfStamper stamper = null;
		try {
			stamper = new PdfStamper(reader, out);
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stamper;
	}

	/**
	 * Closed the pdfreader and the pdfstamper.
	 * 
	 * @param reader
	 * @param stamper
	 */
	public static final void closePdf(PdfReader reader, PdfStamper stamper) {
		try {
			stamper.close();
			reader.close();
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generates a code128 field with the passed content, adds it at the desired
	 * location in the pdf
	 * 
	 * @param reader
	 * @param stamper
	 * @param code128Str
	 * @param hight
	 * @param lenth
	 * @param x
	 * @param y
	 */
	public static final void generateCode128Field(PdfReader reader, PdfStamper stamper, String code128Str, int x, int y,
			float lenth, float hight) {
		PdfContentByte over = stamper.getOverContent(1);
		Rectangle pagesize = reader.getPageSize(1);

		Barcode128 code128 = new Barcode128();
		code128.setBarHeight(hight);
		code128.setFont(null);
		code128.setX(lenth);
		code128.setCode(code128Str);

		com.lowagie.text.pdf.PdfTemplate template = code128.createTemplateWithBarcode(over, Color.BLACK, Color.BLACK);

		over.addTemplate(template, pagesize.getLeft() + x, pagesize.getTop() - y);
		System.out.println("printred");
	}

	/**
	 * Sets a field in the pdf stamper.
	 * 
	 * @param stamper
	 * @param field
	 * @param content
	 */
	public static final void setStamperField(PdfStamper stamper, String field, String content) {
		try {
			stamper.getAcroFields().setField(field, content);
		} catch (IOException | DocumentException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prints all stamper fields within the pdf File
	 * 
	 * @param stamper
	 */
	public static final void printAllStamperFields(PdfStamper stamper) {
		AcroFields fields = stamper.getAcroFields();

		Set<String> fldNames = fields.getFields().keySet();

		for (String fldName : fldNames) {
			System.out.println(fldName + ": " + fields.getField(fldName));
		}
	}

	public static final PDFContainer mergePdfs(List<PDFContainer> containers, String name, DocumentType type) {
		Document document = new Document();
		ByteOutputStream out = new ByteOutputStream();

		PdfWriter writer;
		try {
			writer = PdfWriter.getInstance(document, out);
			document.open();
			PdfContentByte cb = writer.getDirectContent();

			for (PDFContainer pdfContainer : containers) {
				PdfReader pdfReader = new PdfReader(pdfContainer.getData());
				for (int i = 1; i <= pdfReader.getNumberOfPages(); i++) {
					document.newPage();
					// import the page from source pdf
					PdfImportedPage page = writer.getImportedPage(pdfReader, i);
					// add the page to the destination pdf
					cb.addTemplate(page, 0, 0);
				}
			}

			document.close();
		} catch (DocumentException | IOException e) {
			e.printStackTrace();
			return null;
		}

		return new PDFContainer(type, name, out.getBytes());
	}

	class PDFGenerator {

		private File workingDirectory;
		private File output;
		private File template;
		private File processedTex;
		private JLRConverter converter;
		private PrintTemplate printTemplate;

		public PDFGenerator(PrintTemplate printTemplate) {
			openNewPDf(printTemplate);
		}

		public JLRConverter openNewPDf(PrintTemplate printTemplate) {
			this.printTemplate = printTemplate;
			workingDirectory = new File(
					HistoSettings.getAbsolutePath(mainHandlerAction.getSettings().getWorkingDirectory()));

			output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

			logger.debug("TemplateUtil File: " + HistoSettings.getAbsolutePath(printTemplate.getFile()));

			template = new File(HistoSettings.getAbsolutePath(printTemplate.getFile()));

			processedTex = new File(workingDirectory.getAbsolutePath() + File.separator + "tmp.tex");

			converter = new JLRConverter(workingDirectory);

			return converter;
		}

		public PDFContainer generatePDF() {
			try {

				if (!converter.parse(template, processedTex)) {
					logger.error(converter.getErrorMessage());
				}

				JLRGenerator pdfGen = new JLRGenerator();

				if (!pdfGen.generate(processedTex, output, workingDirectory)) {
					logger.error(pdfGen.getErrorMessage());
					return null;
				}

				File test = pdfGen.getPDF();
				byte[] data = readContentIntoByteArray(test);

				return new PDFContainer(printTemplate.getDocumentTyp(),
						"_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf", data);

			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}

		public File getWorkingDirectory() {
			return workingDirectory;
		}

		public File getOutput() {
			return output;
		}

		public File getTemplate() {
			return template;
		}

		public File getProcessedTex() {
			return processedTex;
		}

		public JLRConverter getConverter() {
			return converter;
		}

		public PrintTemplate getPrintTemplate() {
			return printTemplate;
		}

		public void setWorkingDirectory(File workingDirectory) {
			this.workingDirectory = workingDirectory;
		}

		public void setOutput(File output) {
			this.output = output;
		}

		public void setTemplate(File template) {
			this.template = template;
		}

		public void setProcessedTex(File processedTex) {
			this.processedTex = processedTex;
		}

		public void setConverter(JLRConverter converter) {
			this.converter = converter;
		}

		public void setPrintTemplate(PrintTemplate printTemplate) {
			this.printTemplate = printTemplate;
		}

	}
}
