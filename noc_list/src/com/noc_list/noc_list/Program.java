package com.noc_list.noc_list;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This program was written for Adhoc and retrieves and outputs the json data using SHA256
 *
 */
public class Program {

	private static final String AUTH_URL = "http://localhost:8888/auth";
	private static final String USERS_URL = "http://localhost:8888/users";
	private static final String AUTH_HEADER = "Badsec-Authentication-Token";
	private static final String USERS_HEADER = "X-Request-Checksum";
	
	public static void main(String[] args) {
		// Get the token, convert it, and get the users
		Tuple<Integer, String> result = getToken();
		
		if(200 == result.tuple1) {
			result.tuple2 += "/users";
			String hash = HashIt(result.tuple2);
			if(hash.isEmpty()) {
				// We had a problem
				result.tuple1 = 1;
			}
			else {
				result = getUsers(hash);
			}
		}
		
		System.out.print(result.tuple2);
		// If our response code is 200, then we're good.  Otherwise, not so much
		System.exit(result.tuple1 == 200 ? 0 : result.tuple1);
	}
	
	/**
	 * Get the authentication token
	 * @return the token
	 */
	private static Tuple<Integer, String> getToken() {
		Tuple<Integer, String> result = new Tuple<Integer, String>();
		result = GETRequest(AUTH_URL, AUTH_HEADER, "", "");
		return result;
	}

	/**
	 * Get the users
	 * @return the users
	 */
	private static Tuple<Integer, String> getUsers(String hash) {
		Tuple<Integer, String> result = new Tuple<Integer, String>();
		result = GETRequest(USERS_URL, "", USERS_HEADER, hash);
		return result;
	}
		
	/**
	 * Hash the data sent to us
	 * @param token - Data to convert
	 * @return The hashed token
	 */
	private static String HashIt(String token) {
		StringBuffer hexString = new StringBuffer();
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			
			// Convert to hex
		    for (int i = 0; i < hash.length; i++) {
			    String hex = Integer.toHexString(0xff & hash[i]);
			    if(hex.length() == 1) {
			    	hexString.append('0');
			    }
			    hexString.append(hex);
		    }
		} catch (NoSuchAlgorithmException e) {
			System.err.println(e.getStackTrace());
			hexString.setLength(0);
		}
	    return hexString.toString();		
	}

	/**
	 * Make the request
	 * @param url - URL for us to call
	 * @param tokenHeader - Optional; If not blank, we will only read this data
	 * @param header - Optional; if not blank, we will set this value and read the data from the body
	 * @param inputData - Set this data only if there's a header
	 * @return Tuple with response code and data
	 */
	private static Tuple<Integer, String> GETRequest(String url, String tokenHeader, String header, String inputData) {
		
		Tuple<Integer, String> result = new Tuple<Integer, String>(1, "");	
		int responseCode = 0;
		HttpURLConnection connection = null;
		
		try {
			URL urlForGetRequest = new URL(url);
		    String readLine = null;
		    connection = (HttpURLConnection) urlForGetRequest.openConnection();
		    connection.setRequestMethod("GET");
		    if(!header.isEmpty()) {
			    connection.setRequestProperty(header, inputData);
		    }
		    
		    // We're going to try up 3 times and give up after that
		    responseCode = connection.getResponseCode();
		    if(responseCode != HttpURLConnection.HTTP_OK) {
		    	responseCode = connection.getResponseCode();
		    	if(responseCode != HttpURLConnection.HTTP_OK) {
		    		responseCode = connection.getResponseCode();
		    	}
		    }
		    
		    if (responseCode == HttpURLConnection.HTTP_OK) {
		    	// If they passed us a tokenHeader, then we will get the value from the tokenHeader.
		    	// Otherwise, we will get it from the body
		    	if(!tokenHeader.isEmpty()) {
			    	result.tuple2 = connection.getHeaderField(tokenHeader);
		    	}
		    	else {
		    		// Automatically closes everything for us
			        try(BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				        StringBuilder data = new StringBuilder("[");
				        while ((readLine = in.readLine()) != null) {
				            data.append("\"");
				            data.append(readLine);
				            data.append("\", ");
				        }
				        
				        // This will remove the trailing ", "
				        data.delete(data.length() - 2, data.length());
				        data.append("]");
				        result.tuple2 = data.toString();
			        }
			        catch(IOException e) {
			        	// Just throw it and the other catch will take care of it for us
			        	throw e;
			        }
		    	}
		        result.tuple1 = HttpURLConnection.HTTP_OK;
		    }

		} catch (IOException e) {
			System.err.println(e.getStackTrace());
			result.tuple1 = 0;
			result.tuple2 = "";
		}
		finally {
			if(null != connection) {
				connection.disconnect();
			}
		}
		
	    return result;
	}	
}