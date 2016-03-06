package org.aksw.es.bsbmloader.loader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.aksw.es.bsbmloader.parser.ElementParser;
import org.apache.log4j.Logger;
import org.apache.metamodel.UpdateCallback;
import org.apache.metamodel.UpdateScript;
import org.apache.metamodel.UpdateableDataContext;
import org.apache.metamodel.create.TableCreationBuilder;
import org.apache.metamodel.data.DataSet;
import org.apache.metamodel.data.Row;
import org.apache.metamodel.insert.RowInsertionBuilder;
import org.apache.metamodel.query.SelectItem;
import org.apache.metamodel.schema.Column;
import org.apache.metamodel.schema.Table;
import org.apache.metamodel.update.Update;

public class CouchLoader {
	private UpdateableDataContext dataContext;
	private static org.apache.log4j.Logger log = Logger.getLogger(MongoLoader.class);

	public void materializeSimpleData(String target, String source, String forgeinKey, String primaryKey) {
		Column forgeinColumn = dataContext.getTableByQualifiedLabel(target).getColumnByName(forgeinKey);
		Column primaryColumn = dataContext.getTableByQualifiedLabel(source).getColumnByName(primaryKey);
		Column[] sourceColumns = dataContext.getTableByQualifiedLabel(source).getColumns();
		Table targetTable = dataContext.getTableByQualifiedLabel(target);
		Map<String, Object> nestedObj = new HashMap<String, Object>();
		DataSet dataSet = dataContext.query().from(source).selectAll().execute();

		while (dataSet.next()) {
			Object pk = dataSet.getRow().getValue(primaryColumn);
			for (Column columns : sourceColumns) {
				if (dataSet.getRow().getValue(columns) != null) {
					nestedObj.put(columns.getName(), dataSet.getRow().getValue(columns));
				}
			}
			dataContext.executeUpdate(new Update(targetTable).where(forgeinColumn).eq(pk).value(forgeinColumn, nestedObj));
			log.info("Test");
		}
		dataSet.close();

	}

	public void materializeComplexData(String database, String sourceTable, String fkJoinTable, String joinTable,
			String secondSourceTable, String pkSecondSource, String pkFirstSource, String secondFkey) {
		Table tables = dataContext.getTableByQualifiedLabel(sourceTable + "_mat");
		if (tables == null) {
			/**
			 * Create Table productfeatureproduct_mat TODO Auslagern
			 **/
			dataContext.executeUpdate(new UpdateScript() {
				private String sourceTable;
				private String fkJoinTable;

				public void run(UpdateCallback callback) {
					TableCreationBuilder tableCreation = callback.createTable(dataContext.getDefaultSchema(),
							sourceTable + "_mat");

					tableCreation.withColumn(fkJoinTable);

					for (Column column : dataContext.getTableByQualifiedLabel(sourceTable).getColumns()) {
						tableCreation.withColumn(column.getName());
					}

					tableCreation.execute();

				}

				private UpdateScript init(String sourceTable, String fkJoinTable) {
					this.sourceTable = sourceTable;
					this.fkJoinTable = fkJoinTable;
					return this;
				}
			}.init(sourceTable, fkJoinTable));

			/**
			 * Insert Rows
			 */

			tables = dataContext.getTableByQualifiedLabel(sourceTable);
			DataSet dataSet = dataContext.query().from(tables).selectAll().execute(); // get
																		// all
																		// Product
																		// Data
			while (dataSet.next()) {

				insertRows(tables, tables.getColumns(), dataSet.getRow(), fkJoinTable, secondSourceTable, joinTable,
						pkSecondSource, pkFirstSource, secondFkey);
			}
			dataSet.close();
		}
	}

