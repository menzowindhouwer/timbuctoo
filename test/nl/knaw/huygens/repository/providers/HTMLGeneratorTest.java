package nl.knaw.huygens.repository.providers;

import java.io.IOException;
import java.io.StringWriter;

import nl.knaw.huygens.repository.VariationHelper;
import nl.knaw.huygens.repository.model.Document;
import nl.knaw.huygens.repository.storage.mongo.model.TestSystemDocument;
import nl.knaw.huygens.repository.variation.model.GeneralTestDoc;
import nl.knaw.huygens.repository.variation.model.TestConcreteDoc;
import nl.knaw.huygens.repository.variation.model.projecta.OtherDoc;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class HTMLGeneratorTest {

  private HTMLGenerator gen;
  private ObjectMapper mapper;
  private StringWriter writer;

  @Before
  public void setUp() throws Exception {
    mapper = new ObjectMapper();
    writer = new StringWriter();
    JsonFactory factory = new JsonFactory();
    JsonGenerator realGen = factory.createGenerator(writer);
    gen = new HTMLGenerator(realGen);
  }

  private String generateHtml(Document doc) throws JsonGenerationException, JsonMappingException, IOException {
    mapper.writeValue(gen, doc);
    return writer.getBuffer().toString();
  }

  private void assertContains(String html, String key, String value) {
    Assert.assertThat(html, Matchers.containsString("<tr><th>" + key + "</th><td>" + value + "</td></tr>"));
  }

  @Test
  public void testSystemDocument() throws JsonGenerationException, JsonMappingException, IOException {
    TestSystemDocument doc = new TestSystemDocument();
    doc.setAnnotatedProperty("test");
    doc.setId("TSD0000000001");
    doc.setAnnotatedProperty("anonProp");
    doc.setPropWithAnnotatedAccessors("propWithAnnotatedAccessors");

    String html = generateHtml(doc);

    assertContains(html, "Class", "nl.knaw.huygens.repository.storage.mongo.model.TestSystemDocument");
    assertContains(html, "Name", "none");
    assertContains(html, "Test Value", "none");
    assertContains(html, "Id", "TSD0000000001");
    assertContains(html, "Rev", "0");
    assertContains(html, "Last Change", "none");
    assertContains(html, "Creation", "none");
    assertContains(html, "Pid", "none");
    assertContains(html, "Variations", "");
    assertContains(html, "Prop Annotated", "anonProp");
    assertContains(html, "Pwaa", "propWithAnnotatedAccessors");
    assertContains(html, "Current Variation", "none");
    assertContains(html, "Deleted", "no");
  }

  @Test
  public void testDomainDocumentArchetype() throws JsonGenerationException, JsonMappingException, IOException {
    TestConcreteDoc doc = new TestConcreteDoc();
    doc.setId("TCD0000000001");
    doc.name = "test";
    doc.setVariations(VariationHelper.createVariations("testconcretedoc (projecta)", "testconcretedoc (projectb)"));
    doc.setCurrentVariation("projecta");
    doc.setPid("pid");

    String html = generateHtml(doc);

    assertContains(html, "Class", "nl.knaw.huygens.repository.variation.model.TestConcreteDoc");
    assertContains(html, "Name", "test");
    assertContains(html, "Id", "TCD0000000001");
    assertContains(html, "Rev", "0");
    assertContains(html, "Last Change", "none");
    assertContains(html, "Creation", "none");
    assertContains(html, "Pid", "pid");
    assertContains(html, "Variations", "testconcretedoc (projecta);<br>\ntestconcretedoc (projectb);<br>\n");
    assertContains(html, "Current Variation", "projecta");
    assertContains(html, "Deleted", "no");
  }

  @Test
  public void testDomainDocumentSubtype() throws JsonGenerationException, JsonMappingException, IOException {
    GeneralTestDoc doc = new GeneralTestDoc();
    doc.setId("GTD0000000001");
    doc.generalTestDocValue = "generalTestDocValue";
    doc.name = "test";
    doc.setVariations(VariationHelper.createVariations("generaltestdoc (projecta)", "generaltestdoc (projectb)", "testconcretedoc (projecta)", "testconcretedoc (projectb)"));
    doc.setCurrentVariation("projecta");
    doc.setPid("pid");

    String html = generateHtml(doc);

    assertContains(html, "Class", "nl.knaw.huygens.repository.variation.model.GeneralTestDoc");
    assertContains(html, "Name", "test");
    assertContains(html, "General Test Doc Value", "generalTestDocValue");
    assertContains(html, "Id", "GTD0000000001");
    assertContains(html, "Rev", "0");
    assertContains(html, "Last Change", "none");
    assertContains(html, "Creation", "none");
    assertContains(html, "Pid", "pid");
    assertContains(html, "Variations", "generaltestdoc (projecta);<br>\ngeneraltestdoc (projectb);<br>\ntestconcretedoc (projecta);<br>\ntestconcretedoc (projectb);<br>\n");
    assertContains(html, "Current Variation", "projecta");
    assertContains(html, "Deleted", "no");
  }

  @Test
  public void testDomainDocumentProjectSubtype() throws JsonGenerationException, JsonMappingException, IOException {
    OtherDoc doc = new OtherDoc();
    doc.setId("OTD0000000001");
    doc.otherThing = "test";
    doc.setPid("pid");
    doc.setVariations(VariationHelper.createVariations("projecta-otherdoc", "testinheritsfromtestbasedoc (projecta)"));

    String html = generateHtml(doc);

    assertContains(html, "Class", "nl.knaw.huygens.repository.variation.model.projecta.OtherDoc");
    assertContains(html, "Name", "none");
    assertContains(html, "Other Thing", "test");
    assertContains(html, "Id", "OTD0000000001");
    assertContains(html, "Rev", "0");
    assertContains(html, "Last Change", "none");
    assertContains(html, "Creation", "none");
    assertContains(html, "Pid", "pid");
    assertContains(html, "Variations", "projecta-otherdoc;<br>\ntestinheritsfromtestbasedoc (projecta);<br>\n");
    assertContains(html, "Current Variation", "none");
    assertContains(html, "Deleted", "no");
  }

}
