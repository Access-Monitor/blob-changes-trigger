package cloudcomputing.accessmonitor;

import static cloudcomputing.accessmonitor.constants.HttpConstants.SUCCESS;

import cloudcomputing.accessmonitor.service.DetectionService;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import cloudcomputing.accessmonitor.service.impl.DetectionServiceImpl;
import cloudcomputing.accessmonitor.service.impl.FaceAPIServiceImpl;
import com.google.gson.Gson;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.DetectedFace;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.IdentifyResult;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

public class BlobTriggerFunction {

  private final FaceAPIService faceAPIService = new FaceAPIServiceImpl();
  private final DetectionService detectionService = new DetectionServiceImpl();

  @FunctionName("BlobTrigger")
  @StorageAccount("AzureWebJobsStorage")
  public void run(@BlobTrigger(name = "blobContent", path = "accessmonitorblob/{filename}", dataType = "binary") byte[] blobContent,
    @BindingName("filename") String filename, final ExecutionContext context) {

    context.getLogger()
      .info("Java Blob trigger function processed a blob. Name: " + filename + "\n  Size: " + blobContent.length + " Bytes");

    try {
      HttpResponse<String> detectFaceHttpResponse = faceAPIService.faceDetect(blobContent);
      if (detectFaceHttpResponse.statusCode() == SUCCESS) {
        DetectedFace[] detectedFaces = new Gson().fromJson(detectFaceHttpResponse.body(), DetectedFace[].class);
        context.getLogger().info(String.format("Detected faces from blob: %s", Arrays.toString(detectedFaces)));

        if (ArrayUtils.isNotEmpty(detectedFaces)) {
          String[] detectedFaceIds = Arrays.stream(detectedFaces).map(f -> f.faceId().toString()).toArray(String[]::new);
          context.getLogger().info(String.format("Detected face IDs: %s", Arrays.toString(detectedFaceIds)));

          HttpResponse<String> identifyHttpResponse = faceAPIService.faceIdentify(detectedFaceIds);
          IdentifyResult[] identifyResults = new Gson().fromJson(identifyHttpResponse.body(), IdentifyResult[].class);
          context.getLogger().info(String.format("Identification results: %s", Arrays.toString(identifyResults)));
          Arrays.stream(identifyResults)
            .forEach(identifyResult -> processIdentificationResults(identifyResult, blobContent, filename));
        }
        context.getLogger().info("No faces detected from blob");
      }
      context.getLogger()
        .info(String.format("Error while processing face detection from blob: %s", detectFaceHttpResponse.statusCode()));
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

  }

  private void processIdentificationResults(IdentifyResult identifyResult, byte[] blobContent, String filename) {
    if (identifyResult.candidates().isEmpty()) {
      detectionService.auditUnauthorizedDetection(identifyResult, blobContent, filename);
    } else {
      detectionService.auditDetection(identifyResult);
    }
  }

}
