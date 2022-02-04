package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.FaceAPIConstants.FACE_API_BASE_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.FaceAPIConstants.FACE_API_SUBSCRIPTION_KEY;
import static cloudcomputing.accessmonitor.constants.FaceAPIConstants.TEST_PERSON_GROUP;
import static cloudcomputing.accessmonitor.constants.HttpConstants.APPLICATION_OCTET_STREAM;
import static cloudcomputing.accessmonitor.constants.HttpConstants.CONTENT_TYPE_HEADER;
import static cloudcomputing.accessmonitor.constants.HttpConstants.OCP_APIM_SUBSCRIPTION_KEY_HEADER;

import cloudcomputing.accessmonitor.model.api.FaceIdentifyRequestBody;
import cloudcomputing.accessmonitor.model.api.FaceVerifyRequestBody;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import com.google.gson.Gson;
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
  public HttpResponse<String> faceDetect(byte[] blobContent) throws IOException, InterruptedException {
    HttpRequest httpRequest =
      buildBaseFaceAPIHttpRequest(FACE_API_BASE_ENDPOINT + "/face/v1.0/detect?recognitionModel=recognition_04").header(
        CONTENT_TYPE_HEADER, APPLICATION_OCTET_STREAM).POST(BodyPublishers.ofByteArray(blobContent)).build();
    return httpClient.send(httpRequest, BodyHandlers.ofString());
  }

  @Override
  public HttpResponse<String> faceIdentify(String[] detectedFaceIds) throws IOException, InterruptedException {
    if (ArrayUtils.isNotEmpty(detectedFaceIds)) {
      FaceIdentifyRequestBody faceIdentifyRequestBody = new FaceIdentifyRequestBody(detectedFaceIds, 1, TEST_PERSON_GROUP);
      String faceIdentifyRequestBodyJSON = new Gson().toJson(faceIdentifyRequestBody, FaceIdentifyRequestBody.class);
      HttpRequest httpRequest = buildBaseFaceAPIHttpRequest(FACE_API_BASE_ENDPOINT + "/face/v1.0/identify").POST(
        BodyPublishers.ofString(faceIdentifyRequestBodyJSON)).build();
      return httpClient.send(httpRequest, BodyHandlers.ofString());
    }
    return null;
  }

  @Override
  public HttpResponse<String> faceVerify(String faceId1, String faceId2) {
    try {
      FaceVerifyRequestBody faceVerifyRequestBody = new FaceVerifyRequestBody(faceId1, faceId2);
      HttpRequest httpRequest = buildBaseFaceAPIHttpRequest(FACE_API_BASE_ENDPOINT + "/face/v1.0/verify").POST(
        BodyPublishers.ofString(new Gson().toJson(faceVerifyRequestBody, FaceVerifyRequestBody.class))).build();
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
