package com.workingontologist.rdf;

import groovy.lang.GroovyObject;
import org.apache.jena.rdf.model.*
import com.workingontologist.util.*;

public abstract class MapRdfUnitBase {

	protected def prolog = """
@prefix res:      <http://widget.data.com/resource#> .
@prefix rl:      <http://workingontologist.com/rule/map#> .
@prefix var:      <http://workingontologist.com/rule/variable#> .
@prefix fn:      <http://www.w3.org/2005/xpath-functions#> .
@prefix afn:	 <http://jena.hpl.hp.com/ARQ/function#> .
@prefix ea: <http://example.com/attributeType#> .
@prefix ech: <http://example.com/choiceType#> .
@prefix ec: <http://example.com/class#> .
@prefix en: <http://example.com/noteType#> .
@prefix er: <http://example.com/relationship#> .
@prefix ns1: <http://example.com/attributeType#20-20-> .
@prefix os: <http://example.com/OntologyServer#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix some-core: <http://www.example.com/2014/08/some-core#> .
@prefix skos: <http://www.w3.org/2004/02/skos/core#> .
@prefix skos-xl: <http://www.w3.org/2008/05/skos-xl#> .
@prefix teamwork: <http://topbraid.org/teamwork#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix cwr: <http://data.widget.com/c-w-r#> .

"""
	protected def prologSQ = """
prefix res:      <http://widget.data.com/resource#> 
prefix rl:      <http://workingontologist.com/rule/map#> 
prefix var:      <http://workingontologist.com/rule/variable#> 
prefix fn:      <http://www.w3.org/2005/xpath-functions#> 
prefix afn:	 <http://jena.hpl.hp.com/ARQ/function#> 
prefix ea: <http://example.com/attributeType#> 
prefix ech: <http://example.com/choiceType#> 
prefix ec: <http://example.com/class#> 
prefix en: <http://example.com/noteType#> 
prefix er: <http://example.com/relationship#> 
prefix ns1: <http://example.com/attributeType#20-20-> 
prefix os: <http://example.com/OntologyServer#> 
prefix owl: <http://www.w3.org/2002/07/owl#> 
prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> 
prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> 
prefix some-core: <http://www.example.com/2014/08/some-core#> 
prefix skos: <http://www.w3.org/2004/02/skos/core#> 
prefix skos-xl: <http://www.w3.org/2008/05/skos-xl#> 
prefix teamwork: <http://topbraid.org/teamwork#> 
prefix xsd: <http://www.w3.org/2001/XMLSchema#> 
prefix cwr: <http://data.widget.com/c-w-r#> 
"""
	
protected def prologResult = """
@prefix afn:     <http://jena.hpl.hp.com/ARQ/function#> .
@prefix cwr:     <http://data.widget.com/c-w-r#> .
@prefix ea:      <http://example.com/attributeType#> .
@prefix ec:      <http://example.com/class#> .
@prefix ech:     <http://example.com/choiceType#> .
@prefix en:      <http://example.com/noteType#> .
@prefix er:      <http://example.com/relationship#> .
@prefix fn:      <http://www.w3.org/2005/xpath-functions#> .
@prefix ns1:     <http://example.com/attributeType#20-20-> .
@prefix os:      <http://example.com/OntologyServer#> .
@prefix owl:     <http://www.w3.org/2002/07/owl#> .
@prefix rdf:     <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs:    <http://www.w3.org/2000/01/rdf-schema#> .
@prefix res:     <http://widget.data.com/resource#> .
@prefix rl:      <http://workingontologist.com/rule/map#> .
@prefix skos:    <http://www.w3.org/2004/02/skos/core#> .
@prefix skos-xl:  <http://www.w3.org/2008/05/skos-xl#> .
@prefix some-core:  <http://www.example.com/2014/08/some-core#> .
@prefix teamwork:  <http://topbraid.org/teamwork#> .
@prefix var:     <http://workingontologist.com/rule/variable#> .
@prefix xsd:     <http://www.w3.org/2001/XMLSchema#> .
"""
	public MapRdfUnitBase() {
		super();
	}
	
	protected def loadModel(ms){
		Model model = ModelFactory.createDefaultModel()
		model.read(new ByteArrayInputStream((prolog+ms).getBytes()),null, "TTL")
		return model
	}
	protected def saveModel(mout){
		def ba = new ByteArrayOutputStream()
		ModelFileUtil.writeModelOrdered(mout,ba,null)
		return ba.toString()
	}

}