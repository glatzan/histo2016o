package org.histo.util.printer.template;


public class TemplateSendReport extends AbstractTemplate  {

}
//public PDFContainer generateSendReport(AbstractTemplate printTemplate, Patient patient,
//		EmailNotificationSettings emails, FaxNotificationSettings fax, PhoneNotificationSettings phone) {
//	PDFGenerator generator = new PDFGenerator(printTemplate);
//
//	generator.getConverter().replace("patient", patient);
//	generator.getConverter().replace("emailInfos", emails);
//	generator.getConverter().replace("faxInfos", fax);
//	generator.getConverter().replace("phoneInfos", phone);
//	generator.getConverter().replace("reportDate", mainHandlerAction.date(System.currentTimeMillis()));
//
//	return generator.generatePDF();
//}