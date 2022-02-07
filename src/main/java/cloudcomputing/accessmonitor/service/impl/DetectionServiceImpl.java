package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.UNAUTHORIZED_MNG_ACCESS_KEY;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.UNAUTHORIZED_MNG_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.UnauthorizedManagerConstants.X_FUNCTIONS_KEY_HEADER;
import static cloudcomputing.accessmonitor.service.JsonParserService.fromJson;
import static cloudcomputing.accessmonitor.service.JsonParserService.toJson;

import cloudcomputing.accessmonitor.exception.RollbackBlobException;
import cloudcomputing.accessmonitor.model.persistence.AuthorizedDetection;
import cloudcomputing.accessmonitor.model.persistence.UnauthorizedDetection;
import cloudcomputing.accessmonitor.service.DetectionService;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import cloudcomputing.accessmonitor.service.PersistenceServiceAuthorizedMembers;
import cloudcomputing.accessmonitor.service.PersistenceServiceUnauthorizedMembers;
import com.azure.cosmos.models.CosmosItemResponse;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyCandidate;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.Person;
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
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DetectionServiceImpl implements DetectionService {

  private static final int MIN_TIME_FOR_NOTIFICATION = 20;
  private final PersistenceServiceAuthorizedMembers persistenceServiceAuthorizedMembers =
    new PersistenceServiceAuthorizedMembersImpl();
  private final PersistenceServiceUnauthorizedMembers persistenceServiceUnauthorizedMembers =
    new PersistenceServiceUnauthorizedMembersImpl();
  private final FaceAPIService faceAPIService = new FaceAPIServiceImpl();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public void auditAuthorizedDetection(IdentifyResult identifyResult, String filename, Logger logger) {
    try {
      identifyResult.candidates()
        .stream()
        .max(Comparator.comparing(IdentifyCandidate::confidence))
        .ifPresent(candidate -> registerAuthorizedDetection(identifyResult, candidate, filename, logger));
    } catch (Exception e) {
      e.printStackTrace();
      throw new RollbackBlobException(e);
    }
  }

  @Override
  public void auditUnauthorizedDetection(String filename, String faceId, Logger logger) {

    Optional<Boolean> faceAlreadyNotified;
    UnauthorizedDetection unauthorizedDetection;
    try {
      faceAlreadyNotified = persistenceServiceUnauthorizedMembers.lastNotifiedDetections().stream().map(notifiedDetection -> {
        logger.info(String.format("Notified detection in past %s minutes found: faceId %s, filename %s", MIN_TIME_FOR_NOTIFICATION,
          notifiedDetection.getFaceId(), notifiedDetection.getId()));

        HttpResponse<String> verifyResponse = faceAPIService.faceVerify(notifiedDetection.getFaceId(), faceId);
        return fromJson(verifyResponse.body(), VerifyResult.class).isIdentical();
      }).filter(identical -> identical).findAny();

      unauthorizedDetection =
        new UnauthorizedDetection(UUID.randomUUID().toString(), faceId, LocalDateTime.now(ZoneOffset.UTC), filename);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RollbackBlobException(e);
    }

    faceAlreadyNotified.ifPresentOrElse(face -> {
      logger.log(Level.WARNING, String.format("FaceId %s has been already notified", faceId));
      throw new RollbackBlobException();
    }, () -> {
      registerUnauthorizedDetection(unauthorizedDetection, logger);
      emailNotifyUnauthorizedDetection(unauthorizedDetection);
    });
  }

  private void registerAuthorizedDetection(IdentifyResult identifyResult, IdentifyCandidate candidate, String filename,
    Logger logger) {
    HttpResponse<String> getPersonResponse = faceAPIService.getPerson(candidate.personId().toString());
    Person person = fromJson(getPersonResponse.body(), Person.class);

    AuthorizedDetection actualDetection =
      new AuthorizedDetection(identifyResult.faceId().toString(), candidate.personId().toString(), candidate.confidence(),
        LocalDateTime.now(ZoneOffset.UTC), filename, person.name());

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
      logger.log(Level.WARNING,
        String.format("Authorized detection already registered %s minutes ago", elapsedMinutesSinceLastDetection));
      throw new RollbackBlobException();
    }
  }

  private void registerUnauthorizedDetection(UnauthorizedDetection unauthorizedDetection, Logger logger) {
    try {
      CosmosItemResponse<UnauthorizedDetection> createDetectionResponse =
        persistenceServiceUnauthorizedMembers.createDetection(unauthorizedDetection);
      logger.log(Level.INFO,
        String.format("Registered UNAUTHORIZED detection with status: %s", createDetectionResponse.getStatusCode()));
    } catch (Exception e) {
      throw new RollbackBlobException(e);
    }
  }

  private void emailNotifyUnauthorizedDetection(UnauthorizedDetection unauthorizedDetection) {
    try {
      HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(UNAUTHORIZED_MNG_ENDPOINT))
        .header(X_FUNCTIONS_KEY_HEADER, UNAUTHORIZED_MNG_ACCESS_KEY)
        .POST(BodyPublishers.ofString(toJson(unauthorizedDetection)))
        .build();
      httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
