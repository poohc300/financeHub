package com.example.financeHub.config;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {
    
    public static String generateHash(String input) {
	try {
	    
	    // sha 256
	    MessageDigest digest = MessageDigest.getInstance("SHA-256");
	    byte[] hashBytes = digest.digest(input.getBytes());
	    StringBuilder hexString = new StringBuilder();
	    
	    for(byte b : hashBytes) {
		hexString.append(String.format("%02x", b));
	    }
	    
	    return hexString.toString();
	} catch (NoSuchAlgorithmException e) {
	    // TODO: handle exception
	    throw new RuntimeException("Error generating hash", e);
	}
    }

}
