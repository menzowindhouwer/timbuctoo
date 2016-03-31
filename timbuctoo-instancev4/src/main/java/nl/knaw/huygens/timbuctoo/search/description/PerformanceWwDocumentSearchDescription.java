package nl.knaw.huygens.timbuctoo.search.description;


import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import nl.knaw.huygens.timbuctoo.model.Change;
import nl.knaw.huygens.timbuctoo.model.Datable;
import nl.knaw.huygens.timbuctoo.model.DocumentType;
import nl.knaw.huygens.timbuctoo.model.Gender;
import nl.knaw.huygens.timbuctoo.model.LocationNames;
import nl.knaw.huygens.timbuctoo.model.PersonNames;
import nl.knaw.huygens.timbuctoo.search.SearchDescription;
import nl.knaw.huygens.timbuctoo.search.description.facet.FacetDescriptionFactory;
import nl.knaw.huygens.timbuctoo.search.description.fulltext.FullTextSearchDescription;
import nl.knaw.huygens.timbuctoo.search.description.property.PropertyDescriptorFactory;
import nl.knaw.huygens.timbuctoo.search.description.propertyparser.PropertyParserFactory;
import nl.knaw.huygens.timbuctoo.search.description.sort.SortDescription;
import nl.knaw.huygens.timbuctoo.search.description.sort.SortFieldDescription;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static nl.knaw.huygens.timbuctoo.search.description.Property.derivedProperty;
import static nl.knaw.huygens.timbuctoo.search.description.Property.localProperty;
import static nl.knaw.huygens.timbuctoo.search.description.fulltext.FullTextSearchDescription.createDerivedFullTextSearchDescriptionWithBackupProperty;
import static nl.knaw.huygens.timbuctoo.search.description.fulltext.FullTextSearchDescription.createLocalSimpleFullTextSearchDescription;
import static nl.knaw.huygens.timbuctoo.search.description.sort.SortFieldDescription.newSortFieldDescription;

public class PerformanceWwDocumentSearchDescription extends PerformanceSearchDescription implements SearchDescription {
  private static final List<String> SORTABLE_FIELDS = Lists.newArrayList(
          "dynamic_sort_title",
          "dynamic_k_modified",
          "dynamic_sort_creator");

  private static final List<String> FULL_TEXT_SEARCH_FIELDS = Lists.newArrayList(
          "dynamic_t_author_name",
          "dynamic_t_title",
          "dynamic_t_notes");

  private final PropertyParserFactory propertyParserFactory;
  private final PropertyDescriptorFactory propertyDescriptorFactory;

  private final String type = "wwdocument";
  private final Map<String, PropertyDescriptor> dataDescriptors;
  private final List<FacetDescription> facetDescriptions;
  private final PropertyDescriptor idDescriptor;
  private final PropertyDescriptor displayNameDescriptor;
  private final ArrayList<SortFieldDescription> sortFieldDescriptions;
  private final ArrayList<FullTextSearchDescription> fullTextSearchDescriptions;

  public PerformanceWwDocumentSearchDescription(PropertyDescriptorFactory propertyDescriptorFactory,
                                     FacetDescriptionFactory facetDescriptionFactory) {
    propertyParserFactory = new PropertyParserFactory();
    this.propertyDescriptorFactory = propertyDescriptorFactory;

    dataDescriptors = createDataDescriptors();
    facetDescriptions = createFacetDescriptions(facetDescriptionFactory);

    idDescriptor = propertyDescriptorFactory.getLocal(ID_DB_PROP, String.class);

    displayNameDescriptor = createDisplayNameDescriptor();
    fullTextSearchDescriptions = createFullTextSearchDescriptions();

    sortFieldDescriptions = createSortFieldDescriptions();

  }

  private ArrayList<FullTextSearchDescription> createFullTextSearchDescriptions() {
    return Lists.newArrayList(
            createLocalSimpleFullTextSearchDescription("dynamic_t_notes", "wwdocument_notes"),
            createLocalSimpleFullTextSearchDescription("dynamic_t_title", "wwdocument_title"),
            createDerivedFullTextSearchDescriptionWithBackupProperty(
                    "dynamic_t_author_name", "wwperson_names", "wwperson_tempName", "isCreatedBy")
    );
  }

