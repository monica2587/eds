package com.search.eds.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.eds.beans.Dataset;
import com.search.eds.beans.Variable;
import com.search.eds.utils.CsvVariableExtractor;
import com.search.eds.utils.Utils;

public class CustomIndexer extends Indexer {

	private static final Logger s_logger = Logger.getLogger(CustomIndexer.class.getName());

	public boolean index(Client client, List<String> repoList) {
		boolean result = true;
		
		//create an instance of ObjectMapper, reuse
		ObjectMapper mapper = new ObjectMapper();
		
		//index each repo
		for(String repo : repoList) {
			//invoke REST API to list all datasets in the repo
			String responseEntity = "";
			try {
				responseEntity = ClientBuilder.newClient().target(repo)
								.request(MediaType.APPLICATION_JSON_TYPE).get(String.class);
			} catch (Exception e1) {
				//don't let one dataset break the flow
				s_logger.severe("Exception when indexing datasets in the repo " + repo + " " + e1.getMessage());
				continue;
			}
			try {
				//keep track of how many datasets we indexed
				int count = 0;
				//parse response to get list of datasets
				JsonNode resultArrObj = mapper.readTree(responseEntity);
				if(resultArrObj != null && resultArrObj.isArray()) {
					//iterate over each dataset in the list
					for (JsonNode resultObj : resultArrObj) {
						String dataset = "";
						if(resultObj.get("title") != null)
							dataset = resultObj.get("title").textValue();
						
						//TODO: convert to fine when ready
						s_logger.info("Examining dataset " + dataset);
						
						//construct the JSON object that we will index
						JsonNode obj = null;
						
						//check to see if csv file, indexing only variables from numerical datasets
						JsonNode csvResourceObj = resultObj.get("csv");
						if (csvResourceObj != null && !csvResourceObj.isNull() && csvResourceObj.textValue().endsWith(".csv")) {
							CsvVariableExtractor extractor = new CsvVariableExtractor();
							List<String> varNames = extractor.extractVariables(csvResourceObj.textValue());
							if(varNames != null) {
								//if csv file and if contains variables, parse dataset object once and reuse for all variables
								Dataset datasetObj = new Dataset();
								
								//TODO: set data type
								//set dataset id
								//generating our own unique uuid rather than using the one created by portal
								datasetObj.createDatasetId();
								//uncomment below to use uuid created by portal
								/*obj = resultObj.get("uuid");
								if(obj != null && !obj.isNull())
									var.setDatasetId(obj.textValue());*/
								
								//set dataset name
								datasetObj.setDatasetName(dataset);
								
								//set variable description
								obj = resultObj.get("description");
								if(obj != null && !obj.isNull())
									datasetObj.setDescription(obj.textValue());
								
								//TODO: set dataType
								
								//set dataset URL
								obj = resultObj.get("uuid");
								if(obj != null && !obj.isNull())
									datasetObj.setDatasetUrl(repo + "/" + obj.textValue());
								
								//set tag list
								JsonNode tagArrObj = resultObj.get("tags");
								if(tagArrObj != null &&!tagArrObj.isNull() && tagArrObj.textValue().trim().length() > 0){
									List<String> tagList = new ArrayList<String>();
									for (String tagObj : tagArrObj.textValue().split(",")) {
										tagList.add(tagObj);
									}
									datasetObj.setTagList(tagList);
								}
								
								//set format list and URL list
								Set<String> formatList = new HashSet<String>();
								List<String> urlList = new ArrayList<String>();
								
								JsonNode resourceObj = null;
								resourceObj = resultObj.get("kml");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("kml");
									urlList.add(resourceObj.textValue());

								}
								resourceObj = resultObj.get("txt");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("txt");
									urlList.add(resourceObj.textValue());

								}
								resourceObj = resultObj.get("json");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("json");
									urlList.add(resourceObj.textValue());

								}
								resourceObj = resultObj.get("csv");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("csv");
									urlList.add(resourceObj.textValue());

								}
								resourceObj = resultObj.get("xml");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("xml");
									urlList.add(resourceObj.textValue());

								}
								resourceObj = resultObj.get("shp");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("shp");
									urlList.add(resourceObj.textValue());

								}
								resourceObj = resultObj.get("other");
								if (resourceObj != null && !resourceObj.isNull()) {
									formatList.add("other");
									urlList.add(resourceObj.textValue());

								}
								datasetObj.setFormatList(formatList);
								datasetObj.setUrlList(urlList);
								
								//set org
								obj = resultObj.get("publisher");
								if(obj != null && !obj.isNull()) {
									datasetObj.setOrg(Utils.canonicalizeOrg(obj.textValue()));
								}
								for (String varName : varNames) {
									//do not index empty string variables
									if(varName.trim().length() < 1) 
										continue;
									
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
						count++;
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
}
