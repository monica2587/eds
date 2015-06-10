package com.search.eds.beans;

import java.util.List;
import java.util.Set;

public class Variable {
	private String m_id;
	private String m_name;
	private String m_description;
	private String m_dataType;
	private String m_datasetId;
	private String m_datasetName;
	private String m_datasetUrl;
	private List<String> m_tagList;
	private Set<String> m_formatList;
	private List<String> m_urlList;
	private String m_org;
	
	public Variable(String name, Dataset d) {
		m_id = name;
		m_name = name;
		m_description = name + " is a variable in the dataset " + d.getDatasetName() + ". ";
		if(d.getDescription() != null && d.getDescription().trim().length() > 0){
			m_description += d.getDescription();
		}
		m_dataType = d.getDataType();
		m_datasetId = d.getDatasetId();
		m_datasetName = d.getDatasetName();
		m_datasetUrl = d.getDatasetUrl();
		m_tagList = d.getTagList();
		m_formatList = d.getFormatList();
		m_urlList = d.getUrlList();
		m_org = d.getOrg();
	}
	
	public String getId() {
		return m_id;
	}
	
	public void setId(String id) {
		m_id = id;
	}

	public String getName() {
		return m_name;
	}
	
	public void setName(String name) {
		m_name = name;
	}
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
