package org.aksw.es.bsbmloader.loader;

import java.sql.Connection;
import java.sql.SQLException;

import com.mysql.jdbc.Driver;

import org.aksw.es.bsbmloader.main.Main;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator; 

public class Database {
	private SimpleDriverDataSource datasource;
	private Connection connection = null;
	private static org.apache.log4j.Logger log = Logger.getLogger(Database.class);

	

	
	public void setConnectionProperties(String jdbcUrl, String username, String password){
		datasource = new SimpleDriverDataSource();
		datasource.setDriverClass(Driver.class);
		datasource.setUrl(jdbcUrl);
		datasource.setPassword(password);
		datasource.setUsername(username);
	}
	
	public void initBSBMDatabase() throws SQLException{
		log.info("Start Import Data!");
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(new ClassPathResource("dataset/01ProductFeature.sql"));
		populator.addScript(new ClassPathResource("dataset/02ProductType.sql"));
		populator.addScript(new ClassPathResource("dataset/03Producer.sql"));
		populator.addScript(new ClassPathResource("dataset/04Product.sql"));
		populator.addScript(new ClassPathResource("dataset/05ProductTypeProduct.sql"));
		populator.addScript(new ClassPathResource("dataset/06ProductFeatureProduct.sql"));
		populator.addScript(new ClassPathResource("dataset/07Vendor.sql"));
		populator.addScript(new ClassPathResource("dataset/08Offer.sql"));
		populator.addScript(new ClassPathResource("dataset/09Person.sql"));
		populator.addScript(new ClassPathResource("dataset/10Review.sql"));
		populator.addScript(new ClassPathResource("dataset/key.sql"));

		try {
			connection = DataSourceUtils.getConnection(datasource);
			populator.populate(connection);
		} finally {
			if (connection != null) {
				DataSourceUtils.releaseConnection(connection, datasource);
			}
		}
		log.info("Data Import done!");
	}
	
//	For Testing
	public SimpleDriverDataSource getDatasource(){
		return datasource;
	}
	
//	For Testing
	public Connection getConnection(){
		return connection;
	}
	
//	for Testing
	public  void setConnection(SimpleDriverDataSource datasource){
		connection = DataSourceUtils.getConnection(datasource);
	}
	

}