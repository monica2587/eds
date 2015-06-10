package com.search.eds.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientBuilder;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.eds.beans.Dataset;
import com.search.eds.beans.Variable;
import com.search.eds.utils.CsvVariableExtractor;
import com.search.eds.utils.Utils;

public class CKANIndexer extends Indexer {

	private static final Logger s_logger = Logger.getLogger(CKANIndexer.class.getName());
	
	public boolean index(Client client, List<String> repoList){
		boolean result = true;
		
		//create an instance of ObjectMapper, reuse
		ObjectMapper mapper = new ObjectMapper();
		
		//index each repo
		for (String repo : repoList) {	
			//invoke REST API to list all datasets in the repo
			String responseEntity = ClientBuilder.newClient().target(repo)
							.path("api/action/package_list").request().get(String.class);
			try {
				//keep track of how many datasets we indexed
				int count = 0;
				//parse response to get list of datasets
				JsonNode responseObj = mapper.readTree(responseEntity);
				JsonNode resultArrObj = responseObj.get("result");
				if(resultArrObj != null && !resultArrObj.isNull() && resultArrObj.isArray()) {
					//iterate over each dataset in the list
					for (JsonNode resultObj : resultArrObj) {
						//TODO: convert to fine when ready
						s_logger.info("Examining dataset " + resultObj.textValue());
						if(indexVariables(client, mapper, repo, resultObj.textValue())) count ++;
					}
				}
				s_logger.info("Successfully indexed " + count + " datasets in the repo " + repo);
			} catch (JsonProcessingException e) {
				s_logger.severe("Exception when indexing repo " + repo + " " + e.getMessage());
				result = false;
			} catch (IOException e) {
				s_logger.severe("Exception when indexing repo " + repo + " " + e.getMessage());
				result = false;
			}
		}
		return result;
	}
	
	private boolean indexVariables(Client client, ObjectMapper mapper, String repo, String dataset) {
		boolean result = true;
		// invoke REST API to get one dataset from the repo
		String responseEntity = "";
		
		try {
			responseEntity = ClientBuilder.newClient().target(repo)
					.path("api/action/package_show").queryParam("id", dataset)
					.request().get(String.class);
		} catch (Exception e){
			//don't let one dataset break the flow
			s_logger.severe("Exception when indexing dataset " + dataset + " in the repo " + repo + " " + e.getMessage());
			return false;
			
		}
		try {
			JsonNode responseObj = mapper.readTree(responseEntity);
			JsonNode resultObj = responseObj.get("result");
			if(resultObj != null){
				
				//check to see if csv file, indexing only variables from numerical datasets
				String csvFileUrl = null;
				JsonNode resourceArrObj = resultObj.get("resources");
				if(resourceArrObj != null && !resourceArrObj.isNull() && resourceArrObj.isArray()){
					for (JsonNode resourceObj : resourceArrObj) {
						if (resourceObj.get("format").textValue().equalsIgnoreCase("csv") && resourceObj.get("url").textValue().endsWith(".csv")) {
							csvFileUrl = resourceObj.get("url").textValue();
							break;
						}
					}
				}
				if(csvFileUrl != null){
					JsonNode obj = null;
					CsvVariableExtractor extractor = new CsvVariableExtractor();
					List<String> varNames = extractor.extractVariables(csvFileUrl);
					if(varNames != null) {
						//if csv file and if contains variables, parse dataset object once and reuse for all variables
						Dataset datasetObj = new Dataset();
						
						//TODO: set data type
						//set dataset id
						//generating our own unique uuid rather than using the one created by portal
						datasetObj.createDatasetId();
						//uncomment below to use uuid created by portal
						/*obj = resultObj.get("id");
						if(obj != null && !obj.isNull())
							data.setDatasetId(obj.textValue());*/
						
						//set dataset name
						obj = resultObj.get("title");
						if(obj != null && !obj.isNull())
							datasetObj.setDatasetName(obj.textValue());
						
						//set variable description
						obj = resultObj.get("notes");
						if(obj != null && !obj.isNull())
							datasetObj.setDescription(obj.textValue());
						
						//set dataset URL
						datasetObj.setDatasetUrl(repo + "api/action/package_show?id=" + dataset);
						
						//set tag List
						/*JsonNode tagArrObj = resultObj.get("tags");
						if(tagArrObj != null && !tagArrObj.isNull() && tagArrObj.isArray()){
							List<String> tagList = new ArrayList<String>();
							for (JsonNode tagObj : tagArrObj) {
								tagList.add(tagObj.get("name").textValue());
							}
							datasetObj.setTagList(tagList);
						}*/
						
						//For data.gc.ca only
						JsonNode keywordObj = resultObj.get("keywords");
						if(keywordObj != null){
							List<String> tagList = new ArrayList<String>();
							String[] keywordArr = keywordObj.textValue().split(",");
							for (String keyword : keywordArr) {
								tagList.add(keyword);
							}
							datasetObj.setTagList(tagList);
						}
						
						//set format and URL List
						Set<String> formatList = new HashSet<String>();
						List<String> urlList = new ArrayList<String>();
						for (JsonNode resourceObj : resourceArrObj) {
							formatList.add(resourceObj.get("format").textValue().toLowerCase());
							urlList.add(resourceObj.get("url").textValue());
						}
						datasetObj.setFormatList(formatList);
						datasetObj.setUrlList(urlList);
						
						//set org
						obj = resultObj.get("organization");
						if(obj != null && !obj.isNull()) {
							datasetObj.setOrg(Utils.canonicalizeOrg(obj.get("title").textValue()));
						}
						//now create variable objects with dataset object passed in
						for (String varName : varNames) {
							//do not index empty string variables
							if(varName.trim().length() < 1) 
								continue;
							
							//create a variable object
							Variable var = new Variable(varName, datasetObj);
							
							//invoke elasticsearch index API
							IndexResponse esResponse = client.prepareIndex("eds", "variables")
							        .setSource(mapper.writeValueAsString(var))
							        .execute()
							        .actionGet();
							//TODO: change log level to fine when ready
							s_logger.info("done indexing " + var.getName() + " with response index: " + esResponse.getIndex() + " type: " + esResponse.getType() + " and id: " + esResponse.getId());
						}
					}
				}
				
				/*try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					//do nothing
				}*/
			}
		} catch (JsonProcessingException e) {
			s_logger.severe("Exception when indexing dataset " + dataset + " in the repo " + repo + " " + e.getMessage());
			result = false;
		} catch (IOException e) {
			s_logger.severe("Exception when indexing dataset " + dataset + " in the repo " + repo + " " + e.getMessage());
			result = false;
		}
		return result;
	}
}
