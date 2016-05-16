package com.workingontologist.rdf;

import static org.junit.Assert.*;
import com.workingontologist.util.*;
import groovy.json.JsonBuilder
import org.apache.jena.util.FileUtils ;
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.AnonId
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.expr.ExprEvalException;
import org.apache.jena.sparql.expr.ExprList;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.expr.aggregate.Accumulator;
import org.apache.jena.sparql.expr.aggregate.AccumulatorFactory;
import org.apache.jena.sparql.expr.aggregate.AggCustom;
import org.apache.jena.sparql.expr.aggregate.AggregateRegistry;
import org.apache.jena.sparql.function.FunctionEnv;
import org.apache.jena.sparql.graph.NodeConst;

import org.junit.Test;

/*
 * Design/Use Case/Requirements
 * 
 * 	dmap:map	->	rl:map
 fix up the domain expression where needed with OR expressions
 MapCreateParentReln	->	MapRelation
 MapCreateVocabReln ->	MapInstance
 rl:Map should handle src and tgt when both have the range of DatatypeProerty or ObjectProperty
 use of generic bag for multi-way relations
 rl:tgt must be data and object range
 MapRelation has a rl:reln string path w/r.t. homeUri.  Not a URI as with rl:Map
 rl:Map of data could need to move multiple instances (as it does with object)
 Episode number composition handling (variables?
 * 
 * the rl:expr takes any SPARQL Expression which can appear on the LHS of a Bind statement
 * 
 * Careful of maps duplicated on src, they can mask the dup
 * 
 * _ must have a MapUri for every new type
 * _ must guids supporting MapUri
 */
class MapRdfUnitTest extends MapRdfUnitBase {

	static boolean Verbose = true
	static {
		MapRdf.Verbose = false
	}
	
	@Test
	public void test() {
		
		def mins = """
<http://example.com/term#00s1>
  rdf:type ec:Series ;
  ech:title "Series"@en ;
  er:hasChar30 <http://example.com/term#00c1> ;
  some-core:guid "00s1" ;
.
<http://example.com/term#00c1>
  rdf:type ec:Character ;
  en:desc30 "Recurring thought (27 September 2014)"@en ;
  some-core:guid "00c1" ;
.
"""
		def mms = """
ec:Series
	rl:map 
		[rl:type cwr:Rendition ; a rl:Create] ,
		[rl:src some-core:guid ; rl:format "z%s" ; rl:namespace <http://data.widget.com/resource#> ; a rl:MapUri ] ,
	    [rl:src er:hasChar30 ; rl:tgt cwr:hasChar ; a rl:Map ] 
		.
ec:Character
	rl:map 
		[rl:type cwr:Character ; a rl:Create ] ,
		[rl:src some-core:guid ; rl:format "z%s" ; rl:namespace <http://data.widget.com/resource#> ; a rl:MapUri ] ,
    	[rl:src en:desc30 ; rl:tgt  skos:definition ; a rl:Map  ] 
		.
"""
		def modelOutStds = """
<http://data.widget.com/resource#z00c1>
      a       cwr:Character ;
      skos:definition "Recurring thought (27 September 2014)"@en .

<http://data.widget.com/resource#z00s1>
      a       cwr:Rendition ;
      cwr:hasChar <http://data.widget.com/resource#z00c1> .
"""

		Model min = loadModel(mins)
		Model mm = loadModel(mms)
		Model mout = loadModel("")
		

		new MapRdf(prologSQ,prolog).run(min,mout,mm)
		def mouts = saveModel(mout)
		if (Verbose) println mouts
		
		assert true == ModelFileUtil.compareStrings(mouts,prologResult+modelOutStds), "Models are different"
	}

