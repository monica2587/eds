package com.search.eds.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

public class Config {
	private static final Logger s_logger = Logger.getLogger(Config.class.getName());
	private static Config s_instance = new Config();
	private Properties m_prop = null;
	
	private Config(){
		m_prop = new Properties();
		try {
			m_prop.load(new FileInputStream("resources/config.properties"));
		} catch (FileNotFoundException e) {
			s_logger.severe("Could not find config.properties: " + e.getStackTrace());
		} catch (IOException e) {
			s_logger.severe("Could not load config.properties: " + e.getStackTrace());
		}
	}
	
	public static Config getInstance(){
		return s_instance;
	}
	
	public List<String> getCKANRepoList(){
		return getRepoListFromStr(m_prop.getProperty("repo.ckan", ""));	
	}
	
	public List<String> getSocrataRepoList(){
		return getRepoListFromStr(m_prop.getProperty("repo.socrata", ""));
	}
	
	public List<String> getCustomRepoList(){
		return getRepoListFromStr(m_prop.getProperty("repo.custom", ""));
	}
	
	private List<String> getRepoListFromStr(String repoListStr){
		List<String> repoList = new ArrayList<String>();
		if(repoListStr.trim().length() > 0) {
			for (String str : repoListStr.split(",")) {
				//remove any whitespace included in the properties
				repoList.add(str.trim());
			}
		}
		return repoList;
	}
}
