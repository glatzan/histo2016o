package org.histo.util.printer.template;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;

import org.apache.log4j.Logger;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.DateFormat;
import org.histo.model.PDFContainer;
import org.histo.util.TimeUtil;
import org.histo.util.interfaces.FileHandlerUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import de.nixosoft.jlr.JLRConverter;
import de.nixosoft.jlr.JLRGenerator;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PDFGenerator {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private SettingsHandler settingsHandler;

	private File workingDirectory;
	private File output;
	private File template;
	private File processedTex;
	private JLRConverter converter;
	private AbstractTemplate printTemplate;

	public PDFGenerator() {
	}

	public PDFGenerator(AbstractTemplate printTemplate) {
		openNewPDf(printTemplate);
	}

	public JLRConverter openNewPDf(AbstractTemplate printTemplate) {
		this.printTemplate = printTemplate;
		workingDirectory = new File(
				FileHandlerUtil.getAbsolutePath(settingsHandler.getProgramSettings().getWorkingDirectory()));

		output = new File(workingDirectory.getAbsolutePath() + File.separator + "output/");

		template = new File(FileHandlerUtil.getAbsolutePath(printTemplate.getFile()));

		processedTex = new File(workingDirectory.getAbsolutePath() + File.separator + "tmp.tex");

		converter = new JLRConverter(workingDirectory);

		return converter;
	}

	public PDFContainer generatePDF() {
		long test1 = System.currentTimeMillis();

		try {
			System.out.println(template.getAbsolutePath());
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
}
