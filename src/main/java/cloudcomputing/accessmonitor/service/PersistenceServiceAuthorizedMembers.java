package cloudcomputing.accessmonitor.service;

import cloudcomputing.accessmonitor.model.persistence.AuthorizedDetection;
import com.azure.cosmos.util.CosmosPagedIterable;

public interface PersistenceServiceAuthorizedMembers {

  CosmosPagedIterable<AuthorizedDetection> lastDetectionByPersonID(String personId);

  void createDetection(AuthorizedDetection actualDetection);
}
