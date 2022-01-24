package cloudcomputing.accessmonitor.constants;

public class FaceAPIConstants {

  public static final String MODE_MATCH_FACE = "matchFace";
  public static final String FACE_API_BASE_ENDPOINT = System.getenv("FaceAPIEndpoint");
  public static final String FACE_API_SUBSCRIPTION_KEY = System.getenv("FaceAPISubscriptionKey");

  public static final String TEST_PERSON_GROUP = "mainpersongroup";
  public static final String FACE_ID_HEADER = "faceId";
  public static final String FILENAME_HEADER = "filename";

}