  protected ArrayList<SortFieldDescription> createSortFieldDescriptions() {
    return Lists.newArrayList(
            newSortFieldDescription()
                    .withName("dynamic_k_modified")
                    .withDefaultValue(0L)
                    .withProperty(localProperty()
                            .withName("modified")
                            .withParser(propertyParserFactory.getParser(Change.class)))
                    .build(),
            newSortFieldDescription()
                    .withName("dynamic_sort_title")
                    .withDefaultValue("")
                    .withProperty(localProperty()
                            .withName("wwdocument_title"))
                    .build(),
            newSortFieldDescription()
                    .withName("dynamic_sort_creator")
                    .withDefaultValue("")
                    .withProperty(derivedProperty("isCreatedBy")
                            .withName("wwperson_names")
                            .withParser(propertyParserFactory.getParser(PersonNames.class)))
                    .withBackupProperty(derivedProperty("isCreatedBy")
                            .withName("wwperson_tempName"))
                    .build()
    );
  }



  private List<FacetDescription> createFacetDescriptions(FacetDescriptionFactory facetDescriptionFactory) {
    return Lists.newArrayList(
            facetDescriptionFactory.createDatableRangeFacetDescription("dynamic_i_date", "wwdocument_date"),
            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_origin", LocationNames.class, "names", "hasPublishLocation"),

            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_language", String.class, "wwlanguage_name", "hasWorkLanguage"),

            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_genre", String.class, "wwkeyword_value", "hasGenre"),

            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_sources", String.class, "wwdocument_title", "hasDocumentSource"),

            facetDescriptionFactory.createDerivedKeywordDescription(
                    "dynamic_s_author_marital_status", "hasMaritalStatus", "ww", "isCreatedBy"),

            facetDescriptionFactory.createDerivedKeywordDescription(
                    "dynamic_s_author_education", "hasEducation", "ww", "isCreatedBy"),

            facetDescriptionFactory.createDerivedKeywordDescription(
                    "dynamic_s_author_social_class", "hasSocialClass", "ww", "isCreatedBy"),

            facetDescriptionFactory.createDerivedKeywordDescription(
                    "dynamic_s_author_religion", "hasReligion", "ww", "isCreatedBy"),

            facetDescriptionFactory.createDerivedKeywordDescription(
                    "dynamic_s_author_financials", "hasFinancialSituation", "ww", "isCreatedBy"),

            facetDescriptionFactory.createDerivedKeywordDescription(
                    "dynamic_s_author_profession", "hasProfession", "ww", "isCreatedBy"),

            facetDescriptionFactory.createDerivedListFacetDescription(
                    "dynamic_s_author_collective", "isMemberOf", String.class, "wwcollective_name", "isCreatedBy"),

            facetDescriptionFactory.createDerivedListFacetDescription(
                    "dynamic_s_author_deathplace", "hasDeathPlace", LocationNames.class, "names", "isCreatedBy"),

            facetDescriptionFactory.createDerivedListFacetDescription(
                    "dynamic_s_author_birthplace", "hasBirthPlace", LocationNames.class, "names", "isCreatedBy"),

            facetDescriptionFactory.createDerivedListFacetDescription(
                    "dynamic_s_author_residence", "hasResidenceLocation", LocationNames.class, "names", "isCreatedBy"),

            facetDescriptionFactory.createDerivedListFacetDescription(
                    "dynamic_s_author_relatedLocations",
                    Lists.newArrayList("hasBirthPlace", "hasDeathPlace", "hasResidenceLocation"),
                    LocationNames.class,
                    "names",
                    "isCreatedBy"),

            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_author_gender", Gender.class, "wwperson_gender", "isCreatedBy"),

            facetDescriptionFactory.createDatableRangeFacetDescription(
                    "dynamic_i_author_deathDate", "wwperson_deathDate", "isCreatedBy"),

            facetDescriptionFactory.createDatableRangeFacetDescription(
                    "dynamic_i_author_birthDate", "wwperson_birthDate", "isCreatedBy"),

            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_author_children", String.class, "wwperson_children", "isCreatedBy"),

            facetDescriptionFactory.createMultiValueListFacetDescription("dynamic_s_author_types", "wwperson_types",
                    "isCreatedBy"),

            facetDescriptionFactory.createChangeRangeFacetDescription("dynamic_i_modified", "modified"),

            facetDescriptionFactory.createListFacetDescription(
                    "dynamic_s_document_type", DocumentType.class, "wwdocument_documentType"));
  }

