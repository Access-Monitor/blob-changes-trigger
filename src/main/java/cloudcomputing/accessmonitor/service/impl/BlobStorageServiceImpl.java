package cloudcomputing.accessmonitor.service.impl;

import static cloudcomputing.accessmonitor.constants.StorageConstants.BLOB_STORAGE_CONNECTION_STRING;

import cloudcomputing.accessmonitor.service.BlobStorageService;
import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.DeleteSnapshotsOptionType;

public class BlobStorageServiceImpl implements BlobStorageService {

  @Override
  public Response<Void> deleteBlob(String blobName, String containerName) {
    try {
      BlobContainerClient accessMonitorBlobClient = new BlobServiceClientBuilder().connectionString(BLOB_STORAGE_CONNECTION_STRING)
        .buildClient()
        .getBlobContainerClient(containerName);
      BlobClient blobClient = accessMonitorBlobClient.getBlobClient(blobName);
      return blobClient.deleteWithResponse(DeleteSnapshotsOptionType.INCLUDE, null, null, null);
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

}
