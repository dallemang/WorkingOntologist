package xmlToRdf;

import static org.junit.Assert.*;

import groovy.json.JsonBuilder
import org.junit.Test;


// good generic XML to RDF processor
// for conceptual, hierarchical terms
// NOTE: this doesn't work as is
class TestXmlToRdf {

	def prolog = """#
@prefix geo-pos: <http://www.w3.org/2003/01/geo/wgs84_pos#> .
@prefix owl: <http://www.w3.org/2002/07/owl#> .
@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@prefix skos:     <http://www.w3.org/2004/02/skos/core#> .

"""
	
	def classTemplate = """testcase:%s
	rdfs:label	"%s" ;
	skos:definition	"A testcase -related class" ;
	rdf:type	owl:Class ;
    rdfs:subClassOf skos:Concept ;
.
"""

	def ttlTemplate = """testcase:z%s
	skos:prefLabel "%s" ;
	skos:annotation "%s" ;
	rdf:type testcase:%s ;
.
"""

	def ttlRelnTemplate = """testcase:z%s
	skos:prefLabel "%s" ;
	skos:annotation "%s" ;
	skos:exactMatch "z%s" ;
	rdf:type testcase:%s ;
.
"""

	def broaderTemplate = """testcase:z%s
	skos:broader testcase:z%s ;
.
"""

	def narrowerTemplate = """testcase:z%s
	skos:narrower testcase:z%s ;
.
"""

	def relatedTemplate = """testcase:z%s
	skos:related testcase:z%s ;
.
"""

	def apostrophe = 0x92

	def Concepts = [:]
	
	@Test
	public void test() {
		run("C:/git/WO/data.example.com/Testcase/source/TestcaseTaxonomy.xml")

		//printMap("Concepts", Concepts)
		
		processTtl("C:/git/WO/data.example.com/Testcase/Taxonomy/testcase.ttl","testcase")
	}
	def RELNKEY = "-rln"
	def processTtl(def targetFile,baseFile){
		File file = new File(targetFile)
		file.write(String.format(prolog, baseFile, baseFile))
		
		def types = []
		
		Concepts.each{k,v->
			if (v.type == "") return // issue with equivalence classes not having a "class" element
			if (!types.contains(v.type)){
				types += v.type
				file.append(
				String.format(classTemplate, 
				v.type,v.type)
				)
			}
			def name = v.name.replaceAll("[^\\x20-\\x7e]", "'")	// non-ASCII apostrophe, blows up in TBC
			name = name.replaceAll("OMalley","O'Malley")
			file.append(
			String.format(ttlTemplate, 
			k,name,k,v.type)
			)
			if (v.type == "People"){
				file.append(
				String.format(ttlTemplate, 
				k+"issu",name,k+"issu","Issues")
				)
			}
			v.related.each{
				file.append(
					String.format(relatedTemplate,
					k,it)
					)
			}
			v.broader.each{
				file.append(
					String.format(broaderTemplate,
					k,it)
					)
			}
			v.narrower.each{
				file.append(
					String.format(narrowerTemplate,
					k,it)
					)
			}
		}
	}

	public void run(def sourceFile) {
		
		XmlSlurper xs = new XmlSlurper()
		def path = xs.parse(new File(sourceFile))
		
		path.terms.term.each{
			def map = [:]
			Concepts[""+it.@zthesId] = map
			map.name = ""+it.@name
			map.type = ""+it.class.@name
			
			specialReprocessing(map)	// this is kludge stuff 
			
			def broader = it.relationships.relationship.findAll{it.@type == "hierarchical" && it.@name == "Broader Term"}
			def narrower = it.relationships.relationship.findAll{it.@type == "hierarchical" && it.@name == "Narrower Term"}
			def related = it.relationships.relationship.findAll{it.@type == "associative" && it.@name == "Related To"}
			def equivalent = it.relationships.relationship.findAll{it.@type == "equivalence" && it.@name == "Use For"}

			map.broader = broader.collect{""+it.@zthesId}
			map.narrower = narrower.collect{""+it.@zthesId}
			map.related = related.collect{""+it.@zthesId}
			map.equivalent = equivalent.collect{""+it.@zthesId}
		
		}
	}
	
	def specialReprocessing(map){
		if (map.name in ["Personal names","Countries","Groups","Campaign Events", "Contested convention"])
			map.type += "Category";
	}
	
	def printMap(title, map){
		
		println "\n${title}:"
		JsonBuilder jb = new JsonBuilder(map)
		println jb.toPrettyString()
	}
	
}
