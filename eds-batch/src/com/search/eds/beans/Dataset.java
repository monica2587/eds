package com.search.eds.beans;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Dataset {
	private String m_description;
	private String m_dataType;
	private String m_datasetId;
	private String m_datasetName;
	private String m_datasetUrl;
	private List<String> m_tagList;
	private Set<String> m_formatList;
	private List<String> m_urlList;
	private String m_org;
	
	public Dataset() {}
	
	public String getDescription() {
		return m_description;
	}

	public void setDescription(String description) {
		m_description = description;
	}

	public String getDataType() {
		return m_dataType;
	}

	public void setDataType(String datatype) {
		m_dataType = datatype;
	}
	
	public String getDatasetId() {
		return m_datasetId;
	}

	public void setDatasetId(String datasetId) {
		m_datasetId = datasetId;
	}
	
	public void createDatasetId(){
		m_datasetId = UUID.randomUUID().toString();
	}

	public String getDatasetName() {
		return m_datasetName;
	}

	public void setDatasetName(String datasetName) {
		m_datasetName = datasetName;
	}

	public String getDatasetUrl() {
		return m_datasetUrl;
	}

	public void setDatasetUrl(String datasetUrl) {
		m_datasetUrl = datasetUrl;
	}
	
	public List<String> getTagList() {
		return m_tagList;
	}
	
	public void setTagList(List<String> tagList) {
		m_tagList = tagList;
	}
	
	public Set<String> getFormatList() {
		return m_formatList;
	}
	
	public void setFormatList(Set<String> formatList){
		m_formatList = formatList;
	}
	
	public List<String> getUrlList() {
		return m_urlList;
	}
	
	public void setUrlList(List<String> urlList) {
		m_urlList = urlList;
	}
	
	public String getOrg() {
		return m_org;
	}
	
	public void setOrg(String org) {
		m_org = org;
	}
	
	
}
