package org.random.api;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import java.net.URL;

import java.util.Random;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * A Java implementation of the random.org api. It implements the methods from the api as blocking remote procedure calls
 * using the standard Java libraries for sending data via HTTP and the Google Gson libraries for representing JSON objects in Java.
 * @see https://api.random.org/
 * @see http://code.google.com/p/google-gson/
 * @author Anders Haahr
 */
public class RandomJSONRPC {
	/** The URL to send the remote procedure calls to */
	private final String URL = "https://api.random.org/json-rpc/1/invoke";
	/** The following members are the names of the basic methods available in the random.org api */
	private final String INTEGER_METHOD = "generateIntegers";
	private final String DECIMALFRACTION_METHOD = "generateDecimalFractions";
	private final String GAUSSIAN_METHOD = "generateGaussians";
	private final String STRING_METHOD = "generateStrings";
	private final String UUID_METHOD = "generateUUIDs";
	private final String GET_USAGE_METHOD = "getUsage";
	/** The HTTP content type for the requests */
	private final String CONTENT_TYPE = "application/json";	
	/** The default value for the optional replacement parameter */
	private final boolean REPLACEMENT_DEFAULT = true;
	private final int ONE_HOUR_IN_MILLIS = 3600000;
	
	private String mApiKey;
	private long mMaxBlockingTime = 3000;
	/** The request object to be sent to the server */
	private JsonObject mJSONRequest;
	/** The response object received from the server */
	private JsonObject mJSONResponse;
	/** The parameters supplied with the request object */
	private JsonObject mJSONParams;
	/** The time of the last received response */
	private long mLastResponseReceived;
	/** The advisory delay given by the random.org server */
	private long mAdvisoryDelay = 0;
	
	
	/**
	 * Creates a new RandomJSONRPC object with the given api key
	 * @param apiKey The api Key from random.org
	 */
	public RandomJSONRPC(String apiKey){
		mApiKey = apiKey;		
	}
	
	/**
	 * Creates a new RandomJSONRPC object with the given api key and the maximum time the user wants to wait for the server.
	 * @param apiKey The api key from random.org
	 * @param maxBlockingTime The longest amount of time (in milliseconds) that the user wants to wait for the server (default is 3 seconds). 
	 * This does not take into account the time it takes to send the request over the network. 
	 * Only the advisory delay given by the server is used. If the maxBlockingTime value is exceeded a RuntimeException will be thrown.
	 */
	public RandomJSONRPC(String apiKey, long maxBlockingTime){
		mApiKey = apiKey;
		mMaxBlockingTime = maxBlockingTime;		
	}
	
	/** <p><b> public int[] generateIntegers (int n, int min, int max) </b><p>
	 * Calls <code> generateIntegers(int n, int min, int max, true, 10) </code>
	 * @param n How many random integers are needed. Must be within the [1,1e4] range. 
	 * @param min The lower boundary for the range from which the random numbers will be picked. Must be within the [-1e9,1e9] range.
	 * @param max The upper boundary for the range from which the random numbers will be picked. Must be within the [-1e9,1e9] range.
	 * @return a set of random integers limited by the parameters listed above and generated by random.org
	 */
	public int[] generateIntegers (int n, int min, int max) {
		return generateIntegers(n, min, max, REPLACEMENT_DEFAULT);
	}
		
	/**
	 * <p><b> public int[] generateIntegers (int n, int min, int max, boolean replacement, int base) </b><p>
	 * Generates true random integers within a user-defined range.
	 * @param n How many random integers are needed. Must be within the [1,1e4] range. 
	 * @param min The lower boundary for the range from which the random numbers will be picked. Must be within the [-1e9,1e9] range.
	 * @param max The upper boundary for the range from which the random numbers will be picked. Must be within the [-1e9,1e9] range.
	 * @param replacement (default value true) Specifies whether the random numbers should be picked with replacement. 
	 * The default (true) will cause the numbers to be picked with replacement, i.e., the resulting numbers may contain duplicate values (like a series of dice rolls). 
	 * If unique numbers are needed (like raffle tickets drawn from a container), set this value to false.
	 * @param base (default value 10) Specifies the base that will be used to display the numbers. Values allowed are 2, 8, 10 and 16. 
	 * @return a set of random integers limited by the parameters listed above and generated by random.org
	 */
	public int[] generateIntegers(int n, int min, int max, boolean replacement) {
		mJSONParams = initIntegerParams(n, min, max, replacement);
		mJSONRequest = initMethod(INTEGER_METHOD);		
		sendRequest();			
		return extractInts();
	}
	
