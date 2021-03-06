package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.FaceAPIConstants.FACE_API_BASE_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.FaceAPIConstants.FACE_API_SUBSCRIPTION_KEY;
import static cloudcomputing.accessmonitor.constants.FaceAPIConstants.PERSON_GROUP_NAME;
import static cloudcomputing.accessmonitor.constants.HttpConstants.APPLICATION_OCTET_STREAM;
import static cloudcomputing.accessmonitor.constants.HttpConstants.CONTENT_TYPE_HEADER;
import static cloudcomputing.accessmonitor.constants.HttpConstants.OCP_APIM_SUBSCRIPTION_KEY_HEADER;
import static cloudcomputing.accessmonitor.service.JsonParserService.toJson;

import cloudcomputing.accessmonitor.exception.RollbackBlobException;
import cloudcomputing.accessmonitor.model.api.FaceIdentifyRequestBody;
import cloudcomputing.accessmonitor.model.api.FaceVerifyRequestBody;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.apache.commons.lang3.ArrayUtils;

public class FaceAPIServiceImpl implements FaceAPIService {

  private final HttpClient httpClient = HttpClient.newHttpClient();

  @Override
  public HttpResponse<String> faceDetect(byte[] blobContent) {
    try {
      HttpRequest httpRequest =
        buildBaseFaceAPIHttpRequest(FACE_API_BASE_ENDPOINT + "/face/v1.0/detect?recognitionModel=recognition_04").header(
          CONTENT_TYPE_HEADER, APPLICATION_OCTET_STREAM).POST(BodyPublishers.ofByteArray(blobContent)).build();
      return httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RollbackBlobException(e);
    }
  }

  @Override
  public HttpResponse<String> faceIdentify(String[] detectedFaceIds) {
    if (ArrayUtils.isNotEmpty(detectedFaceIds)) {
      try {
        FaceIdentifyRequestBody faceIdentifyRequestBody = new FaceIdentifyRequestBody(detectedFaceIds, 1, PERSON_GROUP_NAME);
        String faceIdentifyRequestBodyJSON = toJson(faceIdentifyRequestBody);
        HttpRequest httpRequest = buildBaseFaceAPIHttpRequest(FACE_API_BASE_ENDPOINT + "/face/v1.0/identify").POST(
          BodyPublishers.ofString(faceIdentifyRequestBodyJSON)).build();
        return httpClient.send(httpRequest, BodyHandlers.ofString());
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
        throw new RollbackBlobException(e);
      }
    }
    return null;
  }

  @Override
  public HttpResponse<String> faceVerify(String faceId1, String faceId2) {
    try {
      FaceVerifyRequestBody faceVerifyRequestBody = new FaceVerifyRequestBody(faceId1, faceId2);
      HttpRequest httpRequest = buildBaseFaceAPIHttpRequest(FACE_API_BASE_ENDPOINT + "/face/v1.0/verify").POST(
        BodyPublishers.ofString(toJson(faceVerifyRequestBody))).build();
      return httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  @Override
  public HttpResponse<String> getPerson(String personId) {
    try {
      HttpRequest httpRequest = buildBaseFaceAPIHttpRequest(
        FACE_API_BASE_ENDPOINT + String.format("/face/v1.0/persongroups/%s/persons/%s", PERSON_GROUP_NAME, personId)).GET().build();
      return httpClient.send(httpRequest, BodyHandlers.ofString());
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Builder buildBaseFaceAPIHttpRequest(String endpoint) {
    return HttpRequest.newBuilder(URI.create(endpoint)).header(OCP_APIM_SUBSCRIPTION_KEY_HEADER, FACE_API_SUBSCRIPTION_KEY);
  }

}
