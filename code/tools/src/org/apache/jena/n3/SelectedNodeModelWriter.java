package org.apache.jena.n3;


import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.util.iterator.Filter;
import org.apache.jena.util.iterator.Map1;

public class SelectedNodeModelWriter extends N3JenaWriterPP {

	private List<Resource> lr = new ArrayList<Resource>();
	private boolean skipPrintBlankNode = true;
	private OutputStream os;

	public boolean isSkipWritingPrefixes() {
		return skipWritingPrefixes;
	}

	public void setSkipWritingPrefixes(boolean skipWritingPrefixes) {
		this.skipWritingPrefixes = skipWritingPrefixes;
	}

	private boolean skipWritingPrefixes = true;

	public SelectedNodeModelWriter() {
		super();
	}

	public SelectedNodeModelWriter(List<Resource> lr) {
		super();
		this.lr = lr;
	}

	// skip printing a subject if not in the list
	// or is blanknode depending on skipPrintBlankNode property
	@Override
	protected boolean skipThisSubject(Resource r) {

		boolean b1 = !lr.contains(r);
		boolean b2 = r.isAnon();
		return b1 || b2 && isSkipPrintBlankNode();
	}

	// a sorted list iterator
	// NOT REACHED??
	@Override
	protected Iterator<RDFNode> rdfListIterator(Resource r) {
		Set<RDFNode> set = new TreeSet<RDFNode>();
		for (Iterator<RDFNode> iter = super.rdfListIterator(r); iter.hasNext();) {
			RDFNode node = iter.next();
			set.add(node);
		}

		return set.iterator();
	}

	/**
	 * Only differences with Jena version are the two TreeSet assignments to
	 * achieve ordering, plus the comparator definition
	 */
	@Override
	protected void writeObjectList(Resource subject, Property property) {

		String propStr = formatProperty(property);

		// Find which objects are simple (i.e. not nested structures)

		StmtIterator sIter = subject.listProperties(property);
		@SuppressWarnings("unchecked")
		Set<RDFNode> simple = new TreeSet<RDFNode>(new Comparator() {

			public int compare(Object o1, Object o2) {
				// just compare string representations
				return o1.toString().compareTo(o2.toString());
			}

		});
		@SuppressWarnings("unchecked")
		Set<RDFNode> complex = new TreeSet<RDFNode>(new Comparator() {

			public int compare(Object o1, Object o2) {
				// just compare string representations
				return o1.toString().compareTo(o2.toString());
			}

		});

		for (; sIter.hasNext();) {
			Statement stmt = sIter.nextStatement();
			RDFNode obj = stmt.getObject();
			if (isSimpleObject(obj))
				simple.add(obj);
			else
				complex.add(obj);
		}
		sIter.close();
		// DEBUG
		int simpleSize = simple.size();
		int complexSize = complex.size();

		// Write property/simple objects

		if (simple.size() > 0) {
			String padSp = null;
			// Simple objects - allow property to be long and alignment to be
			// lost
			if ((propStr.length() + minGap) <= widePropertyLen)
				padSp = pad(calcPropertyPadding(propStr));

			if (doObjectListsAsLists) {
				// Write all simple objects as one list.
				out.print(propStr);
				out.incIndent(indentObject);

				if (padSp != null)
					out.print(padSp);
				else
					out.println();

				for (Iterator<RDFNode> iter = simple.iterator(); iter.hasNext();) {
					RDFNode n = iter.next();
					writeObject(n);

					// As an object list
					if (iter.hasNext())
						out.print(objectListSep);
				}

				out.decIndent(indentObject);
			} else {
				for (Iterator<RDFNode> iter = simple.iterator(); iter.hasNext();) {
					// This is also the same as the complex case
					// except the width the property can go in is different.
					out.print(propStr);
					out.incIndent(indentObject);
					if (padSp != null)
						out.print(padSp);
					else
						out.println();

					RDFNode n = iter.next();
					writeObject(n);
					out.decIndent(indentObject);

					// As an object list
					if (iter.hasNext())
						out.println(" ;");
				}

			}
		}
		// Now do complex objects.
		// Write property each time for a complex object.
		// Do not allow over long properties but same line objects.

		if (complex.size() > 0) {
			// Finish the simple list if there was one
			if (simple.size() > 0)
				out.println(" ;");

			int padding = -1;
			String padSp = null;

			// Can we fit teh start of teh complex object on this line?

			// DEBUG variable.
			int tmp = propStr.length();
			// Complex objects - do not allow property to be long and alignment
			// to be lost
			if ((propStr.length() + minGap) <= propertyCol) {
				padding = calcPropertyPadding(propStr);
				padSp = pad(padding);
			}

			for (Iterator<RDFNode> iter = complex.iterator(); iter.hasNext();) {
				int thisIndent = indentObject;
				// if ( i )
				out.incIndent(thisIndent);
				out.print(propStr);
				if (padSp != null)
					out.print(padSp);
				else
					out.println();

				RDFNode n = iter.next();
				writeObject(n);
				out.decIndent(thisIndent);
				if (iter.hasNext())
					out.println(" ;");
			}
		}
		return;
	}

