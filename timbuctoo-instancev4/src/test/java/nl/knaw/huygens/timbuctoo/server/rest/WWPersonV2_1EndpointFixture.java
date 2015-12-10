package nl.knaw.huygens.timbuctoo.server.rest;

import nl.knaw.huygens.concordion.extensions.HttpCommandExtension;
import nl.knaw.huygens.concordion.extensions.HttpRequest;
import org.apache.commons.lang3.StringUtils;
import org.concordion.api.extension.Extension;
import org.concordion.integration.junit4.ConcordionRunner;
import org.junit.runner.RunWith;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@RunWith(ConcordionRunner.class)
public class WWPersonV2_1EndpointFixture {
  @Extension
  public HttpCommandExtension commandExtension = new HttpCommandExtension(this::doHttpCommand);

//  @Rule
//  public final ResourceTestRule resources = ResourceTestRule.builder().addResource(new WWPersonCollectionV2_1EndPoint()).build();

  private Response doHttpCommand(HttpRequest httpRequest) {
    WebTarget target = ClientBuilder.newClient().target("http://acc.repository.huygens.knaw.nl");
    return target.path(httpRequest.url).request() //
      .headers(httpRequest.headers)
      .method(httpRequest.method);
  }

  public String isEmpty(String value) {
    return StringUtils.isBlank(value) ? "empty" : "not empty";
  }
}
