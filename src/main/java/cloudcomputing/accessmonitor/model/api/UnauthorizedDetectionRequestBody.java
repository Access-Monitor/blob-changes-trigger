package cloudcomputing.accessmonitor.model.api;

public class UnauthorizedDetectionRequestBody {

  private String id;
  private String faceId;
  private String blobContent;

  public UnauthorizedDetectionRequestBody(String id, String faceId, String blobContent) {
    this.faceId = faceId;
    this.blobContent = blobContent;
    this.id = id;
  }

  public UnauthorizedDetectionRequestBody() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFaceId() {
    return faceId;
  }

  public void setFaceId(String faceId) {
    this.faceId = faceId;
  }

  public String getBlobContent() {
    return blobContent;
  }

  public void setBlobContent(String blobContent) {
    this.blobContent = blobContent;
  }
}
