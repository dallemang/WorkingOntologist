
package com.workingontologist.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import groovy.json.JsonSlurper
import groovy.json.JsonBuilder

import org.apache.jena.graph.Triple;
import org.apache.jena.n3.turtle.TurtleEventHandler;
import org.apache.jena.n3.turtle.parser.TurtleParser
import org.apache.jena.query.*
import org.apache.jena.rdf.model.*
import org.apache.jena.util.FileUtils ;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * https://issues.apache.org/jira/browse/JENA-641
 * 
 * @author rspates
 *
 */
public class RdfUtil {

	private static final Logger log = LoggerFactory.getLogger(RdfUtil.class);
	def prolog = ""
	
	public RdfUtil(){
		
	}
	
	public RdfUtil (models){
		prolog = ExtractProlog.makeSqProlog(models)
		
	}
	public void ttlStatsJson(root, file){
		
		new File(file).write ttlStatsJson(root)
	}
	
	public String ttlStatsJson(root){
		def builder = new JsonBuilder()
		
		def map = ttlStatsMap(root);
		
		builder(map)
		return builder.toString()
		
	}
	
	public Map ttlStatsMap(root) {
		
		def rmap = [:]
		// attempt to load and parse all ttl nested under folder
		new File(root).eachFileRecurse(groovy.io.FileType.FILES) { file ->
			if (file =~ /.*.ttl/){
				rmap[file.name] = [:]
				rmap[file.name]['file'] 
				def map = read(file)
				rmap[file.name]['triples'] = map
				try {
					load(file)
					rmap[file.name]['instances'] = query(file)
				} catch (org.apache.jena.atlas.RuntimeIOException e) {
					def text = file.text
					def finder = (text =~ /[^\x00-\x7F]+/)
					log.error("TTL parse error, range: ${finder.first} - ${finder.last}, text= ${finder[0]}")
					log.error("${file}, ${e}")
				} catch (Exception e) {
					log.error("Error: ${file}, ${e}",e)
				}
			}
		}
		return rmap
	}
	// can this be refactored to run off ttlStatsMap?
	public void ttlStatsLog(root) {
		
		// attempt to load and parse all ttl nested under folder
		new File(root).eachFileRecurse(groovy.io.FileType.FILES) { file ->
			if (file =~ /.*.ttl/)
				log.debug(""+file)

			def map = read(file)
			log.debug(""+map)
			try {
				load(file)
				log.debug(""+query(file))
			} catch (org.apache.jena.atlas.RuntimeIOException e) {
				def text = file.text
				def finder = (text =~ /[^\x00-\x7F]+/)
				log.error("TTL parse error, range: ${finder.first} - ${finder.last}, text= ${finder[0]}")
				log.error("Error: ${e}",e)
			} catch (Exception e) {
				log.error("Error: ${e}",e)
			}
		}

	}
		
	def query2(file){
		def s = queryJson(file, """

select ?c
	{
			?c skos:prefLabel ?pl .
			filter(bound(?c) && ""=?pl) .
	}
""")

		def retMap = [:]
		def slurper = new JsonSlurper()
		def map = slurper.parseText(s)
		map.head.vars.each{
			if (map.results.bindings.size()>0) {
			def value = map.results.bindings[0][it].value
			if (value != "0")
				retMap[it] = value
			//def m = map.results.bindings
			}
		}
		return retMap
	}


	def query(file){
		def s = queryJson(file, """

select (count(?r) as ?concept) 
		(count(?gl) as ?genericLocation) 
		(count(?ch) as ?character) 
		(count(?sl) as ?storyLine) 
		(count(?rd) as ?relationshipDevelopment) 
		(count(?th) as ?thread) 
		(count(?pl) as ?place) 
		(count(?tv) as ?series) 
		{
	{
			?c rdfs:subClassOf skos:Concept .
			?r a ?c .
	} union {
			?gl a  creativework:GenericLocation .
	} union {
			?ch a  creativework:Character .
	} union {
			?sl a  epistoryline:EpisodicStoryline .
	} union {
			?rd a  epistoryline:RelationshipDevelopment .
	} union {
			?th a  epistoryline:Thread .
	} union {
			?pl a  creativework:Place .
	} union {
			?tv a  tv:Series .
	}

}
""")

		def retMap = [:]
		def slurper = new JsonSlurper()
		def map = slurper.parseText(s)
		map.head.vars.each{
			def value = map.results.bindings[0][it].value
			if (value != "0")
				retMap[it] = value
			//def m = map.results.bindings
		}
		return retMap
	}



