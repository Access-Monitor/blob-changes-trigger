package cloudcomputing.accessmonitor.service;

import java.io.IOException;
import java.net.http.HttpResponse;

public interface FaceAPIService {

  HttpResponse<String> faceDetect(byte[] blobContent) throws IOException, InterruptedException;

  HttpResponse<String> faceIdentify(String[] detectedFaceIds) throws IOException, InterruptedException;
}
