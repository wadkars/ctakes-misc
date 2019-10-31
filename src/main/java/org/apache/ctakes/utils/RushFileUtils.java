package org.apache.ctakes.utils;

public class RushFileUtils {
	public static String getEncounterId(String fileName) {
		int st = fileName.lastIndexOf("/");
		int end = fileName.lastIndexOf(".");
		return fileName.substring(st+1, end);
		
	}
	public static void main(String[] args) {
		String f = "hdfs://rudu-cldmst001.rush.edu:8020/user/dpugazhe/2017_NOTES/Notes_2017_1/16127120020.txt";
		String encounterId = getEncounterId(f);
		System.err.println(encounterId);
	}
	
	
}
