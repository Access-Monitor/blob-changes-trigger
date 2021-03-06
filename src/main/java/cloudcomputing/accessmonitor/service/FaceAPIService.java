package cloudcomputing.accessmonitor.service;

import java.net.http.HttpResponse;

public interface FaceAPIService {

  HttpResponse<String> faceDetect(byte[] blobContent);

  HttpResponse<String> faceIdentify(String[] detectedFaceIds);

  HttpResponse<String> faceVerify(String faceId1, String faceId2);

  HttpResponse<String> getPerson(String personId);
}
