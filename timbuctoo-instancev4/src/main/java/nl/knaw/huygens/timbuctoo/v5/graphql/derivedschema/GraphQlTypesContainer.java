package nl.knaw.huygens.timbuctoo.v5.graphql.derivedschema;

import graphql.Scalars;
import graphql.TypeResolutionEnvironment;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLOutputType;
import graphql.schema.GraphQLType;
import graphql.schema.GraphQLTypeReference;
import graphql.schema.GraphQLUnionType;
import graphql.schema.TypeResolver;
import nl.knaw.huygens.timbuctoo.v5.datastores.quadstore.dto.Direction;
import nl.knaw.huygens.timbuctoo.v5.datastores.prefixstore.TypeNameStore;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.DataFetcherFactory;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.DataFetcherWrapper;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.PaginationArgumentsHelper;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.RelatedDataFetcher;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.UriFetcher;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.SubjectReference;
import nl.knaw.huygens.timbuctoo.v5.graphql.datafetchers.dto.TypedValue;
import nl.knaw.huygens.timbuctoo.v5.util.RdfConstants;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static graphql.schema.GraphQLFieldDefinition.newFieldDefinition;
import static graphql.schema.GraphQLInterfaceType.newInterface;
import static graphql.schema.GraphQLNonNull.nonNull;
import static graphql.schema.GraphQLObjectType.newObject;
import static graphql.schema.GraphQLUnionType.newUnionType;
import static org.slf4j.LoggerFactory.getLogger;

public class GraphQlTypesContainer {

  public static final String ENTITY_INTERFACE_NAME = "Entity";
  public static final String VALUE_INTERFACE_NAME = "Value";
  private static final Logger LOG = getLogger(GraphQlTypesContainer.class);

  final Map<String, GraphQLObjectType> typeForUri;
  final Map<String, GraphQLObjectType> wrappedValueTypes;
  final Set<GraphQLType> allTypes;

  final GraphQLInterfaceType entityInterface;
  final GraphQLInterfaceType valueInterface;

  final ObjectTypeResolver objectResolver;
  final ValueTypeResolver valueTypeResolver;
  final TypeResolver unionTypeResolver;

  private final TypeNameStore typeNameStore;
  private final DataFetcherFactory dataFetcherFactory;
  private final PaginationArgumentsHelper argumentsHelper;

  public GraphQlTypesContainer(TypeNameStore typeNameStore, DataFetcherFactory dataFetcherFactory,
                               PaginationArgumentsHelper argumentsHelper) {
    this.typeNameStore = typeNameStore;
    this.dataFetcherFactory = dataFetcherFactory;
    this.argumentsHelper = argumentsHelper;
    allTypes = new HashSet<>();
    typeForUri = new HashMap<>();
    objectResolver = new ObjectTypeResolver(this.typeForUri, this.typeNameStore);
    wrappedValueTypes = new HashMap<>();
    valueTypeResolver = new ValueTypeResolver(wrappedValueTypes);
    unionTypeResolver = new UnionTypeResolver(valueTypeResolver, objectResolver);
    entityInterface = newInterface()
      .name(ENTITY_INTERFACE_NAME)
      .field(newFieldDefinition()
        .name("uri")
        .type(Scalars.GraphQLID)
      )
      .typeResolver(objectResolver)
      .build();
    valueInterface = newInterface()
      .name(VALUE_INTERFACE_NAME)
      .field(newFieldDefinition()
        .name("value")
        .type(nonNull(Scalars.GraphQLString))
      )
      .field(newFieldDefinition()
        .name("type")
        .type(nonNull(Scalars.GraphQLString))
      )
      .typeResolver(valueTypeResolver)
      .build();
  }

  public GraphQLFieldDefinition objectField(String fieldName, String description, String predicateUri,
                                            Direction predicateDirection, String typeName, boolean isList,
                                            boolean isOptional) {
    GraphQLTypeReference type = new GraphQLTypeReference(typeName);
    RelatedDataFetcher dataFetcher = this.dataFetcherFactory.relationFetcher(predicateUri, predicateDirection);
    return makeField(fieldName, description, type, dataFetcher, isList, isOptional);
  }

  public GraphQLFieldDefinition unionField(String name, String description, List<GraphQLTypeReference> refs,
                                           List<GraphQLObjectType> types,
                                           String predicateUri, Direction direction, boolean isOptional,
                                           boolean isList) {
    String unionName = "Union_";
    for (GraphQLObjectType type : types) {
      unionName += type.getName() + "_";
    }
    for (GraphQLTypeReference type : refs) {
      unionName += type.getName() + "_";
    }
    unionName += UUID.randomUUID().toString().replaceAll("[^a-zA-Z0-9]", "");
    GraphQLUnionType.Builder unionType = newUnionType()
      .name(unionName)
      .typeResolver(this.unionTypeResolver);
    for (GraphQLObjectType type : types) {
      unionType.possibleType(type);
    }
    for (GraphQLTypeReference type : refs) {
      unionType.possibleType(type);
    }
    RelatedDataFetcher dataFetcher = this.dataFetcherFactory.unionFetcher(predicateUri, direction);
    GraphQLUnionType type = unionType.build();
    return makeField(name, description, type, dataFetcher, isList, isOptional);
  }