	public String queryJson(file,query) {

		ResultSet results =  queryRS(file,query);

		ByteArrayOutputStream baos = new ByteArrayOutputStream()
		// Output query results
		ResultSetFormatter.outputAsJSON(baos, results)

		return baos.toString();
	}

    public Map queryJsonMap(Model model,query) {

            ResultSet results =  queryRS(model,query);

            ByteArrayOutputStream baos = new ByteArrayOutputStream()
            // Output query results
            ResultSetFormatter.outputAsJSON(baos, results)

            def slurper = new JsonSlurper()
            def map = slurper.parseText(baos.toString())

            def numap = [:]

            map.head.vars.each {
                    if (!numap.containsKey(it))
                            numap[it] = []
            }
            map.results.bindings.each{
                    it.each{k,v->
                            numap[k] += v.value
                    }
            }
            return numap
    }

	public String queryFormat(file,query) {
		
		ResultSet results =  queryRS(file,query);
		// Output query results
		def rs = ResultSetFormatter.asText(results);

		return rs
	}

	public ResultSet queryRS(String file,query) {

		def model = load(file);

		return queryRS(model,query)
	}

	public ResultSet queryRS(Model model,query, map) {

		QuerySolutionMap qsm = new QuerySolutionMap();
		map.each{k,v->
			qsm.add(k, v)
		}
		QueryExecution qe = QueryExecutionFactory.create(prolog + query, model, qsm);
		ResultSet results =  qe.execSelect();

		//qe.close();
		return results
	}

	public ResultSet queryRS(Model model,query) {

		QueryExecution qe = QueryExecutionFactory.create(prolog + query, model);
		ResultSet results =  qe.execSelect();

		//qe.close();
		return results
	}

	public Model queryModel(Model model,query) {

		QueryExecution qe = QueryExecutionFactory.create(prolog + query, model);
		Model results =  qe.execDescribe();

		//qe.close();
		return results
	}

	public Map read(tgtDir,tgtFile){
		return read(tgtDir + tgtFile)
	}
	public Map read(tgtFile){
		FileInputStream fis = new FileInputStream(tgtFile)
		MyEventHandler teh = new MyEventHandler();
		try {
			TurtleParser parser = new TurtleParser(fis);
			parser.setEventHandler(teh);
			parser.parse();
		}
		catch(Exception e) {
					log.error("${tgtFile}, ${e}")
		} catch(Error e) {
					log.error("${tgtFile}, ${e}")
		}
		def map = [:]
		map['prefixes'] = teh.prefixCnt
		map['triples'] = teh.tripleCnt
		return map
	}
	public Model load(tgtDir,tgtFile){
		return load(tgtDir + tgtFile)
	}
	public Model load(tgtFile){
		FileInputStream fis = new FileInputStream(tgtFile)
		Model m = ModelFactory.createDefaultModel();
		try {
					m.read(fis,null,"TTL");
		} catch (Exception e) {
					log.error("${tgtFile}, ${e}")
		}
		return m
	}
}
class MyEventHandler implements TurtleEventHandler{

	public int tripleCnt = 0;
	public int prefixCnt = 0;
	@Override
	public void triple(int line, int col, Triple triple) {
		// TODO Auto-generated method stub
		tripleCnt++;
	}

	@Override
	public void prefix(int line, int col, String prefix, String iri) {
		// TODO Auto-generated method stub
		prefixCnt++;
	}

	@Override
	public void startFormula(int line, int col) {
		// TODO Auto-generated method stub
		println "start formula"
	}

	@Override
	public void endFormula(int line, int col) {
		// TODO Auto-generated method stub
		println "end formula"
	}
}