	private void insertRows(Table table, Column[] column, Row row, String forgeinKey, String secondSourceTable,
			String joinTable, String pkSecondSource, String pkFirstSource, String secondFkey) {
		dataContext.executeUpdate(new UpdateScript() {
			private Table table;
			private Column[] column;
			private Row row;
			private String forgeinKey;
			private String secondSourceTable;
			private String joinTable;
			private String pkSecondSource;
			private String pkFirstSource;
			private String secondFkey;

			public void run(UpdateCallback callback) {
				Table tables = dataContext.getTableByQualifiedLabel(table.getName());
				Table matTable = dataContext.getTableByQualifiedLabel(table.getName() + "_mat");
				RowInsertionBuilder rowsInsert = callback.insertInto(matTable);
				for (Column columnInsert : column) {
					rowsInsert.value(columnInsert.getName(), row.getValue(columnInsert.getColumnNumber()));

				}
				Table table = dataContext.getTableByQualifiedLabel(secondSourceTable);
				Table join = dataContext.getTableByQualifiedLabel(joinTable);

				rowsInsert.value(forgeinKey,
						getComplexData(join, table, forgeinKey, pkSecondSource,
								row.getValue(tables.getColumnByName(pkFirstSource)), secondFkey, secondSourceTable,
								pkSecondSource).toArray());
				rowsInsert.execute();

			}

			private UpdateScript init(Table table, Column[] column, Row row, String name, String secondSourceTable,
					String joinTable, String pkSecondSource, String pkFirstSource, String secondFkey) {
				this.row = row;
				this.table = table;
				this.column = column;
				this.forgeinKey = name;
				this.secondSourceTable = secondSourceTable;
				this.joinTable = joinTable;
				this.pkSecondSource = pkSecondSource;
				this.pkFirstSource = pkFirstSource;
				this.secondFkey = secondFkey;
				return this;
			}
		}.init(table, column, row, forgeinKey, secondSourceTable, joinTable, pkSecondSource, pkFirstSource,
				secondFkey));

	}

	private ArrayList<Map<String, Object>> getComplexData(Table joinTable, Table table, String fKey, String jKey,
			Object id, String secondFkey, String secondSourceTable, String pkSecondSource) {
		ArrayList<Object> listRow = new ArrayList<Object>();

		DataSet dataSetJoin = dataContext.query().from(joinTable).select(fKey).where(secondFkey)
				.eq(new ElementParser().getInteger(id)).execute();

		while (dataSetJoin.next()) {
			for (SelectItem column : dataSetJoin.getSelectItems()) {
				listRow.add(dataSetJoin.getRow().getValue(column));
			}
		}

		dataSetJoin.close();
		Iterator<Object> liter = listRow.iterator();
		ArrayList<Map<String, Object>> complexData = new ArrayList<Map<String, Object>>();
		while (liter.hasNext()) {
			int index = new ElementParser().getInteger(liter.next());

			DataSet dataSetTable = dataContext.query().from(secondSourceTable).selectAll().where(pkSecondSource).eq(index).execute();
			while (dataSetTable.next()) {
				Map<String, Object> nestedObj = new HashMap<String, Object>();
				for (SelectItem column : dataSetTable.getSelectItems()) {
					nestedObj.put(column.getColumn().getName(), dataSetTable.getRow().getValue(column));
				}
				complexData.add(nestedObj);
			}
			dataSetTable.close();
		}

		return complexData;
	}

	/**
	 * public void deleteDatabase() { dc.executeUpdate(new UpdateScript() {
	 * 
	 * public void run(UpdateCallback callback) { Schema schema =
	 * dc.getSchemaByName(schemaName); for (Table table : schema.getTables()) {
	 * if (!table.getName().contains("system")) {
	 * callback.dropTable(table).execute(); }
	 * 
	 * }
	 * 
	 * } }); }
	 **/

	public void setUpdateableDataContext(UpdateableDataContext dc) throws Exception {
		this.dataContext = dc;
	}

	public void insertRows(Table table, Column[] column, ArrayList<Row> rows) {
		dataContext.executeUpdate(new UpdateScript() {
			private Table table;
			private Column[] columns;
			private ArrayList<Row> rows = new ArrayList<Row>();

			public void run(UpdateCallback callback) {

				if (dataContext.getTableByQualifiedLabel(table.getName()) == null) {

				}

				Table tables = dataContext.getTableByQualifiedLabel(table.getName());

				RowInsertionBuilder rowsInsert = callback.insertInto(tables);
				for (Row insertRow : rows) {
					for (Column columnInsert : columns) {
						Object value = null;
						if (columnInsert.getType().isTimeBased()) {
							value = new ElementParser().getDate(insertRow.getValue(columnInsert));
						} else {
							value = insertRow.getValue(columnInsert);
						}
						rowsInsert.value(columnInsert.getName(), value);

					}
					rowsInsert.execute();

				}

			}

			private UpdateScript init(Table table, Column[] column, ArrayList<Row> rows) {
				this.columns = column;
				this.table = table;
				this.rows = rows;
				return this;
			}

		}.init(table, column, rows));
	}

}