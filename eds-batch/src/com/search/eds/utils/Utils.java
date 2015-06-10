package com.search.eds.utils;

public class Utils {
	public static String convertToSentence(String str){
		str = str.toLowerCase();
		str = str.replaceAll("_", " ");
		str = str.replaceAll("-", " ");
		return Character.toUpperCase(str.charAt(0)) + str.substring(1);
	}
	
	public static String canonicalizeOrg(String str){
		if(str.equalsIgnoreCase("City of Surrey")) return str;
		else if(str.equalsIgnoreCase("City of Winnipeg")) return str;
		else if(str.indexOf(" | ") > 0) return str.substring(0, str.indexOf(" | ")).trim();
		else return "City of Edmonton";
	}
}
