package com.cloudera.ctakes;

import java.util.ArrayList;
import java.util.List;

public class FileSplitter {
	public static List<String> getLines(String contents, int maxSize) {
		List<String> lst = new ArrayList<>();
		String lines[] = contents.split("\\r?\\n");
		int counter = 0;
		
		String fSplit = "";
		for(String l:lines) {
			fSplit = fSplit + l + "\n";
			counter = counter + l.length();
			if(counter>maxSize) {
				lst.add(fSplit);
				counter = 0;
				fSplit="";
			}
			
		}
		
		if(counter>0) {
			lst.add(fSplit);
		}
		return lst;
	}
	
	public static void main(String[] args) {
		String l = "This is a line\n This is second line\n This is third line \n";
		System.out.print(l);
		List<String> ls = getLines(l,20);
		for(String p: ls) {
			System.err.println(p);
			System.err.println("---");
		}
	}
}
