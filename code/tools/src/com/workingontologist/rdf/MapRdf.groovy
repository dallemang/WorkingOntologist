package com.workingontologist.rdf
import static org.junit.Assert.*;

import com.workingontologist.util.*;

import groovy.json.JsonBuilder

import org.apache.jena.util.FileUtils ;
import org.apache.jena.n3.SelectedNodeModelWriter
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
import org.apache.jena.rdf.model.impl.LiteralImpl
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


public class MapRdf {

	def prologSQ = ""
	def prolog = ""

	def uriMap = [:]
	def inModel
	def outModel
	def mapModel
	public static boolean Verbose = true
	RdfUtil tu = new RdfUtil()
	
	public MapRdf(){}
	public MapRdf(String prologSQ, String prolog){
		this.prologSQ = prologSQ
		this.prolog = prolog
	}
	

	def run(inFile, outFile, mapFile){

		def Report = true;

		def im = ModelFactory.createDefaultModel();
		im.read(new FileInputStream(inFile),null,"TTL")
		def mm = ModelFactory.createDefaultModel();
		mm.read(new FileInputStream(mapFile),null,"TTL")

		prolog = ExtractProlog.makeTtlProlog([im, mm])
		prologSQ = ExtractProlog.makeSqProlog([im, mm])

		def om = ModelFactory.createDefaultModel();
		
		run(im, om, mm)
		ModelFileUtil.writeModelOrdered(outModel,new FileOutputStream(outFile),null)
	}
	
	def run(Model im, Model om, Model mm){
		inModel = im
		outModel = om
		mapModel = mm

		def tmap = [:]
		processMap(tmap)
		processMapExpr(tmap)
		processMapPath(tmap)
		processCreate(tmap)
		processMapInstance(tmap)
		processMapRelation(tmap)
		processMapAssociation(tmap)
		processMapUri(tmap)

		if (Verbose){
			JsonBuilder jb = new JsonBuilder(tmap)
			println "\n\tMappings"
			println jb.toPrettyString()
		}

		processDomain(inModel)
		inModel = processOut(inModel, outModel, tmap)
		processOut2(inModel, outModel, tmap)


		if (Verbose){
			JsonBuilder jb = new JsonBuilder(inPropMap)
			println "\n\tInbound Properties Mapped by Class"
			println jb.toPrettyString()
		}
	}
	
		

	def inPropMap = [:]
	// get a baseline for total properties
	def processDomain(inModel){

		def sq = prologSQ + """
	select  distinct ?t ?p {
		?s a ?t .
		?s ?p ?x .
	}	
	"""
		def rmap = tu.queryJsonMap(inModel,sq)
		for (int i=0;i< rmap["t"].size();i++){
			if (!inPropMap.containsKey(rmap["t"][i])) inPropMap[rmap["t"][i]]=[:]
			inPropMap[rmap["t"][i]][rmap["p"][i]]=""	// create a set of props
		}
	}

	def instanceVariableMap = [:]

	def processOut(inModel, outModel, tmap){

		tmap.each{k,v->


			def sq = prologSQ + """
	select  ?t {
		?t a <%s> .			
	}	
	"""
			def ttl = ""
			def sq2 = String.format(sq, k)
			def rmap = tu.queryJsonMap(inModel,sq2)
			rmap.t.each{
				def uuid = getOneResult(inModel,"select ?one { <${it}> <${tmap[k]["MapUri"]["src"]}> ?one }")
				def uid = String.format(tmap[k]["MapUri"]["fmt"],uuid)
				def uri = "<${tmap[k]["MapUri"]["ns"]}${uid}>"
				def s = "${uri} a <${tmap[k]["Create"][0]}> ;"
				ttl += "${s}\n"
				//println "create: ${tmap[k]["Create"][0]}"
				uriMap[it]=uri

				if (tmap[k].containsKey("MapInstance")){
					def s4 = tmap[k]["MapInstance"]["reln"]
					def s3 = "<${s4}> <${tmap[k]["MapInstance"]["createReln"]}> ; "
					ttl += "\t${s3}\n"
				}
				tmap[k]["Maps"].each{k2,v2->
					if (k2.startsWith("http://workingontologist.com/rule/variable#")) return
						//if (k2.startsWith("http://workingontologist.com/rule/expression#")) return
						if (v2.startsWith("http://workingontologist.com/rule/variable#")) {
							if (!instanceVariableMap.containsKey(it))
								instanceVariableMap[it] = [:]
							if (!instanceVariableMap[it].containsKey(v2))
								instanceVariableMap[it][v2] = []
							def list = getRDFNode(inModel,"select ?one { <${it}> <${k2}> ?one }")
							if (list.size()==0) return
								list.each{res->
									instanceVariableMap[it][v2]+= res
								}
						}
						else {
							def list = getOneResultDatatype(inModel,"select ?one { <${it}> <${k2}> ?one }")
							if (list.size()==0) return
								list.each{res->
									res = res.replaceAll(/[^\x00-\x7F]+/,"")

									def s2 = "<${v2}> ${res} ;"
									ttl += "\t${s2}\n"
									inPropMap[k][k2]=v2
								}
						}
				}

				tmap[k]["MapPath"].each{k2,v2->
					if (k2.startsWith("http://workingontologist.com/rule/variable#")) return
						//if (k2.startsWith("http://workingontologist.com/rule/expression#")) return
						if (v2.startsWith("http://workingontologist.com/rule/variable#")) {
							if (!instanceVariableMap.containsKey(it))
								instanceVariableMap[it] = [:]
							if (!instanceVariableMap[it].containsKey(v2))
								instanceVariableMap[it][v2] = []
							def list = getRDFNode(inModel,"select ?one { <${it}> ${k2} ?one }")
							if (list.size()==0) return
								list.each{res->
									instanceVariableMap[it][v2]+= res
								}
						}
						else {
							def list = getOneResultDatatype(inModel,"select ?one { <${it}> ${k2} ?one }")
							if (list.size()==0) return
								list.each{res->
									res = res.replaceAll(/[^\x00-\x7F]+/,"")

									def s2 = "<${v2}> ${res} ;"
									ttl += "\t${s2}\n"
									inPropMap[k][k2]=v2
								}
						}
				}

				ttl += ".\n"
				//println ttl
			}
			outModel.read(new ByteArrayInputStream((prolog + ttl).getBytes()),null,"TTL")
		}
		return inModel
	}

