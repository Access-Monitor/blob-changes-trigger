package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.DatabaseConstants.AUDIT_CONTAINER_NAME;
import static cloudcomputing.accessmonitor.constants.DatabaseConstants.COSMOSDB_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.DatabaseConstants.COSMOSDB_SUBSCRIPTION_KEY;
import static cloudcomputing.accessmonitor.constants.DatabaseConstants.DATABASE_NAME;

import cloudcomputing.accessmonitor.model.persistence.DetectionAuditPerson;
import cloudcomputing.accessmonitor.service.PersistenceService;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;

public class PersistenceServiceCosmosDBImpl implements PersistenceService {

  private final CosmosContainer container;

  public PersistenceServiceCosmosDBImpl() {
    CosmosClient client = new CosmosClientBuilder().endpoint(COSMOSDB_ENDPOINT)
      .key(COSMOSDB_SUBSCRIPTION_KEY)
      .consistencyLevel(ConsistencyLevel.EVENTUAL)
      .buildClient();
    CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(DATABASE_NAME);
    CosmosDatabase database = client.getDatabase(databaseResponse.getProperties().getId());
    CosmosContainerResponse containerResponse =
      database.createContainerIfNotExists(new CosmosContainerProperties(AUDIT_CONTAINER_NAME, "/personId"));
    container = database.getContainer(containerResponse.getProperties().getId());
  }

  @Override
  public CosmosPagedIterable<DetectionAuditPerson> lastDetectionByPersonID(String personId) {
    return container.queryItems("SELECT * FROM c WHERE c.personId='" + personId + "' ORDER BY c.detectionTimestamp ASC",
      new CosmosQueryRequestOptions(), DetectionAuditPerson.class);
  }

  @Override
  public void createDetection(DetectionAuditPerson actualDetection) {
    container.createItem(actualDetection, new PartitionKey(actualDetection.getPersonId()), new CosmosItemRequestOptions());
  }

}
