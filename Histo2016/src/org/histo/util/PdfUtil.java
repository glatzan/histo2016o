package org.histo.util;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import org.histo.config.HistoSettings;
import org.histo.model.Task;

import com.lowagie.text.DocumentException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfStamper;


/**
 *@formatter:off
 * I_Name
 * I_Birthday
 * I_Insurance
 * I_PIZ_CODE
 * I_PIZ
 * I_Date
 *
 * I_SAMPLES
 * I_EDATE
 * I_TASK_NUMBER_CODE
 * I_TASK_NUMBER
 * I_EYE
 * I_HISTORY
 * C_INSURANCE_NORMAL
 * C_INSURANCE_PRIVATE
 * I_WARD
 * C_MALIGN
 *
 * I_RESIDENT_DOCTOR_FAX
 * I_SURGEON
 *
 * I_DIAGNOSIS_EXTENDED
 * I_DIAGNOSIS
 *
 * I_SIGANTURE_DATE
 *
 * I_PHYSICIAN
 * I_PHYSICIAN_ROLE
 * I_CONSULTANT
 * I_CONSULTANT_ROLE
 *@formatter:on
 * @author glatza
 *
 */
public class PdfUtil {

	public static final byte[] populatePdf(PdfReader pdf, Task task) {

		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try {
			PdfStamper stamper = new PdfStamper(pdf, out);

			stamper.setFormFlattening(true);

			stamper.getAcroFields().setField("I_Name", task.getParent().getPerson().getName()+", "+task.getParent().getPerson().getSurname());
			stamper.getAcroFields().setField("I_Birthday", TimeUtil.formatDate(task.getParent().getPerson().getBirthday(), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));
			stamper.getAcroFields().setField("I_Insurance", task.getParent().getInsurance());
			stamper.getAcroFields().setField("I_PIZ_CODE",  task.getParent().getPiz());
			stamper.getAcroFields().setField("I_PIZ",  task.getParent().getPiz());
			stamper.getAcroFields().setField("I_Date", TimeUtil.formatDate(new Date(System.currentTimeMillis()), HistoSettings.STANDARD_DATEFORMAT_DAY_ONLY));
			
			
			stamper.close();
			pdf.close();

			FileOutputStream fos = new FileOutputStream("Q:\\AUG-T-HISTO\\Formulare\\ergebnis-test.pdf");

			fos.write(out.toByteArray());

			fos.close();

		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return out.toByteArray();
	}
}
