package cloudcomputing.accessmonitor.model.persistence;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class UnauthorizedDetection {

  private String id;
  private String faceId;
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  private LocalDateTime detectionTime;
  private long detectionTimestamp;
  private String filename;

  public UnauthorizedDetection(String id, String faceId, LocalDateTime detectionTime, String filename) {
    this.id = id;
    this.faceId = faceId;
    this.detectionTime = detectionTime;
    this.detectionTimestamp = Timestamp.valueOf(detectionTime).getTime();
    this.filename = filename;
  }

  public UnauthorizedDetection() {
  }

  public long getDetectionTimestamp() {
    return detectionTimestamp;
  }

  public String getFilename() {
    return filename;
  }

  public void setFilename(String filename) {
    this.filename = filename;
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

  public LocalDateTime getDetectionTime() {
    return detectionTime;
  }

  public void setDetectionTime(LocalDateTime detectionTime) {
    this.detectionTime = detectionTime;
    this.detectionTimestamp = Timestamp.valueOf(detectionTime).getTime();
  }
}
