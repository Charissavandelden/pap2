package nl.topicus.injection.mapping;

import java.lang.invoke.MethodType;
import java.util.Map;

public class MetadataHelper {
    
    private static final Map<Class<?>, String> DB_COLUMN_TYPES = Map.ofEntries(
    		Map.entry(Integer.class, "INT"),
    		Map.entry(Long.class, "BIGINT"),
    		Map.entry(String.class, "VARCHAR(255)"),
    		Map.entry(Boolean.class, "BOOLEAN"),
    		Map.entry(Enum.class, "VARCHAR(50)")
    );
    
    private static final Class<?> wrap(Class<?> javaClass)
    {
    	return MethodType.methodType(javaClass).wrap().returnType();
    }
    
    public static String columnTypeFor(Class<?> type)
    {
    	return " " + DB_COLUMN_TYPES.get(wrap(type));
    }
}