	/** 
	 * <p><b> public double[] generateDecimalFractions(int n, int decimalPlaces) </b><p>
	 * Calls <code> generateDecimalFractions(int n, int decimalPlaces, true) </code>
	 * @param n How many random decimal fractions are needed. Must be within the [1,1e4] range. 
	 * @param decimalPlaces The number of decimal places to use. Must be within the [1,20] range.
	 * @return a set of random integers limited by the parameters listed above and generated by random.org
	 */
	public double[] generateDecimalFractions(int n, int decimalPlaces) {
		return generateDecimalFractions(n, decimalPlaces, true);
	}
		
	/**
	 * <p><b> public double[] generateDecimalFractions(int n, int decimalPlaces, boolean replacement) </b><p>
	 * Generates true random decimal fractions from a uniform distribution across the [0,1] interval with a user-defined number of decimal places.
	 * @param n How many random decimal fractions are needed. Must be within the [1,1e4] range. 
	 * @param decimalPlaces The number of decimal places to use. Must be within the [1,20] range.
	 * @param replacement (default value true) Specifies whether the random numbers should be picked with replacement. 
	 * The default (true) will cause the numbers to be picked with replacement, i.e., the resulting numbers may contain duplicate values (like a series of dice rolls). 
	 * If unique numbers are needed (like raffle tickets drawn from a container), set this value to false.
	 * @return a set of random integers limited by the parameters listed above and generated by random.org
	 */
	public double[] generateDecimalFractions(int n, int decimalPlaces, boolean replacement) {
		mJSONParams = initDecimalFractionParams(n, decimalPlaces, replacement);
		mJSONRequest = initMethod(DECIMALFRACTION_METHOD);
		sendRequest();
		return extractDoubles();
	}
	
	/**
	 * <p><b> public double[] generateGaussians(int n, double mean, double standardDeviation, int significantDigits) </b><p>
	 * Generates true random numbers from a Gaussian distribution (also known as a normal distribution). 
	 * The form uses a Box-Muller Transform to generate the Gaussian distribution from uniformly distributed numbers. 
	 * @param n How many random Gaussian numbers are needed. Must be within the [1,1e4] range. 
	 * @param mean The distribution's mean. Must be within the [-1e6,1e6] range.
	 * @param standardDeviation The distribution's standard deviation. Must be within the [-1e6,1e6] range.
	 * @param significantDigits The number of significant digits to use. Must be within the [2,20] range.
	 * @return a set of random Gaussians limited by the parameters listed above and generated by random.org
	 */
	public double[] generateGaussians(int n, double mean, double standardDeviation, int significantDigits) {		
		mJSONParams = initGaussiansParams(significantDigits, mean, standardDeviation, significantDigits);
		mJSONRequest = initMethod(GAUSSIAN_METHOD);
		sendRequest();
		return extractDoubles();
	}
	
	/** 
	 * <p><b> public String[] generateStrings(int n, String characters) </b><p>
	 * Calls <code> generateStrings(int n, int decimalPlaces, true) </code>
	 * @param n How many strings are needed. Must be within the [1,1e4] range. 
	 * @param length The length of each string. Must be within the [1,20] range. All strings will be of the same length.
	 * @param characters A string that contains the set of characters that are allowed to occur in the random strings. The maximum number of characters is 80.
	 * @return a set of random strings limited by the parameters listed above and generated by random.org
	 */
	public String[] generateStrings(int n, int length, String characters) {
		return generateStrings(n, length, characters, true);
	}
	
