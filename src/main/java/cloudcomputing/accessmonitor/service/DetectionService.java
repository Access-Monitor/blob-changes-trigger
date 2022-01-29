package cloudcomputing.accessmonitor.service;

import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;

public interface DetectionService {

  void auditDetection(IdentifyResult identifyResult, byte[] blobContent);

  void auditUnauthorizedDetection(IdentifyResult identifyResults, byte[] blobContent, String filename);
}
