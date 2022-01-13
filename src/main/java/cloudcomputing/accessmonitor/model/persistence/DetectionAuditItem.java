package cloudcomputing.accessmonitor.model.persistence;

import java.time.LocalDateTime;

public class DetectionAuditItem {

  private String id;
  private double confidence;
  private LocalDateTime detectionTime;

  public DetectionAuditItem(String id, double confidence, LocalDateTime detectionTime) {
    this.id = id;
    this.confidence = confidence;
    this.detectionTime = detectionTime;
  }

  public DetectionAuditItem() {
  }

  public String getId() {
    return id;
  }

  public double getConfidence() {
    return confidence;
  }

  public LocalDateTime getDetectionTime() {
    return detectionTime;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setConfidence(double confidence) {
    this.confidence = confidence;
  }

  public void setDetectionTime(LocalDateTime detectionTime) {
    this.detectionTime = detectionTime;
  }

  @Override
  public String toString() {
    return "DetectionAuditItem{" + "id='" + id + '\'' + ", confidence=" + confidence + ", detectionTime=" + detectionTime + '}';
  }
}
