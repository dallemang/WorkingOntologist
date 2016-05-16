package com.workingontologist.util;

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

class ExtractProlog {

	Model inModel
	Model mapModel
	public void setup() {
		inModel = ModelFactory.createDefaultModel();
		inModel.read(new FileInputStream("./testin.ttl"),null,"TTL")
		mapModel = ModelFactory.createDefaultModel();
		mapModel.read(new FileInputStream("./testmap2.ttl"),null,"TTL")
	}

	@Test
	public void test() {
		setup()
		def map = extract ([inModel,mapModel])
		map.sort().each{k,v->
			println("${k}:${v}")
		}
		println ""
	}

	@Test
	public void test2() {
		setup()
		def map = extract ([inModel,mapModel])
		println makeSqProlog(map)
	}

	@Test
	public void test3() {
		setup()
		def map = extract ([inModel,mapModel])
		println makeTtlProlog(map)
	}

	public static Map extract(Model model){
		return extract([model])
	}
	
	public static Map extract(List<Model> models){
		def nsMap = [:]
		models.each{
			it.getNsPrefixMap().each{k,v->
				nsMap[k]=v
			}
		}
		return nsMap
	}
	
	public static makeSqProlog(map){
		def s = "# generated prolog\n"
		map.sort().each{k,v->
			s += "prefix ${k}:\t<${v}>\n"
		}
		return s
	}
	public static  makeTtlProlog(map){
		def s = "# generated prolog\n"
		map.sort().each{k,v->
			s += "@prefix ${k}:\t<${v}> .\n"
		}
		return s
	}
	public static  makeSqProlog(Model model){
		def map = extract(model)
		return makeSqProlog(map)
	}
	public static  makeTtlProlog(Model model){
		def map = extract(model)
		return makeTtlProlog(map)
	}
	public static  makeSqProlog(List<Model> models){
		def map = extract(models)
		return makeSqProlog(map)
	}
	public static  makeTtlProlog(List<Model> models){
		def map = extract(models)
		return makeTtlProlog(map)
	}
}
