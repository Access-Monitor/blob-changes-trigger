package cloudcomputing.accessmonitor.service;

import com.azure.core.http.rest.Response;

public interface BlobStorageService {

  Response<Void> deleteBlob(String blobName, String containerName);
}
