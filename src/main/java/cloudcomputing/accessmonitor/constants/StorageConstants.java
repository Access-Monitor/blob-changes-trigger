package cloudcomputing.accessmonitor.constants;

public class StorageConstants {

  public static final String COSMOSDB_SUBSCRIPTION_KEY = System.getenv("CosmosDBKey");
  public static final String COSMOSDB_ENDPOINT = System.getenv("CosmosDBEndpoint");
  public static final String DATABASE_NAME = "AccessMonitorDb";
  public static final String AUDIT_CONTAINER_NAME = "DetectionAudit";
  public static final String UNAUTHORIZED_CONTAINER_NAME = "UnauthorizedMembers";
  public static final int MIN_TIME_FOR_NOTIFICATION = 10;

  public static final String BLOB_STORAGE_CONNECTION_STRING = System.getenv("BlobStorageConnectionString");
  public static final String ACCESSMONITORBLOB_CONTAINER = "accessmonitorblob";

}
