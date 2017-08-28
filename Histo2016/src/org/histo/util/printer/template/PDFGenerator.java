package org.histo.util.printer.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.histo.action.dialog.ProgrammVersionDialog;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.settings.LdapHandler;
import org.histo.settings.ProgramSettings;
import org.histo.util.TimeUtil;
import org.histo.util.interfaces.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.nixosoft.jlr.JLRConverter;
import de.nixosoft.jlr.JLRGenerator;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PDFGenerator {

	private static Logger logger = Logger.getLogger("org.histo");

	private File workingDirectory;
	private File output;
	private File template;
	private File processedTex;
	private JLRConverter converter;
	private AbstractTemplate printTemplate;

	private ProgramSettings programSettings;

	public PDFGenerator() {
		this(null);
	}

	public PDFGenerator(AbstractTemplate printTemplate) {
		if (printTemplate != null)
			openNewPDf(printTemplate);

		// loading program settings, can't use settings bean, because this
		// object might operate in a thread
		JsonParser parser = new JsonParser();
		JsonObject o = parser.parse(FileHandlerUtil.getContentOfFile(SettingsHandler.PROGRAM_SETTINGS))
				.getAsJsonObject();

		Gson gson = new Gson();

		programSettings = gson.fromJson(o.get(SettingsHandler.SETTINGS_OBJECT_GENERAL), ProgramSettings.class);
	}

	public JLRConverter openNewPDf(AbstractTemplate printTemplate) {
		this.printTemplate = printTemplate;
		workingDirectory = new File(FileHandlerUtil.getAbsolutePath(programSettings.getWorkingDirectory()));

		output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

		template = new File(FileHandlerUtil.getAbsolutePath(printTemplate.getFile()));

		processedTex = new File(workingDirectory.getAbsolutePath() + File.separator
				+ RandomStringUtils.randomAlphanumeric(10) + ".tex");

		converter = new JLRConverter(workingDirectory);

		return converter;
	}

	public PDFContainer generatePDF() {
		long test1 = System.currentTimeMillis();

		try {
			if (!converter.parse(template, processedTex)) {
				logger.error(converter.getErrorMessage());
			}

			JLRGenerator pdfGen = new JLRGenerator();

			if (!pdfGen.generate(processedTex, output, workingDirectory)) {
				logger.debug("Error in File " + processedTex.getAbsolutePath());
				logger.error(pdfGen.getErrorMessage());
				return null;
			}

			File test = pdfGen.getPDF();
			byte[] data = readContentIntoByteArray(test);

			System.out.println((System.currentTimeMillis() - test1));

			return new PDFContainer(printTemplate.getDocumentType(),
					"_" + TimeUtil
							.formatDate(new Date(System.currentTimeMillis()), DateFormat.GERMAN_DATE.getDateFormat())
							.replace(".", "_") + ".pdf",
					data);

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
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
}
