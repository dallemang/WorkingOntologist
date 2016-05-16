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
 */
class MapRdfTest {

	static {
		MapRdf.Verbose = false
	}
	@Test
	public void test() {
		def fin = "./resources/testin3.ttl"
		def fout = "./resources/testout.ttl"
		def fmap = "./resources/testmap3.ttl"
		def fstd = "./resources/testoutRef3.ttl"
		new MapRdf().run(fin,fout,fmap)
		ModelFileUtil.compare(fout,fstd)
		assert true == ModelFileUtil.compare(fout,fstd), "Models are different"
	}

	@Test
	public void test2() {
		def fin = "./resources/testin2.ttl"
		def fout = "./resources/testout.ttl"
		def fmap = "./resources/testmap2.ttl"
		def fstd = "./resources/testoutRef2.ttl"
		new MapRdf().run(fin,fout,fmap)
		ModelFileUtil.compare(fout,fstd)
		assert true == ModelFileUtil.compare(fout,fstd), "Models are different"
	}

}
