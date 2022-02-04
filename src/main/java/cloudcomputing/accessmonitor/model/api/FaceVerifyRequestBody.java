package cloudcomputing.accessmonitor.model.api;

public class FaceVerifyRequestBody {

  private String faceId1;
  private String faceId2;

  public FaceVerifyRequestBody(String faceId1, String faceId2) {
    this.faceId1 = faceId1;
    this.faceId2 = faceId2;
  }

  public FaceVerifyRequestBody() {
  }

  public String getFaceId1() {
    return faceId1;
  }

  public void setFaceId1(String faceId1) {
    this.faceId1 = faceId1;
  }

  public String getFaceId2() {
    return faceId2;
  }

  public void setFaceId2(String faceId2) {
    this.faceId2 = faceId2;
  }
}
