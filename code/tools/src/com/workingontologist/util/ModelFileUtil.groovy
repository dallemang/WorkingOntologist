package com.workingontologist.util

import java.io.OutputStream;
import java.util.Comparator;

import org.apache.jena.n3.SelectedNodeModelWriter
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement

public class ModelFileUtil {
	/**
	 * Write model ordered.
	 *
	 * @param m the m
	 * @param os the os
	 * @param headerComments the header comments
	 */
	public static void writeModelOrdered(Model m, OutputStream os,
			String[] headerComments) {

		SelectedNodeModelWriter smw = new SelectedNodeModelWriter();
		smw.setSkipPrintBlankNode(false);
		smw.setSkipWritingPrefixes(false);
		writeHeader(os, headerComments);
		smw.write(m, os, null);
	}
			
		/**
		 * Write header.
		 *
		 * @param os the os
		 * @param comments the comments
		 */
		protected static void writeHeader(OutputStream os, String[] comments) {
			PrintStream ps = new PrintStream(os);
			if (comments != null)
				for (String s : comments) {
					ps.println("# " + s);
				}
			ps.println(""); // add blank line
			ps.flush();
		}
	
		public static boolean compare(file1, file2){
			def t1 = new File(file1).text
			def t2 = new File(file2).text
			return compareStrings(t1,t2)
		}
		
		public static boolean compareStrings(t10, t20){
			def t1 = t10.replaceAll("[\t \n\r]+", "")
			def t2 = t20.replaceAll("[\t \n\r]+", "")
			boolean b = t1 == t2
			return b
		}
}
