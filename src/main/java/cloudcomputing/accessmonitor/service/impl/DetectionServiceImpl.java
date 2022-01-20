package cloudcomputing.accessmonitor.service.impl;

import cloudcomputing.accessmonitor.model.persistence.DetectionAuditPerson;
import cloudcomputing.accessmonitor.service.DetectionService;
import cloudcomputing.accessmonitor.service.PersistenceService;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyCandidate;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

public class DetectionServiceImpl implements DetectionService {

  private static final int MIN_TIME_FOR_NOTIFICATION = 20;
  private final PersistenceService persistenceService = new PersistenceServiceCosmosDBImpl();

  @Override
  public void auditDetection(IdentifyResult identifyResult) {
    identifyResult.candidates()
      .stream()
      .max(Comparator.comparing(IdentifyCandidate::confidence))
      .ifPresent(candidate -> registerCandidate(identifyResult, candidate));
  }

  private void registerCandidate(IdentifyResult identifyResult, IdentifyCandidate candidate) {
    DetectionAuditPerson actualDetection =
      new DetectionAuditPerson(identifyResult.faceId().toString(), candidate.personId().toString(), candidate.confidence(),
        LocalDateTime.now());

    persistenceService.lastDetectionByPersonID(actualDetection.getPersonId())
      .stream()
      .findFirst()
      .ifPresentOrElse(lastDetection -> createDetectionWithBreak(actualDetection, lastDetection),
        () -> persistenceService.createDetection(actualDetection));
  }

  private void createDetectionWithBreak(DetectionAuditPerson actualDetection, DetectionAuditPerson lastDetection) {
    long elapsedMinutesSinceLastDetection =
      Duration.between(lastDetection.getDetectionTime(), actualDetection.getDetectionTime()).toMinutes();
    if (elapsedMinutesSinceLastDetection > MIN_TIME_FOR_NOTIFICATION) {
      persistenceService.createDetection(actualDetection);
    }
  }

}
