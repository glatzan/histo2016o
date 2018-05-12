package org.histo.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import org.histo.action.MainHandlerAction;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Patient;
import org.histo.service.PDFService;
import org.histo.service.dao.PDFDao;
import org.histo.service.dao.PatientDao;
import org.histo.service.dao.impl.PatientDaoImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CommonController {

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private PDFService pdfService;

	@RequestMapping(value = "/test")
	public String getLogin() {
		System.out.println("Hello Welcome to API " + mainHandlerAction);
		return "Hello Welcome to API";
	}

	@RequestMapping(value = "/dologout", method = RequestMethod.GET)
	public String getLogout() {
		return "Exit to API";
	}

	@RequestMapping(value = "/upload", method = RequestMethod.POST)
	public @ResponseBody String handleFileUpload(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "param1", required = true) String piz) {
		String name = "test11";
		if (!file.isEmpty()) {
			try {
				byte[] bytes = file.getBytes();
				BufferedOutputStream stream = new BufferedOutputStream(
						new FileOutputStream(new File("d:\\" + name + "-uploaded")));
				stream.write(bytes);

				PDFContainer test = new PDFContainer();
				test.setCreationDate(System.currentTimeMillis());
				test.setData(bytes);
				test.setName("TEST");

				Patient p = patientDao.findByPiz(piz);

				pdfService.attachPDF(p, p, test);

				stream.close();
				return "You successfully uploaded " + name + " into " + name + "-uploaded !";
			} catch (Exception e) {
				return "You failed to upload " + name + " => " + e.getMessage();
			}
		} else {
			return "You failed to upload " + name + " because the file was empty.";
		}
	}
}
