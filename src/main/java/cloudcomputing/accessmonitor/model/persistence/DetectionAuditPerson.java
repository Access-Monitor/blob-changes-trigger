package cloudcomputing.accessmonitor.model.persistence;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class DetectionAuditPerson {

  private String id;
  private String personId;
  private double confidence;
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  private LocalDateTime detectionTime;
  private long detectionTimestamp;
  private String blobContent;

  public DetectionAuditPerson(String id, String personId, double confidence, LocalDateTime detectionTime, String blobContent) {
    this.id = id;
    this.personId = personId;
    this.confidence = confidence;
    this.detectionTime = detectionTime;
    this.detectionTimestamp = Timestamp.valueOf(detectionTime).getTime();
    this.blobContent = blobContent;
  }

  public DetectionAuditPerson() {
  }

  public long getDetectionTimestamp() {
    return detectionTimestamp;
  }

  public String getBlobContent() {
    return blobContent;
  }

  public void setBlobContent(String blobContent) {
    this.blobContent = blobContent;
  }

  public String getPersonId() {
    return personId;
  }

  public void setPersonId(String personId) {
    this.personId = personId;
  }

  public String getId() {
    return id;
  }

  public double getConfidence() {
    return confidence;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public LocalDateTime getDetectionTime() {
    return detectionTime;
  }

  public void setDetectionTime(LocalDateTime detectionTime) {
    this.detectionTime = detectionTime;
    this.detectionTimestamp = Timestamp.valueOf(detectionTime).getTime();
  }

  @Override
  public String toString() {
    return "DetectionAuditPerson{" + "id='" + id + '\'' + ", personId='" + personId + '\'' + ", confidence=" + confidence +
             ", detectionTime=" + detectionTime + '}';
  }
}
