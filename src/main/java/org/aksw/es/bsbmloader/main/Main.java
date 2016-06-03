package org.aksw.es.bsbmloader.main;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;


import org.aksw.es.bsbmloader.database.DatabaseBuilder;
import org.aksw.es.bsbmloader.parser.MySQL;
import org.aksw.es.bsbmloader.parser.NoSQLParser;
import org.aksw.es.bsbmloader.starter.Starter;
import org.aksw.es.bsbmloader.starter.StarterFactory;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.log4j.Logger;
import org.apache.metamodel.data.Row;
import org.apache.metamodel.jdbc.dialects.IQueryRewriter;
import org.apache.metamodel.jdbc.dialects.MysqlQueryRewriter;
import org.apache.metamodel.jdbc.dialects.PostgresqlQueryRewriter;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;
import org.apache.commons.cli.CommandLine;

public class Main {
	private static org.apache.log4j.Logger log = Logger.getLogger(Main.class);

	public static void main(String[] args) {

		CommandLineParser parser = new BasicParser();

		try {
			CommandLine commandLine = parser.parse(getOption(), args);
			
			if (commandLine.getArgs() == null) {
				HelpFormatter formater = new HelpFormatter();
				formater.printHelp("Parameter", getOption());

			}
			
			if (commandLine.hasOption("h")) {
				HelpFormatter formater = new HelpFormatter();
				formater.printHelp("Parameter", getOption());
			}

			if (commandLine.hasOption("importToMysql") && hasMySQLConnectionProperties(commandLine)) {
				if (!commandLine.hasOption("file")) {
					startIntDatabase(commandLine.getOptionValue("u"), commandLine.getOptionValue("p"),
							commandLine.getOptionValue("urlMysql"));
				} else {
					startIntDatabase(commandLine.getOptionValue("u"), commandLine.getOptionValue("p"),
							commandLine.getOptionValue("urlMysql"), commandLine.getOptionValue("file"));
				}

			}

			interpretCommandLine(commandLine);

			

		} catch (Exception e) {
			log.error("", e);
		}

	}
	//TODO 	restructuring function 
	private static void interpretCommandLine(CommandLine commandLine) throws Exception{
		Starter starter = new StarterFactory().getStarter(commandLine);

		if (commandLine.hasOption("parseToMongo") && hasMySQLConnectionProperties(commandLine)) {
			startParseToNoSQL(commandLine, starter.createConnectionProperties(commandLine));
		}

		if (commandLine.hasOption("parseToCouch") && hasMySQLConnectionProperties(commandLine)) {
			startParseToNoSQL(commandLine, starter.createConnectionProperties(commandLine));
		}

		if (commandLine.hasOption("parseToExcel") && hasMySQLConnectionProperties(commandLine)) {
			startParseToNoSQL(commandLine, starter.createConnectionProperties(commandLine));
		}

		if (commandLine.hasOption("parseToElastic") && hasMySQLConnectionProperties(commandLine)) {
			startParseToNoSQL(commandLine, starter.createConnectionProperties(commandLine));
		}
		
		if (commandLine.hasOption("parseToJdbc") && hasMySQLConnectionProperties(commandLine)) {
			startParseToNoSQL(commandLine, starter.createConnectionProperties(commandLine));
		}

		if (commandLine.hasOption("materializeMongo")) {
			if (commandLine.hasOption("hostNosql") && commandLine.hasOption("portNosql")) {
				log.info("start materializeMongo"); //TODO dont trigger by simple mat.
				starter.startMaterializeSimple(commandLine);
				log.info("Done");

			}
		}
		
		if (commandLine.hasOption("materializeElastic")) {
			if (commandLine.hasOption("portNosql")) {
				log.info("start materializeMongo");
				starter.startMaterializeSimple(commandLine);
				log.info("Done");

			}
		}

		if (commandLine.hasOption("materializeCouch")) {
			if (commandLine.hasOption("hostNosql") && commandLine.hasOption("portNosql")) {
				log.info("start materializeCouch");
				starter.startMaterializeSimple(commandLine);
				log.info("Done");

			}
		}

		if (commandLine.hasOption("materializeMongo") && commandLine.hasOption("join")) {
			if (commandLine.hasOption("hostNosql") && commandLine.hasOption("portNosql")) {
				log.info("start materializeMongo");
				starter.startMaterializeComplex(commandLine);
				log.info("Done");

			}
		}

		if (commandLine.hasOption("materializeCouch") && commandLine.hasOption("join")) {
			if (commandLine.hasOption("hostNosql") && commandLine.hasOption("portNosql")) {
				log.info("start materializeCouch");
				starter.startMaterializeComplex(commandLine);
				log.info("Done");

			}
		}
	}

	private static void startIntDatabase(String username, String password, String url) throws Exception {
		DatabaseBuilder db = new DatabaseBuilder();
		db.setConnectionProperties(url, username, password);
		db.initBSBMDatabase();
	}

