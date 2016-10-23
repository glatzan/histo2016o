package org.histo.util;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.Resource;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;
import com.lowagie.text.pdf.PdfTemplate;

public class PdfUtil {

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
	public static final void generateCode128Field(PdfReader reader, PdfStamper stamper, String code128Str, float hight,
			float lenth, int x, int y) {
		PdfContentByte over = stamper.getOverContent(1);
		Rectangle pagesize = reader.getPageSize(1);

		Barcode128 code128 = new Barcode128();
		code128.setBarHeight(hight);
		code128.setFont(null);
		code128.setX(lenth);
		code128.setCode(code128Str);

		PdfTemplate template = code128.createTemplateWithBarcode(over, Color.BLACK, Color.BLACK);

		over.addTemplate(template, pagesize.getLeft() + x, pagesize.getTop() - y);
	}
}
