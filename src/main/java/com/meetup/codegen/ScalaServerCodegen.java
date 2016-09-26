package com.meetup.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class ScalaServerCodegen extends DefaultCodegen implements CodegenConfig {

    private String invokerPackage = "io.swagger";

    // source folder where to write the files
    private final String sourceFolder = "src/main/scala";
    private String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

    private static final Set<String> NUMBER_TYPES = new HashSet<>();

    public static Set<String> getNumberTypes() {
        Set<String> copy = new HashSet<>();
        copy.addAll(NUMBER_TYPES);
        return copy;
    }

    static {
        NUMBER_TYPES.addAll(Arrays.asList("Integer", "Long", "Float", "Double"));
    }

    /**
     * Arguments supported by this generator.
     */
    private enum Arg {
        ARTIFACT_NAME("server", "The artifact name"),
        ARTIFACT_ORGANIZATION("com.meetup", "The artifact organization"),
        ARTIFACT_VERSION("1.0.0-SNAPSHOT", "The artifact version");

        public final String value;
        public final String description;
        public final String argument;

        Arg(String value, String description) {
            this.value = value;
            this.description = description;

            String a = name().toLowerCase();
            StringBuilder sb = new StringBuilder();
            List<String> segments = Arrays.asList(a.split("_"));
            sb.append(segments.get(0));

            for (String segment : segments.subList(1, segments.size())) {
                sb.append(segment.substring(0, 1).toUpperCase());
                sb.append(segment.substring(1));
            }

            this.argument = sb.toString();
        }
    }

    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    public String getName() {
        return "meetup-scala-server";
    }

    public String getHelp() {
        return "Generates " + getName() + " library.";
    }

    public ScalaServerCodegen() {
        super();

        // set the output folder here
        outputFolder = "generated-code/meetup-scala-server";

        for (Arg d : Arg.values()) {
            cliOptions.add(CliOption.newString(d.argument, d.description));
        }

        /*
         * Models.  You can write model files using the modelTemplateFiles map.
         * if you want to create one template for file, you can do so here.
         * for multiple files for model, just put another entry in the `modelTemplateFiles` with
         * a different extension
         */
        modelTemplateFiles.put(
                "model.mustache", // the template to use
                ".scala");       // the extension for each file to write


        /**
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */

        apiTemplateFiles.put(
                "server/routing.mustache",   // the template to use
                "Router.scala");       // the extension for each file to write

        apiTemplateFiles.put(
                 "api.mustache",   // the template to use
                 ".scala");       // the extension for each file to write

        apiTemplateFiles.put(
                "server/Main.mustache",   // the template to use
                "Main.scala");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "meetup-scala";

        /**
         * Api Package.  Optional, if needed, this can be used in templates
         */
        apiPackage = "com.meetup.foo.api";

        /*
         * Model Package.  Optional, if needed, this can be used in templates
         */
        modelPackage = "io.swagger.client.model";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        reservedWords = new HashSet<>(); // TODO add scala (and template?) reserved words

        /*
         * Language Specific Primitives.  These types will not trigger imports by
         * the client generator
         */
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

        typeMapping = new HashMap<>();
        typeMapping.put("array", "List");
        typeMapping.put("DateTime", "ZonedDateTime");
        typeMapping.put("long", "Long");
        typeMapping.put("int", "Int");
        typeMapping.put("Integer", "Int");

        importMapping.put("Date", "LocalDate");
    }

    @Override
    public void processOpts() {
        super.processOpts();
        for (Arg arg : Arg.values()) {
            Object _value = additionalProperties.get(arg.argument);
            String value = _value == null ? arg.value : _value.toString();
            additionalProperties.put(arg.argument, value);
            System.out.println(String.format("Exposed %s as %s", arg.argument, arg.value));
        }

        // Set the invoker package and folder to the artifact organization + client.
        invokerPackage =
                additionalProperties.get(Arg.ARTIFACT_ORGANIZATION.argument).toString() +
                        "." + additionalProperties.get(Arg.ARTIFACT_NAME.argument).toString();
        invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

        modelPackage = invokerPackage + ".api.model";
        apiPackage = invokerPackage + ".api.api";
        additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);

        // Now add supporting files as their location depends on the above logic.
        supportingFiles.add(new SupportingFile("server/build.sbt.mustache", "build.sbt"));
        supportingFiles.add(new SupportingFile("server/build.properties.mustache", "project/build.properties"));
        supportingFiles.add(new SupportingFile("server/plugins.sbt.mustache", "project/plugins.sbt"));
        supportingFiles.add(new SupportingFile("server/Makefile.mustache", "Makefile"));
        supportingFiles.add(new SupportingFile("server/Application.mustache", invokerFolder, "Application.scala"));
        supportingFiles.add(new SupportingFile("server/Runner.mustache", invokerFolder, "Runner.scala"));
        supportingFiles.add(new SupportingFile("server/RainbowsHandler.mustache", invokerFolder, "RainbowsHandler.scala"));
        supportingFiles.add(new SupportingFile("server/RequestLoggingHandler.mustache", invokerFolder, "RequestLoggingHandler.scala"));
        supportingFiles.add(new SupportingFile("server/Server.mustache", invokerFolder, "Server.scala"));

        // common
        supportingFiles.add(new SupportingFile("Codec.mustache", invokerFolder, "Codec.scala"));
        supportingFiles.add(new SupportingFile("Serializer.mustache", invokerFolder, "Serializer.scala"));


        // TODO this should be a generator option
        additionalProperties.put("json4s", "true");
        additionalProperties.put("jsonTypePackage", "org.json4s");
        additionalProperties.put("jsonType", "JValue");
        supportingFiles.add(new SupportingFile("parserJson4s.mustache", invokerFolder, "Parser.scala"));
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reseved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        return "`" + name + "`";
    }

    /**
     * Location to write model files.  You can use the modelPackage() as defined when the class is
     * instantiated
     */
    public String modelFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
    }


    /**
     * Optional - type declaration.  This is a String which is used by the templates to instantiate your
     * types.  There is typically special handling for different property types
     *
     * @return a string value used as the `dataType` field for model templates, `returnType` for api templates
     */
    @Override
    public String getTypeDeclaration(Property p) {
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
    public String getSwaggerType(Property p) {
        String swaggerType = super.getSwaggerType(p);
        String type;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type))
                return toModelName(type);
        } else
            type = swaggerType;
        return toModelName(type);
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // import sanitization lifted from ScalaClientCodegen
        @SuppressWarnings("unchecked")
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        final String prefix = modelPackage() + ".";
        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(prefix)) iterator.remove();
        }

        // Now subject the models to Enum treatment.
        return postProcessModelsEnum(objs);
    }


    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        List<CodegenOperation> operationList = (List<CodegenOperation>) operations.get("operation");
        for (CodegenOperation op : operationList) {
            String[] items = op.path.split("/", -1);
            String scalaPath = "";

            for (int i = 1; i < items.length; ++i) {
                if (i > 1) {
                    scalaPath = scalaPath + " :: ";
                }
                if (items[i] != "" && items[i] != null) {
                    if (items[i].matches("^\\{(.*)\\}$")) { // wrap in {}
                        String itemWithoutBrackets = items[i].replace("{", "").replace("}", "");

                        scalaPath = scalaPath + itemWithoutBrackets;
                    } else {
                        scalaPath = scalaPath + "\"" + items[i] + "\"";
                    }
                }
            }

            op.vendorExtensions.put("x-meetup-scala-op-case", scalaPath);
        }

        return objs;
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        return camelize(property.name.split("_"));
    }

    public static String camelize(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for(String s : parts) {
            sb.append(StringUtils.capitalize(s));
        }
        return sb.toString();
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (NUMBER_TYPES.contains(datatype)) {
            return "Number" + value;
        } else {
            return camelize(value.split("[ _]"));
        }
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if (NUMBER_TYPES.contains(datatype)) {
            return value;
        } else {
            return "\"" + escapeText(value).toLowerCase() + "\"";
        }
    }
}
