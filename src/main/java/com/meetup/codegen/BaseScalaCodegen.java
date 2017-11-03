package com.meetup.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

/**
 * A base code generator for scala source. Currently this plays a few roles:
 * 1) provide some default settings like source folder, package, etc
 * 2) provide some general scala constraints including reserved words and primitives
 * 3) opinionated handling of how a spec should be interpreted as scala
 *    (enums, optional properties, etc) as well as support for JSON codec generation
 */
abstract class BaseScalaCodegen extends DefaultCodegen implements CodegenConfig {

    private static final String ARG_INCLUDE_SERIALIZATION = "includeSerialization";
    protected String sourceFolder = "src/main/scala";
    protected String testSourceFolder = "src/main/test";
    protected String invokerPackage = "com.meetup";
    protected String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

    private static final Set<String> NUMBER_TYPES =
            new HashSet<>(Arrays.asList("Int", "Long", "Float", "Double"));

    @Override
    /* Lifted from https://github.com/swagger-api/swagger-codegen/blob/master/modules/swagger-codegen/src/main/java/io/swagger/codegen/languages/AbstractScalaCodegen.java#L182-L185
       to prevent injection. */
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    public static Set<String> getNumberTypes() {
        Set<String> copy = new HashSet<>();
        copy.addAll(NUMBER_TYPES);
        return copy;
    }

    BaseScalaCodegen() {

        outputFolder = "generated-code/" + getName();

        cliOptions.add(CliOption.newBoolean(ARG_INCLUDE_SERIALIZATION, "To include or not include serializers in the model classes"));

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "meetup-scala";

        /*
         * Api Package. Optional, if needed, this can be used in templates
         */
        apiPackage = "com.meetup.foo.api";

        /*
         * Model Package.  Optional, if needed, this can be used in templates
         */
        modelPackage = "io.swagger.client.model";

        reservedWords = new HashSet<>(
                Arrays.asList(
                        "abstract", "case", "catch", "class", "def", "do", "else", "extends",
                        "false", "final", "finally", "for", "forSome", "if", "implicit",
                        "import", "lazy", "match", "new", "null", "object", "override",
                        "package", "private", "protected", "return", "sealed", "super",
                        "this", "throw", "trait", "try", "true", "type", "val", "var",
                        "while", "with", "yield"));

        languageSpecificPrimitives = new HashSet<>(
                Arrays.asList(
                        "Boolean",
                        "Double",
                        "Float",
                        "Int",
                        "Long",
                        "List",
                        "Map",
                        "String")
        );

        instantiationTypes.put("date-time", "ZonedDateTime");
        instantiationTypes.put("array", "List");
        instantiationTypes.put("integer", "Int");

        importMapping.put("ZonedDateTime", "java.time.ZonedDateTime");

        typeMapping.put("date", "LocalDate");
        importMapping.put("LocalDate", "java.time.LocalDate");

        typeMapping.put("DateTime", "ZonedDateTime");

        typeMapping.put("int", "Int");
        typeMapping.put("Integer", "Int");

        typeMapping.put("timestamp", "Instant");
        importMapping.put("Instant", "java.time.Instant");

        typeMapping.put("local-time", "LocalTime");
        importMapping.put("LocalTime", "java.time.LocalTime");

        typeMapping.put("local-date-time", "LocalDateTime");
        importMapping.put("LocalDateTime", "java.time.LocalDateTime");
    }

    @Override
    public void processOpts() {
        Object givenInvPkg = additionalProperties.get(CodegenConstants.INVOKER_PACKAGE);
        invokerPackage = givenInvPkg == null ? invokerPackage : givenInvPkg.toString();

        super.processOpts();
        additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);

        modelPackage = invokerPackage + ".model";
        apiPackage = invokerPackage + ".api";

        invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

        Object incSer = additionalProperties.get(ARG_INCLUDE_SERIALIZATION);
        final boolean includeSerialization = incSer == null || Boolean.TRUE.toString().equals(incSer);
        additionalProperties.put(ARG_INCLUDE_SERIALIZATION, includeSerialization);

