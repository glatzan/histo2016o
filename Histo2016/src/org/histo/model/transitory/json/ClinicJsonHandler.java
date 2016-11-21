package org.histo.model.transitory.json;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataSource;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.MultiPartEmail;
import org.apache.commons.mail.SimpleEmail;
import org.apache.log4j.Logger;
import org.histo.model.PDFContainer;
import org.histo.model.Person;
import org.histo.model.interfaces.GsonAble;
import org.histo.model.patient.Patient;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

//{
// "vorname":"Andreas",
// "mode":null,
// "status":null,
// "piz":"20366346",
// "sonderinfo":null,
// "iknr":null,
// "kvnr":null,
// "titel":null,
// "versichertenstatus":" ",
// "tel":null,
// "anschrift":"Tannenweg 34",
// "wop":null,
// "plz":"79183",
// "name":"Glatz",
// "geburtsdatum":"1988-10-04",
// "gueltig_bis":null,
// "krankenkasse":null,
// "versnr":null,
// "land":"D",
// "weiblich":"",
// "ort":"Waldkirch",
// "status2":null
//}

//{
//
//+      "vorname":"Ioana Maria",
//->       "mode":"K",
//->       "status":"1000",
//ok      "piz":"29017379",
//?      "sonderinfo":"",
//->      "iknr":"61125",
//->      "kvnr":"108018121",
//ok      "titel":null,
//->      "versichertenstatus":"1 ",
//ok     "tel":"0176 62346167",
//ok      "anschrift":"Habsburgerstr. 25",
//?      "wop":"",
//ok      "plz":"79104",
//ok      "name":"Cazana",
//ok      "geburtsdatum":"1989-10-09",
//?     "gueltig_bis":null,
//ok      "krankenkasse":"AOK Baden-Württemberg",
//?     "versnr":"U367703198",
//ok      "land":"D",
//ok      "weiblich":"1",
//ok      "ort":"Freiburg",
//"status2":"1"
//
//}

public class ClinicJsonHandler implements GsonAble {

	private static Logger logger = Logger.getLogger("org.histo");

	private String baseUrl;

	private String userAgent;

	public List<Patient> getPatientsFromClinicJson(String url) {
		String result = requestJsonData(baseUrl + url);
		return createPatientsFromClinicJson(result);
	}

	public Patient getPatientFromClinicJson(String url) {
		String result = requestJsonData(baseUrl + url);
		return createPatientFromClinicJson(result);
	}

	/**
	 * Uses a search list from the clinic backend and creates a Patient list
	 * using these data.
	 * 
	 * @param json
	 *            [{},{}]
	 * @return
	 */
	public List<Patient> createPatientsFromClinicJson(String json) {
		JSONArray arr = new JSONArray(json);
		ArrayList<Patient> patients = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++) {
			patients.add(createPatientFromClinicJson(arr.getJSONObject(i)));
		}
		return patients;
	}

	/**
	 * Creates a new patient using data from the clinic backend.
	 * 
	 * @param json
	 * @return
	 */
	public Patient createPatientFromClinicJson(String json) {
		return createPatientFromClinicJson(new JSONObject(json));
	}

	/**
	 * Creates a new Patient using a json object contain data from the clinic
	 * backend
	 * 
	 * @param json
	 * @return
	 */
	public Patient createPatientFromClinicJson(JSONObject json) {
		Patient patient = new Patient();
		patient.setPerson(new Person());

		if (json.has("error")) {
			patient.getPerson().setName("Zu viele");
			return patient;
		}

		patient.copyIntoObject(json);

		return patient;
	}

	public String requestJsonData(String url) {

		StringBuffer response = new StringBuffer();

		try {

			logger.debug("Fetching patient json from clinic backend: " + url);

			URL obj = new URL(url);

			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			con.setRequestProperty("User-Agent", userAgent);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		logger.debug("Fetch string from clinic: " + response.toString());
		return response.toString();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
