package cloudcomputing.accessmonitor.service;

import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import java.util.logging.Logger;

public interface DetectionService {

  void auditAuthorizedDetection(IdentifyResult identifyResult, String filename);

  void auditUnauthorizedDetection(String filename, String faceId, Logger logger);
}