	def processOut2(inModel, outModel, tmap){

		tmap.each{k,v->


			def sq = prologSQ + """
	select  ?t {
		?t a <%s> .			
	}	
	"""
			def ttl = ""
			def sq2 = String.format(sq, k)
			def rmap = tu.queryJsonMap(inModel,sq2)
			rmap.t.each{
				tmap[k]["MapExpr"].each{k2,v2->

					def map = instanceVariableMap[it]
					def map2 = [:]
					map.each{k3,v3->
						def k3a = k3.split("#")
						map2[k3a[1]] = v3[0] // TODO-problem if more than one value in var!!!
					}
					def res = getOneResultDatatype(inModel,
							"select ?_x { bind (${k2} as ?_x) }",
							"_x",
							map2)
					if (res.size()==0) return
						res.each{uri->

							def uriHome =  uriMap[it]
							def s2 = "${uriHome} <${v2}> ${uri} ;"
							ttl += "\t${s2} .\n"
						}
				}

				tmap[k]["Maps"].each{k2,v2->
					def res = []
					if (k2.startsWith("http://workingontologist.com/rule/expression#")){
						res = getOneResultDatatype(inModel, "select ?one { <${it}> <${k2}> ?one }")
					}
					else if (k2.startsWith("http://workingontologist.com/rule/variable#")){
						res = instanceVariableMap[it][k2]
					}
					else {
						res = getOneResultResource(inModel, "select ?one { <${it}> <${k2}> ?one }")
						inPropMap[k][k2]=v2
					}
					if (res.size()==0) return
						res.each{uri->
							def uriHome =  uriMap[it]
							if (uri instanceof LiteralImpl) {
							def s2 = "${uriHome} <${v2}> \"${uri}\" ;"
							ttl += "\t${s2} .\n"
							} else {
							def s2 = "${uriHome} <${v2}> ${uri} ;"
							ttl += "\t${s2} .\n"
							}
//println "${it}, ${uri}, ${s2}"
						}
				}


				if (tmap[k].containsKey('MapAssociation')) {
					def type = tmap[k]["MapAssociation"]["type"]

					def flda = tmap[k]["MapAssociation"].fld
					def obja = tmap[k]["MapAssociation"].obj
					def s = String.format("""
						[
							a <%s> ;
							""", type)

					for (int i=0;i<flda.size();i++){
						def fld = flda[i]
						def obj = obja[i]

						if (obj == "http://workingontologist.com/rule/map#this") obj = it
						else {
							def fobj = obj.startsWith("http:")? "<${obj}>" : "${obj}"
							def res = getResults(inModel,"select ?one { <${it}> ${fobj} ?one }")
							if (res == null){
								println "Warning: lookup not found: ${it}.${obj}"
								return
							}
							obj = res[0]
						}
						s += String.format("""
						<%s> <%s> ;\n
						""", fld, obj)


					}
					s += String.format("""
						]
						""","")
					ttl += "\t${s}\n"
					ttl += ".\n"

				}

				if (tmap[k].containsKey("MapRelation")) {
					def uri2 = getOneResult(inModel,"select ?one { <${it}> <${tmap[k]["MapRelation"]["src"]}> ?one }")
					if (uri2 == null) {
						println "Warning: result not found for ${tmap[k]["MapRelation"]["src"]}"
						return;
					}
					if (!uriMap.containsKey(uri2)) {
						println "Warning: lookup not found: ${uri2}"
						return
					}
					def uriTgt = uriMap[uri2]
					def s5 = tmap[k]["MapRelation"]["tgt"]
					def uriHome =  uriMap[it]
					def s6 = "${uriHome} <${s5}> ${uriTgt} ; "
					ttl += "\t${s6}\n"
					ttl += ".\n"
				}
			}
			//			println ttl
			try {
				outModel.read(new ByteArrayInputStream((prolog + ttl).getBytes()),null,"TTL")
			} catch (Exception e){
				println "Error ${e}, loading:\n${ttl}"
			}
		}
	}

