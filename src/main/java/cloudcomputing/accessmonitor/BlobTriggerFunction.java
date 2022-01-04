package cloudcomputing.accessmonitor;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

/**
 * Azure Functions with Azure Blob trigger.
 */
public class BlobTriggerFunction {
    /**
     * This function will be invoked when a new or updated blob is detected at the specified path. The blob contents are provided as input to this function.
     */
    @FunctionName("BlobTrigger")
    @StorageAccount("AzureStorageAccountConnectionConfig")
    public void run(
            @BlobTrigger(name = "content", path = "accessmonitorblob/{filename}", dataType = "binary") byte[] content,
            @BindingName("filename") String filename,
            final ExecutionContext context
    ) {
        context.getLogger().info("Java Blob trigger function processed a blob. Name: " + filename + "\n  Size: " + content.length + " Bytes");
    }
}
