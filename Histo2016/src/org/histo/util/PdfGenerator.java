package org.histo.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.histo.action.MainHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DiagnosisStatusState;
import org.histo.config.enums.Gender;
import org.histo.model.Contact;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PdfTemplate;
import org.histo.model.transitory.json.PdfTemplate.CodeRectangle;
import org.histo.model.transitory.json.TexTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.AcroFields;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfCopy;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfSmartCopy;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfWriter;
import com.sun.faces.facelets.util.Path;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.nixosoft.jlr.JLRConverter;
import de.nixosoft.jlr.JLRGenerator;

public class PdfGenerator {

	private ResourceBundle resourceBundle;

	private MainHandlerAction mainHandlerAction;

	public PdfGenerator(MainHandlerAction mainHandlerAction, ResourceBundle resourceBundle) {
		this.resourceBundle = resourceBundle;
		this.mainHandlerAction = mainHandlerAction;
	}

	public PDFContainer generatePDFForReport(Patient patient, Task task, TexTemplate texTemplate,
			Person toSendAddress) {
		;
		File workingDirectory = new File(
				mainHandlerAction.getSettings().getAbsolutePath(mainHandlerAction.getSettings().getWorkingDirectory()));

		File output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

		System.out.println(mainHandlerAction.getSettings().getAbsolutePath(texTemplate.getFile()));
		// loading tex file
		File template = new File(mainHandlerAction.getSettings().getAbsolutePath(texTemplate.getFile()));

		File processedTex = new File(workingDirectory.getAbsolutePath() + File.separator + "tmp.tex");

		JLRConverter converter = new JLRConverter(workingDirectory);

		// $name
		converter.replace("name", task.getParent().getPerson().getName());
		// $surName
		converter.replace("surName", task.getParent().getPerson().getSurname());
		// $birthday
		converter.replace("birthday", resourceBundle.get("pdf.birthday") + " "
				+ mainHandlerAction.date(task.getParent().getPerson().getBirthday()));
		// $piz
		converter.replace("piz", mainHandlerAction.date(task.getDiagnosisInfo().getSignatureDate()));

		// $reportDate
		converter.replace("reportDate", task.getParent().getPerson().getSurname());

		// $address
		if (toSendAddress == null) {
			// converter.replace("toSendAress",
			// resourceBundle.get("pdf.address.none"));
		} else {
			// StringBuffer re
			// converter.replace("toSendAressSex", (toSendAddress.getGender() ==
			// Gender.FEMALE
			// ? resourceBundle.get("pdf.address.female") :
			// resourceBundle.get("pdf.address.male")));
			// converter.replace("toSendAressTitle", toSendAddress.getTitle());
			// converter.replace("toSendAressName", toSendAddress.getName());
			// converter.replace("toSendAressStreet",
			// toSendAddress.getStreet());
			// converter.replace("toSendAressHouseNumber",
			// toSendAddress.getHouseNumber());
			//
			// converter.replace("toSendAressTown", "");
		}

		try {

			if (!converter.parse(template, processedTex)) {
				System.out.println(converter.getErrorMessage());
			}

			JLRGenerator pdfGen = new JLRGenerator();

			if (!pdfGen.generate(processedTex, output, workingDirectory)) {
				System.out.println(pdfGen.getErrorMessage());
			}

			File test = pdfGen.getPDF();
			byte[] data = readContentIntoByteArray(test);

			return new PDFContainer(texTemplate.getDocumentTyp().toString(),
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

	public PDFContainer generatePdfForTemplate(Task task, PdfTemplate template, long dateOfReport,
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

	public PDFContainer generatePdf(Task task, PdfTemplate template, long dateOfReport, Physician addressPhysician,
			Physician signingPhysician) {
		return generatePdf(task, template, dateOfReport, addressPhysician, signingPhysician, null);
	}

	public PDFContainer generatePdf(Task task, PdfTemplate template, long dateOfReport, Physician addressPhysician,
			Physician signingPhysician, HashMap<String, String> additionalFields) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PdfReader pdfReader = getPdfFile(template.getFileWithLogo());
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
		drawBarCodes(task, template, pdfReader, pdf);

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

		String pdfName = (template.isNameAsResources() ? resourceBundle.get(template.getName()) : template.getName())
				+ "_" + task.getPatient().getPiz();

		return new PDFContainer(template.getType(),
				pdfName + "_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf",
				out.toByteArray());
	}

	public final void drawBarCodes(Task task, PdfTemplate template, PdfReader reader, PdfStamper stamper) {
		if (template.getPizCode() != null && task.getParent().getPiz() != null
				&& !task.getParent().getPiz().isEmpty()) {
			drawBarCode(reader, stamper, task.getParent().getPiz(), template.getPizCode());
		}

		if (template.getTaskCode() != null) {
			drawBarCode(reader, stamper, task.getTaskID(), template.getTaskCode());
		}
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
		// DiagnosisStatusState.RE_DIAGNOSIS_NEEDED)
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
		// DiagnosisStatusState.RE_DIAGNOSIS_NEEDED) {
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

		System.out.println(path);
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

	/**
	 * Draws a given barcode into the pdf file
	 * 
	 * @param reader
	 * @param stamper
	 * @param piz
	 * @param codes
	 */
	public static final void drawBarCode(PdfReader reader, PdfStamper stamper, String codeStr, CodeRectangle[] codes) {
		for (CodeRectangle code : codes) {
			generateCode128Field(reader, stamper, codeStr, code.getX(), code.getY(), code.getWidth(), code.getHeight());
		}
	}

	public static final PDFContainer mergePdfs(List<PDFContainer> containers, String name, String type) {
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
}
