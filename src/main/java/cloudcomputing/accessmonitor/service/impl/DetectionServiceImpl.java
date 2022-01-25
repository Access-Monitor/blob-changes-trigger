package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.UNAUTHORIZED_MNG_ACCESS_KEY;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.UNAUTHORIZED_MNG_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.X_FUNCTIONS_KEY_HEADER;

import cloudcomputing.accessmonitor.model.api.UnauthorizedDetectionRequestBody;
import cloudcomputing.accessmonitor.model.persistence.DetectionAuditPerson;
import cloudcomputing.accessmonitor.service.DetectionService;
import cloudcomputing.accessmonitor.service.PersistenceService;
import com.google.gson.Gson;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyCandidate;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

public class DetectionServiceImpl implements DetectionService {

  private static final int MIN_TIME_FOR_NOTIFICATION = 20;
  private final PersistenceService persistenceService = new PersistenceServiceCosmosDBImpl();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public void auditDetection(IdentifyResult identifyResult) {
    identifyResult.candidates()
      .stream()
      .max(Comparator.comparing(IdentifyCandidate::confidence))
      .ifPresent(candidate -> registerCandidate(identifyResult, candidate));
  }

  @Override
  public void auditUnauthorizedDetection(IdentifyResult identifyResults, byte[] blobContent, String filename) {
    try {
      UnauthorizedDetectionRequestBody unauthorizedDetectionRequestBody =
        new UnauthorizedDetectionRequestBody(filename, identifyResults.faceId().toString(), blobContent);
      HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(UNAUTHORIZED_MNG_ENDPOINT))
        .header(X_FUNCTIONS_KEY_HEADER, UNAUTHORIZED_MNG_ACCESS_KEY)
        .POST(BodyPublishers.ofString(new Gson().toJson(unauthorizedDetectionRequestBody)))
        .build();
      httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
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
