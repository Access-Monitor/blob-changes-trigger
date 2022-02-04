package cloudcomputing.accessmonitor.service;

import cloudcomputing.accessmonitor.model.persistence.UnauthorizedDetection;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.util.CosmosPagedIterable;

public interface PersistenceServiceUnauthorizedMembers {

  CosmosItemResponse<UnauthorizedDetection> createDetection(UnauthorizedDetection unauthorizedDetection);

  CosmosPagedIterable<UnauthorizedDetection> lastNotifiedDetections();
}
