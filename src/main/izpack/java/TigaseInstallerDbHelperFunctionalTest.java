package com.izforge.izpack.panels;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.izforge.izpack.installer.ResourceNotFoundException;
import com.izforge.izpack.panels.TigaseInstallerDBHelper.MsgTarget;
import com.izforge.izpack.panels.TigaseInstallerDBHelper.ResultMessage;
import com.izforge.izpack.panels.TigaseInstallerDBHelper.TigaseDBTask;

public class TigaseInstallerDbHelperFunctionalTest {

	public static void main(String[] args) throws ClassNotFoundException {
		 testPostgres();
	}

	// values
	// TigaseConfigConst.getProperty
	// root-db-uri jdbc:postgresql://localhost/postgres?user=postgres&password=1
	// root-tigase-db-uri jdbc:postgresql://localhost/tigasedb?user=postgres&password=1
	// --auth-db null 
	// --user-db null
	// --auth-db-uri 
	// --user-db-uri  jdbc:postgresql://localhost/tigasedb?user=tigase&password=tigase12
	//
	// variables:
	// admins: admin@mateusz-latop
	// adminsPwd: tigase

	private static void testPostgres() throws ClassNotFoundException {

		Properties variables = new Properties();
		variables.put("admins", "admin@mateusz-laptop");
		variables.put("adminsPwd", "tigase");
		variables.put("dbName", "tigasedb");
		variables.put("dbUser", "tigase");
		variables.put("dbPass", "tigase12");
		
		TigaseConfigConst.props.put("root-db-uri", "jdbc:postgresql://localhost/postgres?user=postgres&password=1");
		TigaseConfigConst.props.put("root-tigase-db-uri", "jdbc:postgresql://localhost/tigasedb?user=postgres&password=1");
		TigaseConfigConst.props.put("--user-db-uri", "jdbc:postgresql://localhost/tigasedb?user=tigase&password=tigase12");
		TigaseConfigConst.props.put("--user-db", "pgsql");

		final String databaseScriptsDir = "/home/mateusz/projects/tigase-server/database";
		final Map<String, String> resourceMappings = new HashMap<String, String>();
		resourceMappings.put("pgsql.create", "postgresql-create-db.sql");
		resourceMappings.put("pgsql.schema", "postgresql-schema-4.sql");
		resourceMappings.put("pgsql.sp", "postgresql-schema-4-sp.schema");
		resourceMappings.put("pgsql.props", "postgresql-schema-4-props.sql");

		TigaseInstallerDBHelper helper = new TigaseInstallerDBHelper() {
		
			protected java.io.InputStream getResource(String resource) 
			throws ResourceNotFoundException {
				String realResource = resourceMappings.get(resource);
				File resFile = new File(databaseScriptsDir, realResource);
				try {
					return new FileInputStream(resFile);
				} catch (FileNotFoundException e) {
					throw new ResourceNotFoundException();
				}
			};
			
		};

		
		for (final TigaseDBTask task : TigaseInstallerDBHelper.Tasks
				.getTasksInOrder()) {
			MsgTarget msgTarget = new MsgTarget() {
				public ResultMessage addResultMessage() {
					System.out.println("Task: " + task.getDescription() + " ");
					return new ResultMessage() {
						public void append(String msg) {
							System.out.print(msg);
						}
					};
				}
			};

			task.execute(helper, variables, msgTarget);
		}

	}

}