	/**
	 * <p><b> public String[] generateStrings(int n, int length, String characters, boolean replacement) </b><p>
	 * Generates true random strings. 
	 * @param n How many strings are needed. Must be within the [1,1e4] range. 
	 * @param length The length of each string. Must be within the [1,20] range. All strings will be of the same length.
	 * @param characters A string that contains the set of characters that are allowed to occur in the random strings. The maximum number of characters is 80.
	 * @param replacement (default value true) Specifies whether the random strings should be picked with replacement. 
	 * The default (true) will cause the strings to be picked with replacement, i.e., the resulting strings may contain duplicate (like a series of dice rolls). 
	 * If unique numbers are needed (like raffle tickets drawn from a container), set this value to false.
	 * @return a set of random strings limited by the parameters listed above and generated by random.org
	 */
	public String[] generateStrings(int n, int length, String characters, boolean replacement) {
		mJSONParams = initStringParams(n, length, characters, replacement);
		mJSONRequest = initMethod(STRING_METHOD);
		sendRequest();
		return extractStrings();
	}
	
	/**
	 * <p><b> public UUID[] generateUUIDs(int n) </b><p>
	 * Generates version 4 true random Universally Unique IDentifiers (UUIDs) in accordance with section 4.4 of RFC 4122. 
	 * @param n How many UUIDs are needed. Must be within the [1,1e3] range. 
	 * @return a set of random UUIDS limited by the parameters listed above and generated by random.org
	 */
	public UUID[] generateUUIDs(int n){
		mJSONParams = initUUIDParams(n);
		mJSONRequest = initMethod(UUID_METHOD);
		sendRequest();
		return extractUUIDs();
	}
		
	/**
	 * <p><b> public int getRequestsLeft() </b><p>
	 * Returns the number of requests left on the quota
	 * @return The number of remaining requests
	 */
	public int getRequestsLeft(){
		if(mJSONResponse == null || System.currentTimeMillis() > mLastResponseReceived + ONE_HOUR_IN_MILLIS)
			getUsage();		
		JsonObject resultObject = (JsonObject) mJSONResponse.get("result");
		return resultObject.get("requestsLeft").getAsInt();  
	}
	
	/**
	 * <p><b> public int getBitsLeft() </b><p>
	 * Returns the number of bits left on the quota
	 * @return The number of remaining bits
	 */
	public int getBitsLeft() {		
		if(mJSONResponse == null || System.currentTimeMillis() > mLastResponseReceived + ONE_HOUR_IN_MILLIS)
			getUsage();		
		JsonObject resultObject = (JsonObject) mJSONResponse.get("result");
		return resultObject.get("bitsLeft").getAsInt();
	}
	
	private JsonObject getUsage(){
		mJSONParams = new JsonObject();
		mJSONParams.addProperty("apiKey", mApiKey);
		mJSONRequest = initMethod(GET_USAGE_METHOD);
		sendRequest();
		return mJSONResponse;
	}
	
	/**
	 * Initialise the parameters to put in the JSON request object for integer generation
	 * @return An initialised JSON object holding the parameters necessary to generate integers 
	 */
	private JsonObject initIntegerParams(int n, int min, int max, boolean replacement) {		  
		mJSONParams = new JsonObject();
		mJSONParams.addProperty("apiKey", mApiKey);
		mJSONParams.addProperty("n", n);
		mJSONParams.addProperty("min", min);
		mJSONParams.addProperty("max", max);
		mJSONParams.addProperty("replacement", replacement);
		return mJSONParams;
	}
	
	/**
	 * Initialise the parameters to put in the JSON request object for decimal fraction generation
	 * @return An initialised JSON object holding the parameters necessary to generate decimal fractions
	 */
	private JsonObject initDecimalFractionParams(int n, int decimalPlaces, boolean replacement) {		  
		mJSONParams = new JsonObject();
		mJSONParams.addProperty("apiKey", mApiKey);
		mJSONParams.addProperty("n", n);
		mJSONParams.addProperty("replacement", replacement);
		return mJSONParams;
	}
	
	/**
	 * Initialise the parameters to put in the JSON request object for Gaussian generation
	 * @return An initialised JSON object holding the parameters necessary to generate Gaussians
	 */
	private JsonObject initGaussiansParams(int n, double mean, double standardDeviation, int significantDigits) {		  
		mJSONParams = new JsonObject();
		mJSONParams.addProperty("apiKey", mApiKey);
		mJSONParams.addProperty("n", n);
		mJSONParams.addProperty("mean", mean);
		mJSONParams.addProperty("standardDeviation", standardDeviation);
		mJSONParams.addProperty("significantDigits", significantDigits);
		return mJSONParams;
	}
	