	def getOneResult(model,sq){

		def rmap = tu.queryJsonMap(model,prologSQ + sq)
		return rmap["one"][0]
	}

	def getResults(model,sq){

		def rmap = tu.queryJsonMap(model,prologSQ + sq)
		return rmap["one"]
	}

	def nodeToLiteral(RDFNode rn){
		if (rn.isLiteral()){
			Literal lit = rn.asLiteral()
			def fqn = ""+lit.asNode()
			def fqn2 = fqn.replaceAll('http://www.w3.org/2001/XMLSchema#','xsd:')
			return fqn2.replaceAll("\n", "")
		}
		return rn.asResource()
	}
	def getOneResultDatatype(model,sq){
		return getOneResultDatatype(model,sq,"one")
	}
	def getOneResultDatatype(model,sq,var){

		def rs = tu.queryRS(model,prologSQ + sq)
		def list = []
		while (rs.hasNext()){
			QuerySolution qs = rs.next()
			RDFNode rn = qs.get(var)
			if (rn.isLiteral()){
				Literal lit = rn.asLiteral()
				def fqn = ""+lit.asNode()
				def fqn2 = fqn.replaceAll('http://www.w3.org/2001/XMLSchema#','xsd:')
				list += fqn2.replaceAll("\n", "")
			}
			//return "" + rn.asResource() ?: ""
		}
		return list
	}

	def getOneResultDatatype(model,sq,var,map){

		def rs = tu.queryRS(model,prologSQ + sq,map)
		def list = []
		while (rs.hasNext()){
			QuerySolution qs = rs.next()
			RDFNode rn = qs.get(var)
			if (rn.isLiteral()){
				Literal lit = rn.asLiteral()
				def fqn = ""+lit.asNode()
				def fqn2 = fqn.replaceAll('http://www.w3.org/2001/XMLSchema#','xsd:')
				list += fqn2.replaceAll("\n", "")
			}
			//return "" + rn.asResource() ?: ""
		}
		return list
	}

	def getRDFNode(model,sq){
		return getRDFNode(model,sq,"one")
	}
	def getRDFNode(model,sq,var){

		def rs = tu.queryRS(model,prologSQ + sq)
		def list = []
		while (rs.hasNext()){
			QuerySolution qs = rs.next()
			RDFNode rn = qs.get(var)
			list += rn
		}
		return list
	}

	def getOneResultResource(model,sq){

		def rs = tu.queryRS(model,prologSQ + sq)
		def list = []
		while (rs.hasNext()){
			QuerySolution qs = rs.next()
			RDFNode rn = qs.get("one")
			if (!rn.isLiteral()){
				// attempt a lookup
				def uri = ""+rn.asResource()
				def res = uriMap[uri]
				if (res != null)
					list += res
			}
			//return "" + rn.asResource() ?: ""
		}
		return list
	}

