package cloudcomputing.accessmonitor.service;

import cloudcomputing.accessmonitor.model.persistence.DetectionAuditPerson;
import com.azure.cosmos.util.CosmosPagedIterable;

public interface PersistenceService {

  CosmosPagedIterable<DetectionAuditPerson> lastDetectionByPersonID(String personId);

  void createDetection(DetectionAuditPerson actualDetection);
}