	/**
	 * Initialise the parameters to put in the JSON request object for string generation
	 * @return An initialised JSON object holding the parameters necessary to generate strings
	 */
	private JsonObject initStringParams(int n, int length, String characters, boolean replacement) {		  
		mJSONParams = new JsonObject();
		mJSONParams.addProperty("apiKey", mApiKey);
		mJSONParams.addProperty("n", n);
		mJSONParams.addProperty("length", length);
		mJSONParams.addProperty("characters", characters);
		mJSONParams.addProperty("replacement", replacement);
		return mJSONParams;
	}
	
	/**
	 * Initialise the parameters to put in the JSON request object for UUID generation
	 * @return An initialised JSON object holding the parameters necessary to generate UUIDs
	 */
	private JsonObject initUUIDParams(int n) {		  
		mJSONParams = new JsonObject();
		mJSONParams.addProperty("apiKey", mApiKey);
		mJSONParams.addProperty("n", n);
		return mJSONParams;
	}
	
	/**
	 * Initialise the JSON object representing the request to be sent over the network
	 * @param method The name of the method to be invoked on the server
	 * @return An initialised JSON object holding the fields that the api methods use
	 */
	private JsonObject initMethod(String method) {						  
		mJSONRequest = new JsonObject();
		mJSONRequest.addProperty("jsonrpc", "2.0");
		mJSONRequest.addProperty("method", method);
		mJSONRequest.add("params", mJSONParams);
		mJSONRequest.addProperty("id", new Random().nextInt()); 		
		return mJSONRequest;
	}
	
