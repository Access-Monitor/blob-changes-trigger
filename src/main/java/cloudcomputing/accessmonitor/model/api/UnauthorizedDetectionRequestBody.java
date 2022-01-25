package cloudcomputing.accessmonitor.model.api;

public class UnauthorizedDetectionRequestBody {

  private String id;
  private String faceId;
  private byte[] blobContent;

  public UnauthorizedDetectionRequestBody(String id, String faceId, byte[] blobContent) {
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

  public byte[] getBlobContent() {
    return blobContent;
  }

  public void setBlobContent(byte[] blobContent) {
    this.blobContent = blobContent;
  }
}
