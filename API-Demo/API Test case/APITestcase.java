import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.relevantcodes.extentreports.ExtentReports;
import com.relevantcodes.extentreports.ExtentTest;
import com.relevantcodes.extentreports.LogStatus;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.appium.java_client.TouchAction;
import io.appium.java_client.touch.offset.PointOption
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class APITestcase {
	private static ExtentReports extentReports;
	private static ExtentTest test;
	private static final Logger lOGGER = LoggerFactory.getLogger(APITestcase.class);
	private enum Status {
		PASS, FAIL
	}

	private static class Assertion {
		private String assertionCheckOn;
		private String assertionType;
		private String path;
		private String value;
		private String headerKey;

		public String getHeaderKey() {
			return headerKey;
		}

		public void setHeaderKey(String headerKey) {
			this.headerKey = headerKey;
		}

		public String getAssertionCheckOn() {
			return assertionCheckOn;
		}

		public void setAssertionCheckOn(String assertionCheckOn) {
			this.assertionCheckOn = assertionCheckOn;
		}

		public String getAssertionType() {
			return assertionType;
		}

		public void setAssertionType(String assertionType) {
			this.assertionType = assertionType;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

	}

	private void createReport() {
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd-hh.mm.ss.SSS");
		Random rand = new Random();
		String userName = System.getProperty("user.name");
		int num = rand.nextInt(1000);
		Date date = new Date();
		String time = simpleDateFormat.format(date) + num;
		String reportFilePath = System.getProperty("user.dir") + "/Reports";
		File file = new File(reportFilePath);
		file.mkdir();
		extentReports = new ExtentReports(file.getAbsolutePath() + "/Report_" + time + ".html", true);
		try {
			extentReports.addSystemInfo("Host Name", InetAddress.getLocalHost().getHostName())
					.addSystemInfo("Environment", "Framework").addSystemInfo("User Name", userName);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}
	private void logReport(WebDriver webDriver, Status status) {
		String screenshot = getScreenshot(webDriver);
		if (status.equals(Status.PASS)) {
			test.log(LogStatus.PASS, "Success" + "<br>" + test.addBase64ScreenShot(screenshot));
		} else if (status.equals(Status.PASS)) {
			test.log(LogStatus.FAIL, "Failed" + "<br>" + test.addBase64ScreenShot(screenshot));
		}
	}
	private void startTest() {
		try {
			createReport();
			System.out.println(extentReports);
			test = extentReports.startTest("Sample");
		} catch (Exception e) {
			e.printStackTrace();
			lOGGER.error("Error while creating report.Cause:- " + e.getMessage());
		}
	}
	private static String getScreenshot(WebDriver webDriver) {
		try {
			return "data:image/png;base64, " + ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BASE64);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void launchService(String testData) throws Exception {
		JsonObject jObject = JsonParser.parseString(testData).getAsJsonObject();
		String operationName = null;

		if (jObject.has("operationName")) {
			operationName = jObject.get("operationName").getAsString();
		}

		try {
			switch (operationName) {

			case "GET":
				requestGET(testData);
				break;
			case "POST":
				requestPOST(testData);
				break;
			case "PUT":
				requestPUT(testData);
				break;
			case "DELETE":
				requestDELETE(testData);
				break;

			default:
				lOGGER.info("Please select valid operation");
				break;
			}

		} catch (Exception e) {
			throw new Exception(e.getMessage(), e);
		}

	}

	private void requestGET(String webServiceTestData) {
		Response response = null;
		JsonObject webServiceTestDataJson = JsonParser.parseString(webServiceTestData).getAsJsonObject();
		String requestType = webServiceTestDataJson.get("requestType").getAsString();
		String endPointUrl = webServiceTestDataJson.get("endPointUrl").getAsString();
		JsonObject authorization = webServiceTestDataJson.get("authorization").getAsJsonObject();
		JsonArray assertions = webServiceTestDataJson.get("assertions").getAsJsonArray();
		String headersString = webServiceTestDataJson.get("headers").toString();
		Map<String, Object> headers = new HashMap<>();
		try {
			headers = mapHeaderToSend(new JSONArray(headersString));
		} catch (JSONException e) {
			lOGGER.info("Not able to map the headers");
			return;
		}
		if (authorization != null && authorization.get("authorizationType").getAsString().equals("None")) {
			response = (Response) RestAssured.given().log().all().headers(headers).contentType(requestType)
					.get(endPointUrl).then().log().all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Basic")) {
			response = (Response) RestAssured.given().auth()
					.basic(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null
				&& authorization.get("authorizationType").getAsString().equals("Certificate")) {
			response = (Response) RestAssured.given().auth()
					.certificate(authorization.get("certificateUrl").getAsString(),
							authorization.get("password").getAsString())
					.log().all().headers(headers).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Digest")) {
			response = (Response) RestAssured.given().auth()
					.digest(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Form")) {
			response = (Response) RestAssured.given().auth()
					.form(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth")) {
			response = (Response) RestAssured.given().auth()
					.oauth(authorization.get("consumerKey").getAsString(),
							authorization.get("consumerSecret").getAsString(),
							authorization.get("acessToken").getAsString(),
							authorization.get("secretToken").getAsString())
					.log().all().headers(headers).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth2")) {
			response = (Response) RestAssured.given().auth().oauth2(authorization.get("accessToken").getAsString())
					.log().all().headers(headers).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else {
			lOGGER.info("Invalid authorization selected pleae select valid authorization");
		}
		checkAssertion(assertions, response);
	}

	private void requestPOST(String webServiceTestData) {
		Response response = null;
		JsonObject webServiceTestDataJson = JsonParser.parseString(webServiceTestData).getAsJsonObject();
		String requestType = webServiceTestDataJson.get("requestType").getAsString();
		String endPointUrl = webServiceTestDataJson.get("endPointUrl").getAsString();
		JsonObject authorization = webServiceTestDataJson.get("authorization").getAsJsonObject();
		JsonArray assertions = webServiceTestDataJson.get("assertions").getAsJsonArray();
		String body = webServiceTestDataJson.get("body").getAsString();
		String headersString = webServiceTestDataJson.get("headers").toString();
		Map<String, Object> headers = new HashMap<>();
		try {
			headers = mapHeaderToSend(new JSONArray(headersString));
		} catch (JSONException e) {
			lOGGER.info("Not able to map the headers");
			return;
		}
		if (authorization != null && authorization.get("authorizationType").getAsString().equals("None")) {
			response = (Response) RestAssured.given().log().all().headers(headers).body(body).contentType(requestType)
					.post(endPointUrl).then().log().all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Basic")) {
			response = (Response) RestAssured.given().auth()
					.basic(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).post(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null
				&& authorization.get("authorizationType").getAsString().equals("Certificate")) {
			response = (Response) RestAssured.given().auth()
					.certificate(authorization.get("certificateUrl").getAsString(),
							authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).post(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Digest")) {
			response = (Response) RestAssured.given().auth()
					.digest(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).post(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Form")) {
			response = (Response) RestAssured.given().auth()
					.form(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).post(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth")) {
			response = (Response) RestAssured.given().auth()
					.oauth(authorization.get("consumerKey").getAsString(),
							authorization.get("consumerSecret").getAsString(),
							authorization.get("acessToken").getAsString(),
							authorization.get("secretToken").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).post(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth2")) {
			response = (Response) RestAssured.given().auth().oauth2(authorization.get("accessToken").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).get(endPointUrl).then().log()
					.all().extract().response();
		} else {
			lOGGER.info("Invalid authorization selected pleae select valid authorization");
		}
		checkAssertion(assertions, response);
	}

	private void requestPUT(String webServiceTestData) {
		Response response = null;
		JsonObject webServiceTestDataJson = JsonParser.parseString(webServiceTestData).getAsJsonObject();
		String requestType = webServiceTestDataJson.get("requestType").getAsString();
		String endPointUrl = webServiceTestDataJson.get("endPointUrl").getAsString();
		JsonObject authorization = webServiceTestDataJson.get("authorization").getAsJsonObject();
		JsonArray assertions = webServiceTestDataJson.get("assertions").getAsJsonArray();
		String body = webServiceTestDataJson.get("body").getAsString();
		String headersString = webServiceTestDataJson.get("headers").toString();
		Map<String, Object> headers = new HashMap<>();
		try {
			headers = mapHeaderToSend(new JSONArray(headersString));
		} catch (JSONException e) {
			lOGGER.info("Not able to map the headers");
			return;
		}
		if (authorization != null && authorization.get("authorizationType").getAsString().equals("None")) {
			response = (Response) RestAssured.given().log().all().headers(headers).body(body).contentType(requestType)
					.put(endPointUrl).then().log().all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Basic")) {
			response = (Response) RestAssured.given().auth()
					.basic(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).put(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null
				&& authorization.get("authorizationType").getAsString().equals("Certificate")) {
			response = (Response) RestAssured.given().auth()
					.certificate(authorization.get("certificateUrl").getAsString(),
							authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).put(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Digest")) {
			response = (Response) RestAssured.given().auth()
					.digest(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).put(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Form")) {
			response = (Response) RestAssured.given().auth()
					.form(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).put(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth")) {
			response = (Response) RestAssured.given().auth()
					.oauth(authorization.get("consumerKey").getAsString(),
							authorization.get("consumerSecret").getAsString(),
							authorization.get("acessToken").getAsString(),
							authorization.get("secretToken").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).put(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth2")) {
			response = (Response) RestAssured.given().auth().oauth2(authorization.get("accessToken").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).put(endPointUrl).then().log()
					.all().extract().response();
		} else {
			lOGGER.info("Invalid authorization selected pleae select valid authorization");
		}
		checkAssertion(assertions, response);
	}

	private void requestDELETE(String webServiceTestData) {
		Response response = null;
		JsonObject webServiceTestDataJson = JsonParser.parseString(webServiceTestData).getAsJsonObject();
		String requestType = webServiceTestDataJson.get("requestType").getAsString();
		String endPointUrl = webServiceTestDataJson.get("endPointUrl").getAsString();
		JsonObject authorization = webServiceTestDataJson.get("authorization").getAsJsonObject();
		JsonArray assertions = webServiceTestDataJson.get("assertions").getAsJsonArray();
		String body = webServiceTestDataJson.get("body").getAsString();
		String headersString = webServiceTestDataJson.get("headers").toString();
		Map<String, Object> headers = new HashMap<>();
		try {
			headers = mapHeaderToSend(new JSONArray(headersString));
		} catch (JSONException e) {
			lOGGER.info("Not able to map the headers");
			return;
		}
		if (authorization != null && authorization.get("authorizationType").getAsString().equals("None")) {
			response = (Response) RestAssured.given().log().all().headers(headers).body(body).contentType(requestType)
					.delete(endPointUrl).then().log().all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Basic")) {
			response = (Response) RestAssured.given().auth()
					.basic(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).delete(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null
				&& authorization.get("authorizationType").getAsString().equals("Certificate")) {
			response = (Response) RestAssured.given().auth()
					.certificate(authorization.get("certificateUrl").getAsString(),
							authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).delete(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Digest")) {
			response = (Response) RestAssured.given().auth()
					.digest(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).delete(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("Form")) {
			response = (Response) RestAssured.given().auth()
					.form(authorization.get("userName").getAsString(), authorization.get("password").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).delete(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth")) {
			response = (Response) RestAssured.given().auth()
					.oauth(authorization.get("consumerKey").getAsString(),
							authorization.get("consumerSecret").getAsString(),
							authorization.get("acessToken").getAsString(),
							authorization.get("secretToken").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).delete(endPointUrl).then().log()
					.all().extract().response();
		} else if (authorization != null && authorization.get("authorizationType").getAsString().equals("OAuth2")) {
			response = (Response) RestAssured.given().auth().oauth2(authorization.get("accessToken").getAsString())
					.log().all().headers(headers).body(body).contentType(requestType).delete(endPointUrl).then().log()
					.all().extract().response();
		} else {
			lOGGER.info("Invalid authorization selected pleae select valid authorization");
		}
		checkAssertion(assertions, response);
	}

	private void checkAssertion(JsonArray assertionJsonArrray, Response response) {
		try {
			if (assertionJsonArrray.size() > 0) {
				ObjectMapper objectMapper = new ObjectMapper();
				objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
				for (int i = 0; i < assertionJsonArrray.size(); i++) {
					Assertion assertionDto = new Assertion();
					JsonObject jsonObject = assertionJsonArrray.get(i).getAsJsonObject();
					assertionDto.setAssertionCheckOn(jsonObject.get("assertionCheckOn").getAsString());
					assertionDto.setAssertionType(jsonObject.get("assertionType").getAsString());
					assertionDto.setPath(jsonObject.get("path").getAsString());
					assertionDto.setValue(jsonObject.get("value").getAsString());
					assertionDto.setHeaderKey(jsonObject.get("headerKey").getAsString());
					try {
						if (assertionDto.getAssertionCheckOn().equals("Body")) {
							String path = assertionDto.getPath();
							String value = assertionDto.getValue();
							DocumentContext docCtx = JsonPath.parse(response.asString());
							JsonPath jsonPath = JsonPath.compile(path);

							Object actualValue = null;
							Object expectedValue = null;
							if (isJsonObject(value)) {
								expectedValue = objectMapper.readTree(value);
							} else {
								expectedValue = value;
							}
							if (isJsonObject(docCtx.read(jsonPath).toString())) {
								actualValue = objectMapper.readTree(docCtx.read(jsonPath).toString());
							} else {
								actualValue = docCtx.read(jsonPath).toString();
							}
							if (assertionDto.getAssertionType().equals("Equals")) {
								assertEquals(actualValue, expectedValue);
							} else if (assertionDto.getAssertionType().equals("Not Equals")) {
								assertNotEquals(actualValue, expectedValue);
							}

							test.log(LogStatus.PASS, "Validated body for node: " + path + " ,"
									+ assertionDto.getAssertionType().toLowerCase() + " " + expectedValue.toString());
							lOGGER.info("Assertion done for body");
						}
						if (assertionDto.getAssertionCheckOn().equals("Status")) {
							if (assertionDto.getAssertionType().equals("Equals")) {
								response.then().assertThat().statusCode(Integer.valueOf(assertionDto.getValue()));
							} else if (assertionDto.getAssertionType().equals("Not Equals")) {
								response.then().assertThat()
										.statusCode(is(not(Integer.valueOf(assertionDto.getValue()))));
							}

							test.log(LogStatus.PASS, "Validated status code "
									+ assertionDto.getAssertionType().toLowerCase() + " " + assertionDto.getValue());
							lOGGER.info("Assertion done for Status");
						}
						if (assertionDto.getAssertionCheckOn().equals("Header")) {
							if (assertionDto.getAssertionType().equals("Equals")) {
								response.then().assertThat().header(assertionDto.getHeaderKey(),
										assertionDto.getValue());

							} else if (assertionDto.getAssertionType().equals("Not Equals")) {
								response.then().assertThat().header(assertionDto.getHeaderKey(),
										is(not(assertionDto.getValue())));
							}

							test.log(LogStatus.PASS,
									"Validated header key " + "<b>" + assertionDto.getHeaderKey() + "</b> "
											+ assertionDto.getAssertionType().toLowerCase() + " <b>"
											+ assertionDto.getValue() + "</b>");
							lOGGER.info("Assertion done for Header");
						}
					} catch (AssertionError e) {
						lOGGER.error("Assertion error: -" + e.getMessage());
						lOGGER.info("Assertion( at index " + (i + 1) + " ) failed. Reason " + e.getMessage());
					} catch (JsonMappingException e) {

					} catch (JsonProcessingException e) {

					}
				}
				lOGGER.info("All assertion passed as expected.");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		lOGGER.info("No assertions set to be checked.");
	}

	private static Map<String, Object> mapHeaderToSend(JSONArray jsonArray) {
		Map<String, Object> newMapToSend = new HashMap<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			try {
				JSONObject jsonObject = jsonArray.getJSONObject(i);
				if (jsonObject.getBoolean("isHeaderRequired")) {
					newMapToSend.put(jsonObject.getString("headerKey"), jsonObject.getString("headerValue"));
				}

			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return newMapToSend;
	}

	private static boolean isJsonObject(String jsonString) {
		try {
			new JSONObject(jsonString);
		} catch (JSONException e) {
			try {
				new JSONArray(jsonString);
			} catch (JSONException e1) {
				return false;
			}
		}
		return true;
	}



	@BeforeTest
	public void beforeTest() {
		startTest();
	}
	@Test
	public void testCase() throws Exception {
		try {

			// Web Service Request
		launchService("{\"operationName\":\"POST\",\"requestType\":\"application/json\",\"endPointUrl\":\"http://localhost:8010/sh/getSessionId\",\"authorization\":{\"accessToken\":\"\",\"authorizationType\":\"None\",\"certificateUrl\":\"\",\"consumerKey\":\"\",\"consumerSecret\":\"\",\"password\":\"\",\"secretToken\":\"\",\"userName\":\"\"},\"assertions\":[],\"body\":\"\",\"headers\":[{\"isHeaderRequired\":false,\"headerValue\":\"\",\"headerKey\":\"\"}]}");
			// Web Service Request
		launchService("{\"operationName\":\"POST\",\"requestType\":\"application/json\",\"endPointUrl\":\"http://localhost:8010/sh/createPTASC\",\"authorization\":{\"accessToken\":\"\",\"authorizationType\":\"None\",\"certificateUrl\":\"\",\"consumerKey\":\"\",\"consumerSecret\":\"\",\"password\":\"\",\"secretToken\":\"\",\"userName\":\"\"},\"assertions\":[],\"body\":{\"study__c\":\"\",\"study_country__c\":\"\",\"study_site__c\":\"\",\"task_name__c\":\"\",\"content__c\":\"\",\"number_of_documents_attached__c\":\"\",\"sctask_used_by_ptasc__c\":\"\",\"certify_copy__c\":\"false\"},\"headers\":[{\"isHeaderRequired\":true,\"headerValue\":\"$.{{message}}\",\"headerKey\":\"message\"},{\"isHeaderRequired\":false,\"headerValue\":\"\",\"headerKey\":\"\"}]}");
			// Query Service Request
			queryService( "API data repository","Oracle","synsoaddb11.oci.syneoshealth.com","SYNPSOAD",1526,"QA_TESTER","Gt567er34#uit5");
		} finally {
			lOGGER.info("Test Case Completed");
		}
	}
	@AfterClass
	public void tearDown() {
		extentReports.flush();
	}
}