	private static void startIntDatabase(String username, String password, String url, String path) throws Exception {
		DatabaseBuilder db = new DatabaseBuilder();
		db.setConnectionProperties(url, username, password);
		db.initBSBMDatabase(path);

	}
	//TODO 	restructuring function 
	private static void startParseToNoSQL(CommandLine commandLine, NoSQLParser nosql) throws Exception {
		log.info("Start Parse to NoSQL/JDBC");
		String[] sqldatabase = commandLine.getOptionValue("urlMysql").split("/");
		BlockingQueue<Row> queue = new ArrayBlockingQueue<Row>(1000);
		MySQL mysql = new MySQL(queue);
		
		mysql.setConnectionProperties(commandLine.getOptionValue("urlMysql"), commandLine.getOptionValue("u"),
				commandLine.getOptionValue("p"));

		
		
		for (Table table : mysql.getTableMysql(sqldatabase[sqldatabase.length -1 ])) {
			if (commandLine.hasOption("d")) {
				 nosql.deleteDatabase(table.getName());
			}
			Column[] column = mysql.getColumnMysql(table.getName(), sqldatabase[sqldatabase.length -1 ]);
			mysql.setTable(table);
			mysql.setDatabase(sqldatabase[sqldatabase.length -1 ]);
			if(!commandLine.hasOption("targetUrl")){
				nosql.createTable(table, column, null);
			}
			else{
				nosql.createTable(table, column,  getIQueryRewriter(commandLine.getOptionValue("targetUrl")));
			}
			

			nosql.setQueue(queue, table, column);
			Thread mysqlThread = new Thread(mysql);
			Thread firstNosqlThread = new Thread(nosql);
			Thread secondNosqlThread = new Thread(nosql);
			Thread thirdNosqlThread = new Thread(nosql);
			mysqlThread.start();
			if(!commandLine.hasOption("targetUrl")){
				firstNosqlThread.start();
				secondNosqlThread.start();
				thirdNosqlThread.start();

				while (mysqlThread.isAlive() || firstNosqlThread.isAlive() || secondNosqlThread.isAlive() || thirdNosqlThread.isAlive()) {
					Thread.sleep(1);
				}
				
			}
			else{
				firstNosqlThread.start();
				while (mysqlThread.isAlive() || firstNosqlThread.isAlive()){
					Thread.sleep(1);
				}
			}

		}

		log.info("Done");
	}
	//TODO 	restructuring function 
	private static Options getOption() {
		Options options = new Options();
		options.addOption("h", "help", false, "Show help");
		options.addOption("importToMysql", false, "Start to import the data to MySQL");
		options.addOption("u", "userMysql", true, "The username in MySQL");
		options.addOption("p", "passwordMysql", true, "The password in MySQL");
		options.addOption("urlMysql", true, "The jdbc-url for MySQL. For example: jdbc:mysql://localhost/benchmark");
		options.addOption("portNosql", true, "The username in MongoDB");
		options.addOption("hostNosql", true, "the password in MongoDB");
		options.addOption("parseToMongo", false, "Start to parse the MySQl databaste to a MongoDB database");
		options.addOption("file", true, "Use your BSBM sqlfiles");
		options.addOption("d", "deleteDatabase", false, "Delete an existing NoSQL Database");
		options.addOption("databaseName", true, "Set the name of the NoSQL database");
		options.addOption("materializeMongo", false, "Materialize a N to One Relationship in MongoDB");
		options.addOption("target", true, "The target table with the forgein key");
		options.addOption("source", true, "The source table with the primary key");
		options.addOption("fk", "forgeinKey", true, "The forgein key");
		options.addOption("pk", "primayKey", true, "The primary key");
		options.addOption("join", true, "The join table with the forgein key");
		options.addOption("parseToCouch", false, "Import the mysql databaste to couchdb");
		options.addOption("fkJoinTable", true, "The forgeinkey of the join table");
		options.addOption("secondSource", true, "The secound source table");
		options.addOption("pkSecond", true, "The primary key of the second table");
		options.addOption("secondFkey", true, "The second key of the join table");
		options.addOption("passwordCouch", true, "The host for CouchDB");
		options.addOption("userCouch", true, "The host for CouchDB");
		options.addOption("materializeCouch", false, "Materialize a N to One Relationship in MongoDB");
		options.addOption("materializeElastic", false, "Materialize a N to One Relationship in MongoDB");
		options.addOption("parseToExcel", false, "Import the mysql databaste to Excel");
		options.addOption("parseToElastic", false, "Import the mysql databaste to Excel");
		options.addOption("excelFile", true, "Use your BSBM sqlfiles");
		options.addOption("targetUrl",true,"Url of target jdbc");
		options.addOption("user", true, "username");
		options.addOption("password", true, "password");
		options.addOption("parseToJdbc", false, "Import the mysql databaste to JDBC");
		options.addOption("objectId", false, "use only ObjectId");
		
		return options;

	}
	
	private static IQueryRewriter getIQueryRewriter(String url){
	
		if(url.contains("mysql")){
			return new MysqlQueryRewriter(null);
		}
		if(url.contains("postgesql")){
			return new PostgresqlQueryRewriter(null);
		}
		
		return null;
	}

	private static boolean hasMySQLConnectionProperties(CommandLine commandLine) throws Exception {
		boolean hasProperties = false;

		if (commandLine.hasOption("u") && commandLine.hasOption("urlMysql")) {
			hasProperties = true;
		} else {
			throw new Exception("Missing parameter");
		}

		return hasProperties;
	}

}