	/**
	 * Wait for advisory delay and make the call to the method that does the actual networking. 
	 * This call is done in it's own runnable to avoid networking on the main thread. 
	 * This method also handles all the checked exceptions that the client has no use of. 
	 * Note that the actual networking is done on a separate thread. This is done to allow the code to run on Android,
	 * since the Android platform disallows networking to be done on the main thread.
	 * @return The JSON response object from random.org
	 */
	protected void sendRequest(){
		mJSONResponse = null;
		long timeSinceLastRequest = System.currentTimeMillis() - mLastResponseReceived;
		long waitingTime = mAdvisoryDelay - timeSinceLastRequest;
		if (waitingTime > 0){
			//in the waiting state
			if(waitingTime > mMaxBlockingTime){
				//if the waiting time advised by random.org is larger than the time the user wants to wait, throw an exception
				throw new RuntimeException("The advised waiting is higher than the max accepted value");
			}
			try {
				// Wait for the advised amount of time
				Thread.sleep(waitingTime);
			} 
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
		new Thread(new Runnable() {			
			@Override
			public void run() {
				try {
					//calls to parser and connect methods
					mJSONResponse = parseHTTPResponse(doPost());					
				} 
				catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				} 
				catch (IllegalStateException e) {
					throw new RuntimeException(e);
				} 
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}).start();
		//call the guard to make sure nothing is returned until the remote procedure call has returned
		guardResponse();
		//store the time when the response is received (unless the response is and error or the response of a getUsage request)
		if(((JsonObject) mJSONResponse.get("result")).has("advisoryDelay"))
			mLastResponseReceived = System.currentTimeMillis();
	}
	
	/** 
	 * Implementation of a concurrency guard to make sure execution of the generate methods does not continue 
	 * to their return statements before the HTTP request has returned (i.e. before mJSONResponse has a non null value)
	 * Also calls the error checking method and stores the adVisoryDelay returned from the server
	 */
	private void guardResponse() {
		while(mJSONResponse == null)
			try {
				Thread.sleep(50);
			} 
			catch (InterruptedException e) {
				e.printStackTrace();
			}			
		//check if the response contains an error object 
		errorCheck();	
		//if no error object was found and we are can access the advisoryDelay field, then store the value of the advisoryDelay field.
		//Note that is we are performing a getUsage call there will not be an AdvisoryDelay field.
		if(((JsonObject) mJSONResponse.get("result")).has("advisoryDelay"))
			mAdvisoryDelay = ((JsonObject) mJSONResponse.get("result")).get("advisoryDelay").getAsLong();
	}
	
	/**
	 * Check if an error occurred and in that case throw the appropriate exception
	 * @param json The JSON response object from the server
	 */	
	private void errorCheck() {		
		JsonObject error;		
		if(!mJSONResponse.has("error"))
			return;
		else
			 error = (JsonObject) mJSONResponse.get("error");		
		int errorCode = error.get("code").getAsInt();
		String message = error.get("message").getAsString();		
		//the cases where an illegal argument has been supplied by the user
		if (errorCode == 200 || errorCode == 201 || errorCode == 202 || errorCode == 203 || errorCode == 300 || errorCode == 301 || errorCode == 301 || errorCode == 400 || errorCode == 401)
			throw new IllegalArgumentException("Code: " + String.valueOf(errorCode) + ". Message: " + message);
		//the case where an unknown error occurred, or an error that has nothing to do with the parameters supplied by the client occurred
		throw new RuntimeException("Code: " + String.valueOf(errorCode) + ". Message" + message); 
	}
	
	/**
	 * Do the actual connect() call to to open the connection and send the data over the network
	 * @return The HTTPResponse object from the server
	 * @throws IOException 
	 */
	private BufferedReader doPost() throws IOException {
		//create a connection object and connect to the server
		HttpsURLConnection con = createRequest();
		con.connect();
		//return the stream from the response
		return new BufferedReader(new InputStreamReader(con.getInputStream()));
	}
	
	/**
	 * Create a connection object for communicating with the random.org server
	 * @return The HttpsURLConnection object with the correct settings for sending the JSONRPC requests
	 */
	private HttpsURLConnection createRequest () throws IOException {
		HttpsURLConnection con = (HttpsURLConnection) new URL(URL).openConnection();
		//set HTTP properties
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", CONTENT_TYPE); 
		con.setDoOutput(true); 
		con.setDoInput(true);
		DataOutputStream out = new DataOutputStream(con.getOutputStream());
		//write the JSON request object to the output stream
		out.write(mJSONRequest.toString().getBytes());
		return con;
	}
	
	/**
	 * Parse the data from the remote procedure call response
	 * @param buffer The buffer with the response stream
	 * @return The JSON object containing the response
	 */
	private JsonObject parseHTTPResponse(BufferedReader buffer) throws UnsupportedEncodingException, IllegalStateException, IOException{
		String content = "";
		String line;
		while ((line = buffer.readLine()) != null) 
			content += line;
		buffer.close(); 
		return new JsonParser().parse(content).getAsJsonObject();
	}
	
	/**
	 * Extract integers from the JSON response object
	 * @return An array containing the integers
	 */
	private int[] extractInts() {
		JsonArray dataArray = unwrapJSONResponse();
		int length = dataArray.size();
		int i = 0;
		int[] result = new int[length];
		while (i < length){
			result[i] = (Integer) dataArray.get(i).getAsInt();
			i++;
		}
		return result;
	}
	
	/**
	 * Extract doubles from the JSON response object
	 * @return An array containing the doubles
	 */
	private double[] extractDoubles() {
		JsonArray dataArray = unwrapJSONResponse();
		int length = dataArray.size();
		int i = 0;
		double[] result = new double[length];
		while (i < length){
			result[i] = (Double) dataArray.get(i).getAsDouble();
			i++;
		}
		return result;
	}
	
	/**
	 * Extract strings from the JSON response object
	 * @return An array containing the strings
	 */
	private String[] extractStrings() {
		JsonArray dataArray = unwrapJSONResponse();
		int length = dataArray.size();
		int i = 0;
		String[] result = new String[length];
		while (i < length){
			result[i] = (String) dataArray.get(i).getAsString();
			i++;
		}
		return result;
	}
	
	/**
	 * Extract UUIDs from the JSON response object
	 * @return An array containing the UUIDs
	 */
	private UUID[] extractUUIDs() {
		JsonArray dataArray = unwrapJSONResponse();
		int length = dataArray.size();
		int i = 0;
		UUID[] result = new UUID[length];
		while (i < length){
			result[i] = UUID.fromString ((String) dataArray.get(i).getAsString());
			i++;
		}
		return result;
	}
	
	/**
	 * Unwrap the data from inside the result and random fields
	 * @return The JSOn object with the data
	 */
	private JsonArray unwrapJSONResponse() {		
		JsonObject resultObject = (JsonObject) mJSONResponse.get("result");		
		JsonObject randomObject = (JsonObject) resultObject.get("random");
		return randomObject.getAsJsonArray("data");		
	}
	
}
