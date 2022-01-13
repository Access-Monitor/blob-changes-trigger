package cloudcomputing.accessmonitor.model.api;

import java.util.Arrays;

public class FaceIdentifyRequestBody {

  private final String[] faceIds;
  private final int maxNumOfCandidatesReturned;
  private final String personGroupId;

  public FaceIdentifyRequestBody(String[] faceIds, int maxNumOfCandidatesReturned, String personGroupId) {
    this.faceIds = faceIds;
    this.maxNumOfCandidatesReturned = maxNumOfCandidatesReturned;
    this.personGroupId = personGroupId;
  }

  @Override
  public String toString() {
    return "FaceIdentifyRequestBody{" + "faceIds=" + Arrays.toString(faceIds) + ", maxNumOfCandidatesReturned=" +
             maxNumOfCandidatesReturned + ", personGroupId='" + personGroupId + '\'' + '}';
  }
}
