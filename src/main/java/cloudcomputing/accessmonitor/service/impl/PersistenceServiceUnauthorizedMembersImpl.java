package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.StorageConstants.COSMOSDB_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.StorageConstants.COSMOSDB_SUBSCRIPTION_KEY;
import static cloudcomputing.accessmonitor.constants.StorageConstants.DATABASE_NAME;
import static cloudcomputing.accessmonitor.constants.StorageConstants.MIN_TIME_FOR_NOTIFICATION;
import static cloudcomputing.accessmonitor.constants.StorageConstants.UNAUTHORIZED_CONTAINER_NAME;

import cloudcomputing.accessmonitor.model.persistence.UnauthorizedDetection;
import cloudcomputing.accessmonitor.service.PersistenceServiceUnauthorizedMembers;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class PersistenceServiceUnauthorizedMembersImpl implements PersistenceServiceUnauthorizedMembers {

  private final CosmosContainer container;

  public PersistenceServiceUnauthorizedMembersImpl() {
    CosmosClient client = new CosmosClientBuilder().endpoint(COSMOSDB_ENDPOINT)
      .key(COSMOSDB_SUBSCRIPTION_KEY)
      .consistencyLevel(ConsistencyLevel.EVENTUAL)
      .buildClient();
    CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(DATABASE_NAME);
    CosmosDatabase database = client.getDatabase(databaseResponse.getProperties().getId());
    CosmosContainerResponse containerResponse =
      database.createContainerIfNotExists(new CosmosContainerProperties(UNAUTHORIZED_CONTAINER_NAME, "/faceId"));
    container = database.getContainer(containerResponse.getProperties().getId());
  }

  @Override
  public CosmosItemResponse<UnauthorizedDetection> createDetection(UnauthorizedDetection unauthorizedDetection) {
    return container.createItem(unauthorizedDetection, new PartitionKey(unauthorizedDetection.getFaceId()),
      new CosmosItemRequestOptions());
  }

  @Override
  public CosmosPagedIterable<UnauthorizedDetection> lastNotifiedDetections() {
    long backTimestamp = Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(MIN_TIME_FOR_NOTIFICATION)).getTime();
    return container.queryItems(
      "SELECT * FROM c WHERE c.detectionTimestamp > " + backTimestamp + " ORDER BY c.detectionTimestamp ASC",
      new CosmosQueryRequestOptions(), UnauthorizedDetection.class);
  }

}