  private Map<String, PropertyDescriptor> createDataDescriptors() {
    Map<String, PropertyDescriptor> dataDescriptors = Maps.newHashMap();
    dataDescriptors.put("_id", propertyDescriptorFactory.getLocal(ID_DB_PROP, String.class));
    dataDescriptors.put("authorName", createAuthorDescriptor());
    dataDescriptors.put("title", propertyDescriptorFactory.getLocal("wwdocument_title", String.class));
    dataDescriptors.put("date", propertyDescriptorFactory.getLocal("wwdocument_date", Datable.class));
    dataDescriptors.put("authorGender", propertyDescriptorFactory.getDerived(
            "isCreatedBy",
            "wwperson_gender",
            Gender.class));
    dataDescriptors.put("documentType", propertyDescriptorFactory
            .getLocal("wwdocument_documentType", DocumentType.class));
    dataDescriptors.put("modified_date", propertyDescriptorFactory
            .getLocal("modified", propertyParserFactory.getParser(Change.class)));
    dataDescriptors.put("genre", propertyDescriptorFactory
            .getDerived("hasGenre", "wwkeyword_value", String.class));
    dataDescriptors.put("publishLocation", propertyDescriptorFactory.getDerived(
            "hasPublishLocation",
            "names",
            LocationNames.class));
    dataDescriptors.put("language", propertyDescriptorFactory.getDerived(
            "hasWorkLanguage",
            "wwlanguage_name",
            String.class));

    return dataDescriptors;
  }

  @Override
  public List<String> getSortableFields() {
    return SORTABLE_FIELDS;
  }

  @Override
  public List<String> getFullTextSearchFields() {
    return FULL_TEXT_SEARCH_FIELDS;
  }

  private PropertyDescriptor createDisplayNameDescriptor() {
    PropertyDescriptor titleDescriptor = propertyDescriptorFactory.getLocal("wwdocument_title", String.class);
    PropertyDescriptor dateDescriptor = propertyDescriptorFactory.getLocal("wwdocument_date", Datable.class, "(", ")");

    PropertyDescriptor documentDescriptor = propertyDescriptorFactory.getAppender(titleDescriptor, dateDescriptor, " ");

    return propertyDescriptorFactory.getAppender(createAuthorDescriptor(), documentDescriptor, " - ");
  }

  private PropertyDescriptor createAuthorDescriptor() {
    PropertyDescriptor authorNameDescriptor = propertyDescriptorFactory.getDerivedWithSeparator(
            "isCreatedBy",
            "wwperson_names",
            propertyParserFactory.getParser(PersonNames.class),
            "; ");
    PropertyDescriptor authorTempNameDescriptor = propertyDescriptorFactory.getDerivedWithSeparator(
            "isCreatedBy",
            "wwperson_tempName",
            propertyParserFactory.getParser(String.class),
            "; ");

    return propertyDescriptorFactory
            .getComposite(authorNameDescriptor, authorTempNameDescriptor);
  }

  @Override
  protected List<FacetDescription> getFacetDescriptions() {
    return facetDescriptions;
  }

  @Override
  protected Map<String, PropertyDescriptor> getDataPropertyDescriptors() {
    return dataDescriptors;
  }

  @Override
  protected PropertyDescriptor getDisplayNameDescriptor() {
    return displayNameDescriptor;
  }

  @Override
  protected PropertyDescriptor getIdDescriptor() {
    return idDescriptor;
  }

  @Override
  public String getType() {
    return type;
  }


  @Override
  public List<FullTextSearchDescription> getFullTextSearchDescriptions() {
    return fullTextSearchDescriptions;
  }

  @Override
  protected SortDescription getSortDescription() {

    return new SortDescription(sortFieldDescriptions);
  }
}