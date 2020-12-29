package co.il.avivsmile.util;
import org.hibernate.dialect.PostgreSQL9Dialect;

	import org.hibernate.dialect.function.SQLFunctionTemplate;
	import org.hibernate.type.StandardBasicTypes;
public class SqlDialectExtending extends PostgreSQL9Dialect{
	
//	registerFunction( "datediff", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "datediff(interval, ?1, ?2)" ) );

	

	    public SqlDialectExtending() {
	        registerFunction("TIMESTAMPDIFF", new SQLFunctionTemplate(StandardBasicTypes.INTEGER, "TIMESTAMPDIFF(MINUTE, ?1, ?2)"));
	    
	}

}
