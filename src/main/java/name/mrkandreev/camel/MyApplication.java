package name.mrkandreev.camel;

import com.azure.storage.blob.models.ListBlobsOptions;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.azure.storage.blob.BlobConstants;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

@SpringBootApplication()
public class MyApplication {
  public static void main(String[] args) {
    SpringApplication.run(MyApplication.class, args);
  }
}

@Component
@Log4j2
class MyRoute extends RouteBuilder {
  private static final String ACCOUNT = "ACCOUNT_NAME";

  private static final String BLOB_CONTAINER_NAME = "BLOB_CONTAINER_NAME";

  private static final String ACCESS_KEY = "ACCESS_KEY";

  @Override
  public void configure() throws Exception {
    from("direct:list")
        .routeId("listBlobs")
        .setHeader(BlobConstants.LIST_BLOB_OPTIONS)
        .constant(new ListBlobsOptions().setMaxResultsPerPage(5))
        .to(
            String.format(
                "azure-storage-blob://%s/%s?accessKey=RAW(%s)&operation=listBlobs&synchronous=true",
                ACCOUNT,
                BLOB_CONTAINER_NAME,
                ACCESS_KEY
            )
        )
        .process(exchange -> {
          int itemsCount = exchange.getIn().getBody(List.class).size();
          log.info(String.format("Items count = '%d'", itemsCount));
        });

    from("timer://runOnce?repeatCount=1&delay=0")
        .to("direct:list");
  }
}
