package cloudcomputing.accessmonitor;

import com.google.gson.Gson;
import com.microsoft.azure.cognitiveservices.vision.faceapi.models.DetectedFace;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.BlobTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class BlobTriggerFunction {

  public static final int SUCCESS = 200;
  public static final String APPLICATION_OCTET_STREAM = "application/octet-stream";
  public static final String FACE_API_ENDPOINT = "FaceAPIEndpoint";
  public static final String FACE_API_SUBSCRIPTION_KEY = "FaceAPISubscriptionKey";

  @FunctionName("BlobTrigger")
  @StorageAccount("AzureWebJobsStorage")
  public void run(@BlobTrigger(name = "blobContent", path = "accessmonitorblob/{filename}", dataType = "binary") byte[] blobContent,
    @BindingName("filename") String filename, final ExecutionContext context) {

    context.getLogger()
      .info("Java Blob trigger function processed a blob. Name: " + filename + "\n  Size: " + blobContent.length + " Bytes");

    try {
      HttpResponse<String> httpResponse = faceDetect(blobContent);

      if (httpResponse.statusCode() == SUCCESS) {
        DetectedFace[] detectedFaces = new Gson().fromJson(httpResponse.body(), DetectedFace[].class);

      }
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }

  }

  private HttpResponse<String> faceDetect(byte[] blobContent) throws IOException, InterruptedException {
    HttpClient httpClient = HttpClient.newHttpClient();
    HttpRequest httpRequest = buildRequest(blobContent);
    return httpClient.send(httpRequest, BodyHandlers.ofString());
  }

  private HttpRequest buildRequest(byte[] blobContent) {
    return HttpRequest.newBuilder(URI.create(System.getenv(FACE_API_ENDPOINT)))
             .header("Content-type", APPLICATION_OCTET_STREAM)
             .header("Ocp-Apim-Subscription-Key", System.getenv(FACE_API_SUBSCRIPTION_KEY))
             .POST(BodyPublishers.ofByteArray(blobContent))
             .build();
  }
}