	def processMap(tmap){

		def sq = prologSQ + """
select  ?t ?src ?tgt {
	?t rl:map ?m .
	?m rl:src ?src .
	?m rl:tgt ?tgt .
	?m a rl:Map .		
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.src.size();i++) {
			//println "from ${rmap.t[i]}, map ${rmap.src[i]} -> ${rmap.tgt[i]}"
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("Maps"))
				tmap[rmap.t[i]]["Maps"] = [:]
			tmap[rmap.t[i]]["Maps"][rmap.src[i]] = rmap.tgt[i]
		}

	}

	def processMapExpr(tmap){

		def sq = prologSQ + """
select  ?t ?src ?tgt {
	?t rl:map ?m .
	?m rl:expr ?src .
	?m rl:tgt ?tgt .
	?m a rl:Map .		
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.src.size();i++) {
			//println "from ${rmap.t[i]}, map ${rmap.src[i]} -> ${rmap.tgt[i]}"
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("MapExpr"))
				tmap[rmap.t[i]]["MapExpr"] = [:]
			tmap[rmap.t[i]]["MapExpr"][rmap.src[i]] = rmap.tgt[i]
		}

	}

	def processMapPath(tmap){

		def sq = prologSQ + """
select  ?t ?src ?tgt {
	?t rl:map ?m .
	?m rl:path ?src .
	?m rl:tgt ?tgt .
	?m a rl:Map .		
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.src.size();i++) {
			//println "from ${rmap.t[i]}, map ${rmap.src[i]} -> ${rmap.tgt[i]}"
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("MapPath"))
				tmap[rmap.t[i]]["MapPath"] = [:]
			tmap[rmap.t[i]]["MapPath"][rmap.src[i]] = rmap.tgt[i]
		}

	}

	def processCreate(tmap){

		def sq = prologSQ + """
select  ?t ?tgt {
	?t rl:map ?m .
	?m rl:type ?tgt .
	?m a rl:Create .		
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.t.size();i++) {
			//println "from ${rmap.t[i]}, create ${rmap.tgt[i]}"
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("Create"))
				tmap[rmap.t[i]]["Create"] = []
			tmap[rmap.t[i]]["Create"] += rmap.tgt[i]
		}

	}
	def processMapInstance(tmap){

		def sq = prologSQ + """
select  ?t ?tgt ?scope ?lu {
	?t rl:map ?m .
	?m rl:ref ?tgt .
	?m rl:reln ?scope .
	?m a rl:MapInstance .		
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.t.size();i++) {
			//println "from ${rmap.t[i]}, create reln ${rmap.tgt[i]} predicate ${rmap.scope[i]} w/lookup ${rmap.lu[i]}"
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("MapInstance"))
				tmap[rmap.t[i]]["MapInstance"] = [:]
			tmap[rmap.t[i]]["MapInstance"]["createReln"] = rmap.tgt[i]
			tmap[rmap.t[i]]["MapInstance"]["reln"] = rmap.scope[i]
		}
	}

	def processMapRelation(tmap){

		def sq = prologSQ + """
select  ?t ?src ?tgt {
	?t rl:map ?m .
	?m rl:src ?src .
	?m rl:reln ?tgt .
	?m a rl:MapRelation .
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.t.size();i++) {
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("MapRelation"))
				tmap[rmap.t[i]]["MapRelation"] = [:]
			tmap[rmap.t[i]]["MapRelation"]["src"] = rmap.src[i]
			tmap[rmap.t[i]]["MapRelation"]["tgt"] = rmap.tgt[i]
		}
	}

	def processMapAssociation(tmap){

		def sq = prologSQ + """
select  ?t ?type ?fld ?obj {
	?t rl:map ?m .
	?m a rl:MapAssociation .
	?m rl:type ?type .
	?m rl:parameters [rl:fld ?fld ; rl:reln ?obj] 
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.t.size();i++) {
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("MapAssociation"))
				tmap[rmap.t[i]]["MapAssociation"] = [:]
			if (!tmap[rmap.t[i]]["MapAssociation"].containsKey("type"))
				tmap[rmap.t[i]]["MapAssociation"]["type"] = rmap.type[i]
			if (!tmap[rmap.t[i]]["MapAssociation"].containsKey("fld")) {
				tmap[rmap.t[i]]["MapAssociation"]["fld"] = []
				tmap[rmap.t[i]]["MapAssociation"]["obj"] = []
			}
			tmap[rmap.t[i]]["MapAssociation"]["fld"] += rmap.fld[i]
			tmap[rmap.t[i]]["MapAssociation"]["obj"] += rmap.obj[i]
		}
		//println "here"
	}

	def processMapUri(tmap){

		def sq = prologSQ + """
select  ?t ?src ?fmt ?ns {
	?t rl:map ?m .
	?m rl:src ?src .
	?m rl:format ?fmt .
	?m rl:namespace ?ns .
	?m a rl:MapUri .
}	
"""
		def rmap = tu.queryJsonMap(mapModel,sq)

		for(int i=0;i<rmap.t.size();i++) {
			//println "from ${rmap.t[i]}, create reln ${rmap.src[i]} predicate ${rmap.fmt[i]} namespace ${rmap.ns[i]}"
			if (!tmap.containsKey(rmap.t[i]))
				tmap[rmap.t[i]] = [:]
			if (!tmap[rmap.t[i]].containsKey("MapUri"))
				tmap[rmap.t[i]]["MapUri"] = [:]
			tmap[rmap.t[i]]["MapUri"]["src"] = rmap.src[i]
			tmap[rmap.t[i]]["MapUri"]["fmt"] = rmap.fmt[i]
			tmap[rmap.t[i]]["MapUri"]["ns"] = rmap.ns[i]
		}
	}

}
