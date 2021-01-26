package name.mrkandreev.camel;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.azure.storage.common.StorageSharedKeyCredential;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication()
public class MyApplication {
  public static void main(String[] args) {
    SpringApplication.run(MyApplication.class, args);
  }
}

class AzureBlobConfigHelper {
  private AzureBlobConfigHelper() {
  }

  public static final String ACCOUNT = "ENTER_YOUR_ACCOUNT";

  public static final String BLOB_CONTAINER_NAME = "CONTAINER";

  public static final String ACCESS_KEY = "ACCESS_KEY";
}

@Component
@Log4j2
class MyRoute extends RouteBuilder {

  @Override
  public void configure() {
    from("direct:list")
        .routeId("listBlobs")
        .process(exchange -> {
          BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
              .endpoint(String
                  .format("https://%s.blob.core.windows.net", AzureBlobConfigHelper.ACCOUNT))
              .credential(new StorageSharedKeyCredential(AzureBlobConfigHelper.ACCOUNT,
                  AzureBlobConfigHelper.ACCESS_KEY))
              .buildClient();
          BlobContainerClient containerClient =
              blobServiceClient.getBlobContainerClient(AzureBlobConfigHelper.BLOB_CONTAINER_NAME);
          ListBlobsOptions blobsOptions = new ListBlobsOptions().setMaxResultsPerPage(5);
          Duration duration = Duration.of(10, ChronoUnit.SECONDS);

          Iterator<BlobItem>
              iterator = containerClient.listBlobs(blobsOptions, duration).iterator();
          exchange.getIn().setBody(iterator);
        })
        .loopDoWhile(exchange -> {
          Iterator<BlobItem> iterator =
              (Iterator<BlobItem>) exchange.getIn().getBody(Iterator.class);
          return iterator.hasNext();
        })
        .process(exchange -> {
          Iterator<BlobItem> iterator =
              (Iterator<BlobItem>) exchange.getIn().getBody(Iterator.class);
          exchange.getIn().setBody(iterator.next());
        })
        .process(exchange -> {
          BlobItem item = exchange.getIn().getBody(BlobItem.class);
          log.info(item);
        })
        .end();

    from("timer://runOnce?repeatCount=1&delay=0")
        .to("direct:list");
  }
}
