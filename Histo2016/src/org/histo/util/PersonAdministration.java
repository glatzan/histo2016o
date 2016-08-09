package org.histo.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.histo.model.Patient;
import org.histo.model.Person;
import org.primefaces.json.JSONArray;
import org.primefaces.json.JSONObject;

//    {
//	   "vorname":"Andreas",
//	   "mode":null,
//	   "status":null,
//	   "piz":"20366346",
//	   "sonderinfo":null,
//	   "iknr":null,
//	   "kvnr":null,
//	   "titel":null,
//	   "versichertenstatus":" ",
//	   "tel":null,
//	   "anschrift":"Tannenweg 34",
//	   "wop":null,
//	   "plz":"79183",
//	   "name":"Glatz",
//	   "geburtsdatum":"1988-10-04",
//	   "gueltig_bis":null,
//	   "krankenkasse":null,
//	   "versnr":null,
//	   "land":"D",
//	   "weiblich":"",
//	   "ort":"Waldkirch",
//	   "status2":null
//	}

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
// "status2":"1"
//
//}

public class PersonAdministration {
	private final String USER_AGENT = "Mozilla/5.0";

	public String getRequest(String url) {

		StringBuffer response = new StringBuffer();

		try {
			URL obj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			con.setRequestProperty("User-Agent", USER_AGENT);

			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			String inputLine;
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();

		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}

		return response.toString();
	}

	/**
	 * Uses a search list from the clinic backend and creates a Patient list
	 * using these data.
	 * 
	 * @param json
	 *            [{},{}]
	 * @return
	 */
	public List<Patient> getPatientsFromClinicJson(String json) {
		JSONArray arr = new JSONArray(json);
		ArrayList<Patient> patients = new ArrayList<>();
		for (int i = 0; i < arr.length(); i++) {
			patients.add(getPatientFromClinicJson(arr.getJSONObject(i)));
		}
		return patients;
	}

	/**
	 * Creates a new patient using data from the clinic backend.
	 * 
	 * @param json
	 * @return
	 */
	public Patient getPatientFromClinicJson(String json) {
		JSONObject obj = new JSONObject(json);
		return getPatientFromClinicJson(obj);
	}

	public Patient getPatientFromClinicJson(JSONObject json) {
		Patient patient = new Patient();
		patient.setPerson(new Person());
		return updatePatientFromClinicJson(patient, json);
	}

	public Patient updatePatientFromClinicJson(Patient patient, String json) {
		JSONObject obj = new JSONObject(json);
		return updatePatientFromClinicJson(patient, obj);
	}

	/**
	 * Updates a patient objekt with a given json array from the clinic backend
	 *
	 * { "vorname":"Test", "mode":"W", "status":null, "piz":"25201957",
	 * "sonderinfo":"", "iknr":"00190", "kvnr":null, "titel":"Prof. Dr. med.",
	 * "versichertenstatus":" ", "tel":"12-4085", "anschrift": "Gillenweg 4",
	 * "wop":null, "plz":"79110", "name":"Test", "geburtsdatum":"1972-08-22",
	 * "gueltig_bis":null, "krankenkasse":"Wissenschaftliche Unters.",
	 * "versnr":null, "land":"D", "weiblich":"", "ort":"Freiburg",
	 * "status2":null }
	 * 
	 * @param patient
	 * @param json
	 * @return
	 */
	public Patient updatePatientFromClinicJson(Patient patient, JSONObject obj) {

		if (patient.getPerson() == null)
			patient.setPerson(new Person());

		patient.setPiz(obj.optString("piz"));
		
		Person person = patient.getPerson();
		
		// Person data
		person.setTitle(obj.optString("titel"));
		person.setName(obj.optString("name"));
		person.setSurname(obj.optString("vorname"));
		// parsing date
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN);
		Date date;
		try {
			date = format.parse(obj.optString("geburtsdatum"));
		} catch (ParseException e) {
			date = new Date();
		}
		person.setBirthday(date);

		person.setTown(obj.optString("ort"));
		person.setLand(obj.optString("land"));
		person.setPostcode(obj.optString("plz"));
		person.setStreet(obj.optString("anschrift"));
		person.setPhoneNumber(obj.optString("tel"));
		// 1 equals female, empty equals male
		person.setGender(obj.optString("weiblich").equals("1") ? Person.GENDER_FEMALE : Person.GENDER_MALE);

		// TODO
		person.setEmail("");
		// todo
		person.setHouseNumber("");

		// patient data
		patient.setInsurance(obj.optString("krankenkasse"));

		return patient;
	}
}