	/**
	 * Write the selected nodes from the model
	 * 
	 * @param model
	 * @param output
	 * @param lr
	 */
	public synchronized void write(Model model, OutputStream output,
			List<Resource> lr) {
		this.lr = lr;
		this.os = output;
		write(model, output, (String) null);

	}
	
	// a little heavy handed but it works
	protected int comparePropertiesObjects(Resource r1,Resource r2){
		@SuppressWarnings("unchecked")
		Set<String> set1 = new TreeSet<String>(new Comparator() {

			public int compare(Object o1, Object o2) {
				// just compare string representations
				return o1.toString().compareTo(o2.toString());
			}

		});
		@SuppressWarnings("unchecked")
		Set<String> set2 = new TreeSet<String>(new Comparator() {

			public int compare(Object o1, Object o2) {
				// just compare string representations
				return o1.toString().compareTo(o2.toString());
			}

		});

		StmtIterator sIter1 = r1.listProperties();
		for (; sIter1.hasNext();) {
			Statement stmt = sIter1.nextStatement();
			Property p = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			set1.add(p.toString()+" "+obj.toString());
		}
		sIter1.close();

		StmtIterator sIter2 = r2.listProperties();
		for (; sIter2.hasNext();) {
			Statement stmt = sIter2.nextStatement();
			Property p = stmt.getPredicate();
			RDFNode obj = stmt.getObject();
			set2.add(p.toString()+" "+obj.toString());
		}
		sIter2.close();
		
		String s1 = "";
		String s2 = "";
		
		for (String s : set1){
			s1 += s;
		}
		for (String s : set2){
			s2 += s;
		}
		
		return s1.compareTo(s2);
	}


	@SuppressWarnings("unchecked")
	protected ResIterator listSubjects(Model model) {
		if (lr == null) {
			lr = model.listSubjects().toList();
			Collections.sort(lr, new Comparator() {

				public int compare(Object o1, Object o2) {
					Resource r1 = (Resource) o1;
					Resource r2 = (Resource) o2;
					
					// handle blank nodes
					if (r1.isAnon() && r2.isAnon()){
						return comparePropertiesObjects(r1,r2);
					}
					return r1.toString().compareTo(r2.toString());
				}

			});
		}
		
		final Iterator it = lr.iterator();
		return new ResIterator() {

			public Resource removeNext() {
				// TODO Auto-generated method stub
				return null;
			}

			public <X extends Resource> ExtendedIterator<Resource> andThen(
					Iterator<X> other) {
				// TODO Auto-generated method stub
				return null;
			}

			public ExtendedIterator<Resource> filterKeep(Filter<Resource> f) {
				// TODO Auto-generated method stub
				return null;
			}

			public ExtendedIterator<Resource> filterDrop(Filter<Resource> f) {
				// TODO Auto-generated method stub
				return null;
			}

			public <U> ExtendedIterator<U> mapWith(Map1<Resource, U> map1) {
				// TODO Auto-generated method stub
				return null;
			}

			public List<Resource> toList() {
				// TODO Auto-generated method stub
				return null;
			}

			public Set<Resource> toSet() {
				// TODO Auto-generated method stub
				return null;
			}

			public void close() {
				// TODO Auto-generated method stub
			}

			public boolean hasNext() {
				return it.hasNext();
			}

			public Resource next() {
				return (Resource) it.next();
			}

			public void remove() {
				// TODO Auto-generated method stub

			}

			public Resource nextResource() {
				return (Resource) it.next();
			}

			@Override
			public ExtendedIterator<Resource> filterDrop(
					Predicate<Resource> arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public ExtendedIterator<Resource> filterKeep(
					Predicate<Resource> arg0) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public <U> ExtendedIterator<U> mapWith(Function<Resource, U> arg0) {
				// TODO Auto-generated method stub
				return null;
			}

		};
	}

	// avoid writing prefixes here
	@Override
	protected void writePrefixes(Model model) {
		if (!this.isSkipWritingPrefixes())
			printPrefixes(model, os);
	}

	// avoid writing blank nodes here
	@Override
	protected void finishWriting() {
	}

	public void setSkipPrintBlankNode(boolean skipPrintBlankNode) {
		this.skipPrintBlankNode = skipPrintBlankNode;
	}

	public boolean isSkipPrintBlankNode() {
		return skipPrintBlankNode;
	}

	/**
	 * Print sorted prefixes for the model. Borrowed from jena's
	 * N3JenaWriterCommon
	 * 
	 * @param m
	 *            the model
	 * @param os
	 *            the outputStream
	 */
	public static void printPrefixes(Model m, OutputStream os) {

		PrintWriter out = new PrintWriter(os);
		Map<String, String> prefixMap = m.getNsPrefixMap();
		List<String> keys = new ArrayList<String>(prefixMap.keySet());
		Collections.sort(keys);
		for (String p : keys) {
			String u = prefixMap.get(p);

			String tmp = "@prefix " + p + ": ";
			out.print(tmp);
			out.print(pad(16 - tmp.length()));
			// NB Starts with a space to ensure a gap.
			out.println(" <" + u + "> .");
		}
		out.flush();
	}

}
