package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.StorageConstants.ACCESSMONITORBLOB_CONTAINER;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.UNAUTHORIZED_MNG_ACCESS_KEY;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.UNAUTHORIZED_MNG_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.X_FUNCTIONS_KEY_HEADER;

import cloudcomputing.accessmonitor.model.persistence.AuthorizedDetection;
import cloudcomputing.accessmonitor.model.persistence.UnauthorizedDetection;
import cloudcomputing.accessmonitor.service.BlobStorageService;
import cloudcomputing.accessmonitor.service.DetectionService;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import cloudcomputing.accessmonitor.service.PersistenceServiceAuthorizedMembers;
import cloudcomputing.accessmonitor.service.PersistenceServiceUnauthorizedMembers;
import com.azure.core.http.rest.Response;
import com.azure.cosmos.models.CosmosItemResponse;
import com.google.gson.Gson;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyCandidate;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.VerifyResult;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DetectionServiceImpl implements DetectionService {

  private static final int MIN_TIME_FOR_NOTIFICATION = 20;
  private final PersistenceServiceAuthorizedMembers persistenceServiceAuthorizedMembers =
    new PersistenceServiceAuthorizedMembersImpl();
  private final PersistenceServiceUnauthorizedMembers persistenceServiceUnauthorizedMembers =
    new PersistenceServiceUnauthorizedMembersImpl();
  private final FaceAPIService faceAPIService = new FaceAPIServiceImpl();
  private final BlobStorageService blobStorageService = new BlobStorageServiceImpl();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public void auditAuthorizedDetection(IdentifyResult identifyResult, String filename, Logger logger) {
    identifyResult.candidates()
      .stream()
      .max(Comparator.comparing(IdentifyCandidate::confidence))
      .ifPresent(candidate -> registerAuthorizedDetection(identifyResult, candidate, filename, logger));
  }

  @Override
  public void auditUnauthorizedDetection(String filename, String faceId, Logger logger) {

    Optional<Boolean> faceAlreadyNotified =
      persistenceServiceUnauthorizedMembers.lastNotifiedDetections().stream().map(notifiedDetection -> {
        logger.info(String.format("Notified detection in past %s minutes found: faceId %s, filename %s", MIN_TIME_FOR_NOTIFICATION,
          notifiedDetection.getFaceId(), notifiedDetection.getId()));

        HttpResponse<String> verifyResponse = faceAPIService.faceVerify(notifiedDetection.getFaceId(), faceId);
        return new Gson().fromJson(verifyResponse.body(), VerifyResult.class).isIdentical();
      }).filter(identical -> identical).findAny();

    UnauthorizedDetection unauthorizedDetection =
      new UnauthorizedDetection(filename, faceId, LocalDateTime.now(ZoneOffset.UTC), filename);

    faceAlreadyNotified.ifPresentOrElse(face -> {
      logger.log(Level.WARNING, String.format("FaceId %s has been already notified", face));
      deleteBlob(filename, logger);
    }, () -> {
      registerUnauthorizedDetection(unauthorizedDetection, logger);
      emailNotifyUnauthorizedDetection(unauthorizedDetection);
    });
  }

  private void registerAuthorizedDetection(IdentifyResult identifyResult, IdentifyCandidate candidate, String filename,
    Logger logger) {
    AuthorizedDetection actualDetection =
      new AuthorizedDetection(identifyResult.faceId().toString(), candidate.personId().toString(), candidate.confidence(),
        LocalDateTime.now(ZoneOffset.UTC), filename);

    persistenceServiceAuthorizedMembers.lastDetectionByPersonID(actualDetection.getPersonId())
      .stream()
      .findFirst()
      .ifPresentOrElse(lastDetection -> registerDetectionAvoidingFlood(actualDetection, lastDetection.getDetectionTime(), logger),
        () -> persistenceServiceAuthorizedMembers.createDetection(actualDetection));
  }

  private void registerDetectionAvoidingFlood(AuthorizedDetection actualDetection, LocalDateTime detectionTime, Logger logger) {
    long elapsedMinutesSinceLastDetection = Duration.between(detectionTime, actualDetection.getDetectionTime()).toMinutes();
    if (elapsedMinutesSinceLastDetection > MIN_TIME_FOR_NOTIFICATION) {
      persistenceServiceAuthorizedMembers.createDetection(actualDetection);
    } else {
      deleteBlob(actualDetection.getFilename(), logger);
      logger.log(Level.WARNING,
        String.format("Authorized detection already registered %s minutes ago", elapsedMinutesSinceLastDetection));
    }
  }

  private void registerUnauthorizedDetection(UnauthorizedDetection unauthorizedDetection, Logger logger) {
    CosmosItemResponse<UnauthorizedDetection> createDetectionResponse =
      persistenceServiceUnauthorizedMembers.createDetection(unauthorizedDetection);
    logger.log(Level.INFO,
      String.format("Registered UNAUTHORIZED detection with status: %s", createDetectionResponse.getStatusCode()));
  }

  private void emailNotifyUnauthorizedDetection(UnauthorizedDetection unauthorizedDetection) {
    try {
      HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(UNAUTHORIZED_MNG_ENDPOINT))
        .header(X_FUNCTIONS_KEY_HEADER, UNAUTHORIZED_MNG_ACCESS_KEY)
        .POST(BodyPublishers.ofString(new Gson().toJson(unauthorizedDetection)))
        .build();
      httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void deleteBlob(String filename, Logger logger) {
    Response<Void> deleteResponse = blobStorageService.deleteBlob(filename, ACCESSMONITORBLOB_CONTAINER);
    logger.log(Level.WARNING, String.format("Delete blob %s with response status %s", filename, deleteResponse.getStatusCode()));
  }
}
