package cloudcomputing.accessmonitor;

import static cloudcomputing.accessmonitor.constants.DatabaseConstants.AUDIT_CONTAINER_NAME;
import static cloudcomputing.accessmonitor.constants.DatabaseConstants.COSMOSDB_ENDPOINT;
import static cloudcomputing.accessmonitor.constants.DatabaseConstants.COSMOSDB_SUBSCRIPTION_KEY;
import static cloudcomputing.accessmonitor.constants.DatabaseConstants.DATABASE_NAME;
import static cloudcomputing.accessmonitor.constants.HttpConstants.SUCCESS;

import cloudcomputing.accessmonitor.model.persistence.DetectionAuditItem;
import cloudcomputing.accessmonitor.service.FaceAPIService;
import cloudcomputing.accessmonitor.service.impl.FaceAPIServiceImpl;
import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosContainerProperties;
import com.azure.cosmos.models.CosmosContainerResponse;
import com.azure.cosmos.models.CosmosDatabaseResponse;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import org.apache.commons.lang3.ArrayUtils;

public class BlobTriggerFunction {

  private final FaceAPIService faceAPIService = new FaceAPIServiceImpl();

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
        String[] detectedFaceIds = Arrays.stream(detectedFaces).map(f -> f.faceId().toString()).toArray(String[]::new);
        HttpResponse<String> identifyHttpResponse = faceAPIService.faceIdentify(detectedFaceIds);
        IdentifyResult[] identifyResults = new Gson().fromJson(identifyHttpResponse.body(), IdentifyResult[].class);
        if (ArrayUtils.isNotEmpty(identifyResults)) {
          // someone recognized
          Arrays.stream(identifyResults).forEach(this::auditDetection); //TODO tofix: db read not working
        } else {
          // not authorized
        }
      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

  }

  private void auditDetection(IdentifyResult identifyResult) {
    CosmosClient client = new CosmosClientBuilder().endpoint(COSMOSDB_ENDPOINT)
                            .key(COSMOSDB_SUBSCRIPTION_KEY)
                            .consistencyLevel(ConsistencyLevel.EVENTUAL)
                            .buildClient();
    CosmosDatabaseResponse databaseResponse = client.createDatabaseIfNotExists(DATABASE_NAME);
    CosmosDatabase database = client.getDatabase(databaseResponse.getProperties().getId());
    CosmosContainerProperties containerProperties = new CosmosContainerProperties(AUDIT_CONTAINER_NAME, "/id");
    CosmosContainerResponse containerResponse = database.createContainerIfNotExists(containerProperties);
    CosmosContainer container = database.getContainer(containerResponse.getProperties().getId());

    identifyResult.candidates().forEach(candidate -> {
      DetectionAuditItem detectionAuditItem =
        new DetectionAuditItem(candidate.personId().toString(), candidate.confidence(), LocalDateTime.now());

      CosmosItemResponse<DetectionAuditItem> persistedDetectedItem =
        container.readItem(detectionAuditItem.getId(), new PartitionKey(detectionAuditItem.getId()), DetectionAuditItem.class);
      LocalDateTime lastDetectionTime = persistedDetectedItem.getItem().getDetectionTime();
      long minutes = Duration.between(lastDetectionTime, detectionAuditItem.getDetectionTime()).toMinutes();

      container.createItem(detectionAuditItem, new PartitionKey(detectionAuditItem.getId()), new CosmosItemRequestOptions());
    });

  }
}
