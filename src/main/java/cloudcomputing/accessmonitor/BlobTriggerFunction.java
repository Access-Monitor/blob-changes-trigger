package cloudcomputing.accessmonitor;

import static cloudcomputing.accessmonitor.constants.HttpConstants.SUCCESS;
import static cloudcomputing.accessmonitor.constants.StorageConstants.ACCESSMONITORBLOB_CONTAINER;
import static cloudcomputing.accessmonitor.service.JsonParserService.fromJson;

import cloudcomputing.accessmonitor.exception.RollbackBlobException;
import cloudcomputing.accessmonitor.service.BlobStorageService;
import cloudcomputing.accessmonitor.service.DetectionService;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import cloudcomputing.accessmonitor.service.impl.BlobStorageServiceImpl;
import cloudcomputing.accessmonitor.service.impl.DetectionServiceImpl;
import cloudcomputing.accessmonitor.service.impl.FaceAPIServiceImpl;
import com.azure.core.http.rest.Response;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.DetectedFace;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;

public class BlobTriggerFunction {

  private final FaceAPIService faceAPIService = new FaceAPIServiceImpl();
  private final DetectionService detectionService = new DetectionServiceImpl();
  private final BlobStorageService blobStorageService = new BlobStorageServiceImpl();

  @FunctionName("BlobTrigger")
  @StorageAccount("AzureWebJobsStorage")
  public void run(@BlobTrigger(name = "blobContent", path = "accessmonitorblob/{filename}", dataType = "binary") byte[] blobContent,
    @BindingName("filename") String filename, final ExecutionContext context) {

    Logger logger = context.getLogger();
    logger.info("Java Blob trigger function processed a blob. Name: " + filename + "\n  Size: " + blobContent.length + " Bytes");

    try {
      HttpResponse<String> detectFaceHttpResponse = faceAPIService.faceDetect(blobContent);
      if (detectFaceHttpResponse.statusCode() == SUCCESS) {
        DetectedFace[] detectedFaces = fromJson(detectFaceHttpResponse.body(), DetectedFace[].class);
        logger.info(String.format("Detected faces from blob: %s", Arrays.toString(detectedFaces)));

        if (ArrayUtils.isNotEmpty(detectedFaces)) {
          String[] detectedFaceIds = Arrays.stream(detectedFaces).map(f -> f.faceId().toString()).toArray(String[]::new);
          logger.info(String.format("Detected face IDs: %s", Arrays.toString(detectedFaceIds)));

          HttpResponse<String> identifyHttpResponse = faceAPIService.faceIdentify(detectedFaceIds);
          IdentifyResult[] identifyResults = fromJson(identifyHttpResponse.body(), IdentifyResult[].class);
          logger.info(String.format("Identification results: %s", Arrays.toString(identifyResults)));
          Arrays.stream(identifyResults).forEach(identifyResult -> processIdentificationResults(identifyResult, filename, logger));
        } else {
          logger.info("No faces detected from blob");
          throw new RollbackBlobException();
        }
      } else {
        logger.info(String.format("Error while processing face detection from blob: %s", detectFaceHttpResponse.statusCode()));
        throw new RollbackBlobException();
      }
    } catch (RollbackBlobException rollbackBlobException) {
      deleteBlob(filename, logger);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private void processIdentificationResults(IdentifyResult identifyResult, String filename, Logger logger) {
    if (identifyResult.candidates().isEmpty()) {
      detectionService.auditUnauthorizedDetection(filename, identifyResult.faceId().toString(), logger);
    } else {
      detectionService.auditAuthorizedDetection(identifyResult, filename, logger);
    }
  }

  private void deleteBlob(String filename, Logger logger) {
    Response<Void> deleteResponse = blobStorageService.deleteBlob(filename, ACCESSMONITORBLOB_CONTAINER);
    logger.log(Level.WARNING, String.format("Delete blob %s with response status %s", filename, deleteResponse.getStatusCode()));
  }

}
