package com.search.eds.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientBuilder;

import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.search.eds.beans.Dataset;
import com.search.eds.beans.Variable;
import com.search.eds.utils.Utils;

public class SocrataIndexer extends Indexer {

	private static final Logger s_logger = Logger.getLogger(SocrataIndexer.class.getName());

	public boolean index(Client client, List<String> repoList) {
		boolean result = true;
		
		//create an instance of ObjectMapper, reuse
		ObjectMapper mapper = new ObjectMapper();
		
		//index each repo
		for(String repo : repoList) {
			
			try{
				//keep track of how many datasets we indexed
				int count = 0;
				//invoke HTML page listing all datasets
				List<String> datasetList = getDatasetListFromHTML(repo + "browse");
				for (String dataset : datasetList) {
					//TODO: convert to fine when ready
					s_logger.info("Examining dataset " + dataset);
					if(indexVariables(client, mapper, repo, dataset)) count++;
				}
				s_logger.info("Successfully indexed " + count + " datasets in the repo " + repo);
			} catch (IOException e){
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
					.path("api/views/" + dataset)
					.request().get(String.class);
		} catch (Exception e1) {
			//don't let one dataset break the flow
			s_logger.severe("Exception when indexing dataset " + dataset + " in the repo " + repo + " " + e1.getMessage());
			return false;
		}
		try {
			JsonNode responseObj = mapper.readTree(responseEntity);
			JsonNode obj = null;
			
			//check to see if tabular file, indexing only variables from numerical datasets
			obj = responseObj.get("viewType");
			if(obj != null && !obj.isNull()) {
				String format = obj.textValue().trim();
				
				//extract variable info - for tabular files only
				if(format.equalsIgnoreCase("tabular")){
					//if csv file and if contains variables, parse dataset object once and reuse for all variables
					Dataset datasetObj = new Dataset();
					
					//TODO: set data type
					//set dataset id
					//generating our own unique uuid rather than using the one created by portal
					datasetObj.createDatasetId();
					//uncomment below to use uuid created by portal
					/*obj = responseObj.get("id");
					if(obj != null && !obj.isNull())
						var.setDatasetId(obj.textValue());*/
					
					//set dataset name
					obj = responseObj.get("name");
					if(obj != null && !obj.isNull()){
						datasetObj.setDatasetName(obj.textValue());
					}
					
					//set variable description based on dataset description
					obj = responseObj.get("description");
					if(obj != null && !obj.isNull()) {
						datasetObj.setDescription(obj.textValue());
					}
					
					//set dataset URL
					datasetObj.setDatasetUrl(repo + "resource/" + dataset);
					
					//set tag list
					List<String> tagList = new ArrayList<String>();
					//add category information to tags
					obj = responseObj.get("category");
					if(obj != null && !obj.isNull())
						tagList.add(obj.textValue());
					//check if tags information exists, if so, append
					JsonNode tagArrObj = responseObj.get("tags");
					if(tagArrObj != null && !tagArrObj.isNull() && tagArrObj.isArray()){
						for (JsonNode tagObj : tagArrObj) {
							tagList.add(tagObj.textValue());
						}
					}
					datasetObj.setTagList(tagList);
					
					//set format list
					datasetObj.setFormatList(Collections.singleton(format.toLowerCase()));
					
					//set url list
					datasetObj.setUrlList(Collections.singletonList(repo + "resource/" + dataset));
					
					//set org
					obj = responseObj.get("attribution");
					if(obj != null && !obj.isNull()){
						datasetObj.setOrg(Utils.canonicalizeOrg(obj.textValue()));
					} else {
						//if url has edmonton, then its city of edmonton - deal with null values in organization
						if(repo.indexOf("edmonton") > -1) {
							datasetObj.setOrg(Utils.canonicalizeOrg("City of Edmonton"));
						}
					}
					
					//now create variable objects with dataset object passed in
					JsonNode varArrObj = responseObj.get("columns");
					//for each variable, plug in dataset information
					for (JsonNode varObj : varArrObj) {
						Variable var = null;
						
						//set variable name
						obj = varObj.get("name");
						if(obj != null && !obj.isNull()) {
							var = new Variable(obj.textValue(), datasetObj);
						}
						
						//set variable data type
						if(var != null) {
							obj = varObj.get("dataTypeName");
							if(obj != null && !obj.isNull())
								var.setDataType(obj.textValue());
						
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

	private List<String> getDatasetListFromHTML(String url) throws IOException {
		List<String> datasetList = new ArrayList<String>();
		
		//parse HTML into DOM - loop through all pages
		for (int i = 1; ; i++) {
			Document doc = Jsoup.connect(url + "?page=" + i).timeout(5000).get();
			Element table = doc.select("table.gridList").first();
			Elements rows = table.getElementsByAttributeValue("itemtype", "http://schema.org/Dataset");
			
			if(rows.size() == 0) break; //page has no results => we have reached the last page
			
			for (Element row : rows) {
				datasetList.add(row.attr("data-viewid"));
			}
			
		}
		return datasetList;
	}

}
