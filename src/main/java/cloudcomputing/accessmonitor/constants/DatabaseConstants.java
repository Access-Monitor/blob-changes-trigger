package cloudcomputing.accessmonitor.constants;

public class DatabaseConstants {

  public static final String COSMOSDB_SUBSCRIPTION_KEY = System.getenv("CosmosDBKey");
  public static final String COSMOSDB_ENDPOINT = System.getenv("CosmosDBEndpoint");
  public static final String DATABASE_NAME = "AccessMonitorDb";
  public static final String AUDIT_CONTAINER_NAME = "DetectionAudit";
}
