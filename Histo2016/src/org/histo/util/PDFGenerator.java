package org.histo.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.template.DocumentTemplate;
import org.histo.util.interfaces.FileHandlerUtil;
import org.histo.util.interfaces.PDFNonBlockingReturnHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream;

import de.nixosoft.jlr.JLRConverter;
import de.nixosoft.jlr.JLRGenerator;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PDFGenerator {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ThreadPoolTaskExecutor taskExecutor;

	private File workingDirectory;
	private File output;
	private File template;
	private File processedTex;
	private JLRConverter converter;
	private DocumentTemplate printTemplate;

	public PDFGenerator() {
		this(null);
	}

	public PDFGenerator(DocumentTemplate printTemplate) {
		if (printTemplate != null)
			openNewPDf(printTemplate);
	}

	public JLRConverter openNewPDf(DocumentTemplate printTemplate) {
		this.printTemplate = printTemplate;

		workingDirectory = new File(
				FileHandlerUtil.getAbsolutePath(globalSettings.getProgramSettings().getWorkingDirectory()));

		output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

		template = new File(FileHandlerUtil.getAbsolutePath(printTemplate.getContent()));

		processedTex = new File(workingDirectory.getAbsolutePath() + File.separator
				+ RandomStringUtils.randomAlphanumeric(10) + ".tex");

		converter = new JLRConverter(workingDirectory);

		return converter;
	}

	/**
	 * Generates a new PDF in a blocking way
	 * 
	 * @return
	 */
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

			logger.debug("Generation time " + (System.currentTimeMillis() - test1) + " ms");

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

	/**
	 * Generates a PDF in a non blocking way (new thread), needs a return handler.
	 * 
	 * @param returnHandler
	 */
	public void generatePDFNonBlocking(PDFNonBlockingReturnHandler returnHandler) {
		taskExecutor.execute(new Thread() {
			public void run() {
				logger.debug("Stargin PDF Generation in new Thread");
				PDFContainer returnPDF = generatePDF();

				returnHandler.setPDFContent(returnPDF);
				returnHandler.setPDFGenerationStatus(true);
				logger.debug("PDF Generation completed, thread ended");
			}
		});
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

	public static final List<PDFContainer> getPDFsofType(List<PDFContainer> containers, DocumentType type) {
		return containers.stream().filter(p -> p.getType().equals(type)).collect(Collectors.toList());
	}

	public static final PDFContainer getLatestPDFofType(List<PDFContainer> containers, DocumentType type) {
		return getLatestPDFofType(getPDFsofType(containers, type));
	}

	public static final PDFContainer getLatestPDFofType(List<PDFContainer> containers) {
		if (containers.size() == 0)
			return null;

		PDFContainer latest = containers.get(0);

		for (PDFContainer pdfContainer : containers) {
			if (latest.getCreationDate() < pdfContainer.getCreationDate())
				latest = pdfContainer;
		}

		return latest;
	}

	public static final List<PDFContainer> sortPDFListByDate(List<PDFContainer> list, boolean asc) {

		// sorting
		Collections.sort(list, (PDFContainer p1, PDFContainer p2) -> {
			if (p1.getCreationDate() == p2.getCreationDate()) {
				return 0;
			} else if (p1.getCreationDate() < p2.getCreationDate()) {
				return asc ? -1 : 1;
			} else {
				return asc ? 1 : -1;
			}
		});

		return null;
	}
}