        if (includeSerialization) {
            supportingFiles.add(new SupportingFile("Codec.mustache", invokerFolder, "Codec.scala"));
            supportingFiles.add(new SupportingFile("Serializer.mustache", invokerFolder, "Serializer.scala"));
            supportingFiles.add(new SupportingFile("parserJson4s.mustache", invokerFolder, "Parser.scala"));
            supportingFiles.add(new SupportingFile("codecInstancesJson4s.mustache", invokerFolder, "CodecInstances.scala"));

            additionalProperties.put("json4s", "true");
            additionalProperties.put("jsonTypePackage", "org.json4s");
            additionalProperties.put("jsonType", "JValue");
        }
    }

    @Override
    public final Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // import sanitization lifted from ScalaClientCodegen
        // explicitly removes imports of classes in the same package
        // including these imports causes warnings
        @SuppressWarnings("unchecked")
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        final String prefix = modelPackage() + ".";
        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(prefix)) iterator.remove();
        }

        // special handling for the model's timestamps
        objs = postProcessModelsTimestamp(objs);

        // special handling for the model's $ref entries
        objs = postProcessModelsRefs(objs);

        // Now subject the models to Enum treatment.
        return this.postProcessModelsEnum(objs);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> postProcessModelsEnum(Map<String, Object> objs) {
        // first do the default enum handling that comes in DefaultCodegen
        // this is a prereq to the custom handling we do next
        objs = super.postProcessModelsEnum(objs);

        // our own custom handling
        // we get the first Enum's name and put it in an accessible place, so that it may be
        // easily referenced in Mustache templates
        List<Map> models = (List<Map>) objs.get("models");
        for (Map map : models ) {
            if (map.containsKey("model")) {
                CodegenModel cm = (CodegenModel) map.get("model");
                for (CodegenProperty prop : cm.vars) {
                    if (prop.isEnum) {
                        List<Map<String,String>> enumVars = (List<Map<String,String>>) prop.allowableValues.get("enumVars");
                        Map<String,String> firstValMap = enumVars.isEmpty() ? new HashMap<>() : enumVars.get(0);
                        String firstValName = firstValMap.containsKey("name") ? firstValMap.get("name") : "";
                        prop.vendorExtensions.put("firstEnumName", firstValName);
                    }
                }
            }
        }
        return objs;
    }

    /**
     * OpenAPI yaml specs support generating objects that refer to other objects, like so:
     *
     * aReferenceToAnotherDefinition:
     *   $ref: '#/definitions/SomeOtherDefinition'
     *
     * We want to mark the objects as such, so that we can handle them separately when
     * auto-generating unit tests.
     *
     * @param objs Map of models
     * @return map of models with "isRef" field set to true for $ref properties
     */
    public Map<String, Object> postProcessModelsRefs(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        List<Map> models = (List<Map>) objs.get("models");
        for (Map map : models ) {
            if (map.containsKey("model")) {
                CodegenModel cm = (CodegenModel) map.get("model");
                for (CodegenProperty prop : cm.vars) {
                    // ignore containers. they may contain refs but are not themselves refs.
                    if (prop.isNotContainer != null && prop.isNotContainer &&
                            prop.jsonSchema != null && prop.jsonSchema.contains("$ref")) {
                        prop.vendorExtensions.put("isRef", true);
                    }
                }
            }
        }
        return objs;
    }

    /**
     * Our OpenAPI yaml specs support generating java.time.Instant properties like so:
     *
     * aTimestampProperty:
     *   type: string
     *   format: timestamp
     *
     * The codegen will interpret this property as a String and, in the model, it will set
     * a boolean, isString, to true.
     *
     * We want the isString property to be false/null, and instead have an isTimestamp property be true.
     * This is similar to how Dates and DateTimes are handled, except those are done for us by default.
     *
     * Unfortunately we cannot add an "isTimestamp" property in the model at the same level as the other
     * booleans.  Instead, we have to put it in the "vendorExceptions" map.  So to check for timestamps,
     * you'd need to check for "vendorExtensions.isTimestamp".
     *
     * @param objs Map of models
     * @return map of models with "isTimestamp" field set to true for timestamp properties
     */
    public Map<String, Object> postProcessModelsTimestamp(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        List<Map> models = (List<Map>) objs.get("models");
        for (Map map : models ) {
            if (map.containsKey("model")) {
                CodegenModel cm = (CodegenModel) map.get("model");
                for (CodegenProperty prop : cm.vars) {
                    if (prop.dataFormat != null && prop.dataFormat.equals("timestamp")) {
                        prop.isString = null; // null instead of false to match the default behavior of the other booleans
                        prop.vendorExtensions.put("isTimestamp", true);
                    }
                }
            }
        }
        return objs;
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reseved words
     *
     * @return the escaped term
     */
    @Override
    public final String escapeReservedWord(String name) {
        return "`" + name + "`";
    }

    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    public final String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public final String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public final String modelTestFileFolder() {
        return outputFolder + "/" + testSourceFolder + "/" + testPackage().replace('.', File.separatorChar);
    }

    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public final String getTypeDeclaration(Property p) {
        String type;
        if (p instanceof ArrayProperty) {
            ArrayProperty ap = (ArrayProperty) p;
            Property inner = ap.getItems();
            type = getSwaggerType(p) + "[" + getTypeDeclaration(inner) + "]";
        } else if (p instanceof MapProperty) {
            MapProperty mp = (MapProperty) p;
            Property inner = mp.getAdditionalProperties();
            type = getSwaggerType(p) + "[String, " + getTypeDeclaration(inner) + "]";
        } else {
            type = super.getTypeDeclaration(p);
        }
        return type;
    }

    /**
     * Optional - swagger type conversion.  This is used to map swagger types in a `Property` into
     * either language specific types via `typeMapping` or into complex models if there is not a mapping.
     *
     * @return a string value of the type or complex model for this property
     * @see io.swagger.models.properties.Property
     */
    @Override
    public final String getSwaggerType(Property p) {
        String swaggerType;

        Set<String> types = new HashSet<>();
        types.addAll(Arrays.asList(
                "timestamp",
                "local-time",
                "local-date-time"));

        if(p.getClass().equals(StringProperty.class) && types.contains(p.getFormat())) {
            swaggerType = p.getFormat();
        } else {
            swaggerType = super.getSwaggerType(p);
        }

        String type;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = swaggerType;
        return toModelName(type);
    }

    public String getHelp() {
        return "Generates " + getName() + " library.";
    }

    @Override
    public final String toEnumName(CodegenProperty property) {
        return camelize(property.name.split("_"));
    }

    @Override
    public final String toEnumVarName(String value, String datatype) {
        if (NUMBER_TYPES.contains(datatype)) {
            return "Number" + value;
        } else {
            return camelize(value.split("[ _]"));
        }
    }

    @Override
    public final String toEnumValue(String value, String datatype) {
        if (NUMBER_TYPES.contains(datatype)) {
            return value;
        } else {
            return "\"" + escapeText(value).toLowerCase() + "\"";
        }
    }

    static String camelize(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for(String s : parts) {
            sb.append(StringUtils.capitalize(s));
        }
        return sb.toString();
    }
}