	@Test
	public void test2() {
		
		def mins = """
<http://example.com/term#00s1>
  rdf:type ec:Series ;
  ech:title "Series"@en ;
  er:hasChar30 <http://example.com/term#00c1> ;
  some-core:guid "00s1" ;
.
<http://example.com/term#00c1>
  rdf:type ec:Character ;
  en:desc30 "Recurring thought (27 September 2014)"@en ;
  some-core:guid "00c1" ;
  en:exId30 "123" ;
  en:inId30 "456" ;
.
"""
		def mms = """
ec:Series
	rl:map 
		[rl:type cwr:Rendition ; a rl:Create] ,
		[rl:src some-core:guid ; rl:format "z%s" ; rl:namespace <http://data.widget.com/resource#> ; a rl:MapUri ] ,
	    [rl:src er:hasChar30 ; rl:tgt cwr:hasChar ; a rl:Map ] ,
		[rl:path "er:hasChar30/en:exId30" ; rl:tgt var:c ; a rl:Map ] ,
		[rl:src var:c ; rl:tgt cwr:perCharSerExid ; a rl:Map ] 
		.
ec:Character
	rl:map 
		[rl:type cwr:Character ; a rl:Create ] ,
		[rl:src some-core:guid ; rl:format "z%s" ; rl:namespace <http://data.widget.com/resource#> ; a rl:MapUri ] ,
    	[rl:src en:desc30 ; rl:tgt  skos:definition ; a rl:Map  ] 
		.
"""
		def modelOutStds = """
<http://data.widget.com/resource#z00c1>
      a       cwr:Character ;
      skos:definition "Recurring thought (27 September 2014)"@en .

<http://data.widget.com/resource#z00s1>
      a       cwr:Rendition ;
      cwr:hasChar <http://data.widget.com/resource#z00c1> ;
      cwr:perCharSerExid "123" .
"""

		Model min = loadModel(mins)
		Model mm = loadModel(mms)
		Model mout = loadModel("")
		

		new MapRdf(prologSQ,prolog).run(min,mout,mm)
		def mouts = saveModel(mout)
		if (Verbose) println mouts
		
		assert true == ModelFileUtil.compareStrings(mouts,prologResult+modelOutStds), "Models are different"
	}
	@Test
	public void test3() {
		
		def mins = """
<http://example.com/term#00s1>
  rdf:type ec:Series ;
  ech:title "Series"@en ;
  er:hasChar30 <http://example.com/term#00c1> ;
  some-core:guid "00s1" ;
.
<http://example.com/term#00c1>
  rdf:type ec:Character ;
  en:desc30 "Recurring thought (27 September 2014)"@en ;
  some-core:guid "00c1" ;
  en:exId30 "123" ;
  en:inId30 "456" ;
.
"""
		def mms = """
ec:Series
	rl:map 
		[rl:type cwr:Rendition ; a rl:Create] ,
		[rl:src some-core:guid ; rl:format "z%s" ; rl:namespace <http://data.widget.com/resource#> ; a rl:MapUri ] ,
	    [rl:src er:hasChar30 ; rl:tgt cwr:hasChar ; a rl:Map ] ,
		[rl:path "er:hasChar30/en:exId30" ; rl:tgt var:c ; a rl:Map ] ,
		[rl:src var:c ; rl:tgt cwr:perCharSerExid ; a rl:Map ] ,
		[rl:expr "concat('help',?c)" ; rl:tgt cwr:perCharSerHelpExId ; a rl:Map ] 
		.
ec:Character
	rl:map 
		[rl:type cwr:Character ; a rl:Create ] ,
		[rl:src some-core:guid ; rl:format "z%s" ; rl:namespace <http://data.widget.com/resource#> ; a rl:MapUri ] ,
    	[rl:src en:desc30 ; rl:tgt  skos:definition ; a rl:Map  ] 
		.
"""
		def modelOutStds = """
<http://data.widget.com/resource#z00c1>
      a       cwr:Character ;
      skos:definition "Recurring thought (27 September 2014)"@en .

<http://data.widget.com/resource#z00s1>
      a       cwr:Rendition ;
      cwr:hasChar <http://data.widget.com/resource#z00c1> ;
      cwr:perCharSerExid "123" ;
      cwr:perCharSerHelpExId
              "help123" .
"""

		Model min = loadModel(mins)
		Model mm = loadModel(mms)
		Model mout = loadModel("")
		

		new MapRdf(prologSQ,prolog).run(min,mout,mm)
		def mouts = saveModel(mout)
		if (Verbose) println mouts
		
		assert true == ModelFileUtil.compareStrings(mouts,prologResult+modelOutStds), "Models are different"
	}


}
