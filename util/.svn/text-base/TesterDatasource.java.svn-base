package util;

import org.apache.tomcat.jdbc.pool.PoolProperties;

public class TesterDatasource extends DatasourceConnection{

	@Override
	public void setDatasourceDetails(PoolProperties poolProp){
		poolProp.setUrl("jdbc:postgresql://" + Configuration.databaseIP + ":" + Configuration.databasePort + "/"+Configuration.databaseName);
    	poolProp.setDriverClassName("org.postgresql.Driver");
    	poolProp.setUsername(Configuration.testDatabaseUser);
    	poolProp.setPassword(Configuration.testDatabaseUserPasswd);
    }
}