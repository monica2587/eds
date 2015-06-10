package com.search.eds.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CsvVariableExtractor {
	
	public List<String> extractVariables(String fileUrl) throws IOException {
		if(fileUrl.startsWith("https://")) {
			//https is causing sun.security.validator.ValidatorException due to certificate issues with data portal
			fileUrl = fileUrl.replace("https://", "http://");
		}
		URL url = new URL(fileUrl);
		//check to ensure that the file exists
		if(url.getProtocol().equalsIgnoreCase("http")) {
			HttpURLConnection huc = (HttpURLConnection) url.openConnection();
		    int responseCode = huc.getResponseCode();
		    if(responseCode != HttpURLConnection.HTTP_OK) return null;
		}
	    
		//Store words in line after split in list
		List<String> wordsInLine = null;
		
		//Create reader and read line by line
		BufferedReader in = null;
		try{
			in = new BufferedReader(new InputStreamReader(url.openStream()));
			
			int countWordsInLine = 0;
			do {
				wordsInLine = parseLine(in);
				countWordsInLine = wordsInLine.size();
				//remove empty strings in the list before checking count
				for (String word : wordsInLine) {
					if(word.trim().length() == 0) 
						countWordsInLine--;
				}
			} while(wordsInLine != null && countWordsInLine <= 1);
		} finally {
			if(in != null){
				in.close();
			}
		}
		return wordsInLine;
	}
	
	/*
	 * Copied the following code from 
	 * http://agiletribe.wordpress.com/2012/11/23/the-only-class-you-need-for-csv-files/
	 * 
	 */
	private List<String> parseLine(Reader r) throws IOException {
		int ch = r.read();
		while (ch == '\r') {
			ch = r.read();
		}
		if (ch < 0) {
			return null;
		}
		List<String> store = new ArrayList<String>();
		StringBuffer curVal = new StringBuffer();
		boolean inquotes = false;
		boolean started = false;
		while (ch >= 0) {
			if (inquotes) {
				started = true;
				if (ch == '\"') {
					inquotes = false;
				} else {
					curVal.append((char) ch);
				}
			} else {
				if (ch == '\"') {
					inquotes = true;
					if (started) {
						// if this is the second quote in a value, add a quote
						// this is for the double quote in the middle of a value
						curVal.append('\"');
					}
				} else if (ch == ',') {
					store.add(curVal.toString());
					curVal = new StringBuffer();
					started = false;
				} else if (ch == '\r') {
					// ignore LF characters
				} else if (ch == '\n') {
					// end of a line, break out
					break;
				} else {
					curVal.append((char) ch);
				}
			}
			ch = r.read();
		}
		store.add(curVal.toString());
		return store;
	}
}