  public GraphQLFieldDefinition valueField(String name, String description, String typeUri, boolean isList,
                                           boolean isOptional,
                                           String predicateUri) {
    GraphQLObjectType type = valueType(typeUri);
    RelatedDataFetcher dataFetcher = this.dataFetcherFactory.typedLiteralFetcher(predicateUri);
    return makeField(name, description, type, dataFetcher, isList, isOptional);
  }

  public GraphQLFieldDefinition valueField(String name, String description, boolean isList, boolean isOptional,
                                           String predicateUri) {
    RelatedDataFetcher dataFetcher = this.dataFetcherFactory.typedLiteralFetcher(predicateUri);
    return makeField(name, description, valueInterface, dataFetcher, isList, isOptional);
  }

  private GraphQLFieldDefinition makeField(String name, String description, GraphQLOutputType type,
                                           RelatedDataFetcher dataFetcher,
                                           boolean list, boolean optional) {
    GraphQLFieldDefinition.Builder result = newFieldDefinition()
      .name(name)
      .dataFetcher(new DataFetcherWrapper(list, dataFetcher));

    if (description != null) {
      result.description(description);
    }
    if (list) {
      argumentsHelper.makePaginatedList(result, type);
    } else if (!optional) {
      result.type(nonNull(type));
    } else {
      result.type(type);
    }

    return result.build();
  }

  public GraphQLObjectType valueType(String typeUri) {
    return wrappedValueTypes.computeIfAbsent(typeUri, uri -> {
      final String name = typeNameStore.makeGraphQlValuename(uri);
      GraphQLObjectType valueType = newObject()
        .name(name)
        .withInterface(this.valueInterface)
        .field(newFieldDefinition()
          .name("value")
          .type(nonNull(Scalars.GraphQLString))
        )
        .field(newFieldDefinition()
          .name("type")
          .type(nonNull(Scalars.GraphQLString))
        )
        .build();
      wrappedValueTypes.put(uri, valueType);
      allTypes.add(valueType);
      return valueType;
    });
  }

  public GraphQLObjectType objectType(String objectName, String description, Optional<String> typeUri,
                                      List<GraphQLFieldDefinition> fieldDefinitions) {
    GraphQLObjectType objectType = newObject()
      .name(objectName)
      .withInterface(entityInterface)
      .field(newFieldDefinition()
        .name("uri")
        .type(Scalars.GraphQLID)
        .dataFetcher(new UriFetcher())
      )
      .fields(fieldDefinitions)
      .description(description)
      .build();
    if (typeUri.isPresent()) {
      typeForUri.put(typeUri.get(), objectType);
    }
    allTypes.add(objectType);
    return objectType;
  }

  public Map<String, GraphQLObjectType> getRdfTypeRepresentingTypes() {
    return typeForUri;
  }

  public Set<GraphQLType> getAllObjectTypes() {
    return allTypes;
  }

  private class ObjectTypeResolver implements TypeResolver {

    protected final TypeNameStore typeNameStore;
    protected final Map<String, GraphQLObjectType> typeForUri;

    private ObjectTypeResolver(Map<String, GraphQLObjectType> typeForUri, TypeNameStore typeNameStore) {
      this.typeForUri = typeForUri;
      this.typeNameStore = typeNameStore;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
      //Often a thing has one type. In that case this lambda is easy to implement. Simply return that type
      //In rdf things can have more then one type though (types are like java interfaces)
      //Since this lambda only allows us to return 1 type we need to do a bit more work and return one of the types that
      //the user actually requested
      Set<String> typeUris = ((SubjectReference) environment.getObject()).getTypes();
      for (Selection selection : environment.getField().getSelectionSet().getSelections()) {
        if (selection instanceof InlineFragment) {
          InlineFragment fragment = (InlineFragment) selection;
          String typeUri = typeNameStore.makeUri(fragment.getTypeCondition().getName());
          if (typeUris.contains(typeUri)) {
            return typeForUri.get(typeUri);
          }
        } else {
          LOG.error("I have a union type whose selection is not an InlineFragment!");
        }
      }
      return typeUris.isEmpty() ? typeForUri.get(RdfConstants.UNKNOWN) : typeForUri.get(typeUris.iterator().next());
    }
  }

  private class UnionTypeResolver implements TypeResolver {
    private final ValueTypeResolver valueTypeResolver;
    private final ObjectTypeResolver objectTypeResolver;

    private UnionTypeResolver(ValueTypeResolver valueTypeResolver, ObjectTypeResolver objectTypeResolver) {
      this.valueTypeResolver = valueTypeResolver;
      this.objectTypeResolver = objectTypeResolver;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
      if (environment.getObject() instanceof TypedValue) {
        return valueTypeResolver.getType(environment);
      } else {
        return objectTypeResolver.getType(environment);
      }
    }
  }

  private class ValueTypeResolver implements TypeResolver {

    protected Map<String, GraphQLObjectType> wrappedValueTypes;

    private ValueTypeResolver(Map<String, GraphQLObjectType> wrappedValueTypes) {
      this.wrappedValueTypes = wrappedValueTypes;
    }

    @Override
    public GraphQLObjectType getType(TypeResolutionEnvironment environment) {
      return wrappedValueTypes.get(((TypedValue) environment.getObject()).getType());
    }
  }
}