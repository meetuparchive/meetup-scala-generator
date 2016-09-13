package com.meetup.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class ScalaClientCodegen extends DefaultCodegen implements CodegenConfig {

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
        CLIENT_NAME("client", "The artifact thing"),
        CLIENT_ORGANIZATION("com.meetup.client", "The org .."),
        CLIENT_VERSION("1.0.0", "The client version");

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

    // source folder where to write the files
    private final String sourceFolder = "src/main/scala";

    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    public String getName() {
        return "meetup-scala-client";
    }

    public String getHelp() {
        return "Generates " + getName() + " library.";
    }

    public ScalaClientCodegen() {
        super();

        // set the output folder here
        outputFolder = "generated-code/meetup-scala-client";

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

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "api.mustache",   // the template to use
                ".scala");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "meetup-scala-client";

        /*
         * Api Package.  Optional, if needed, this can be used in templates
         */
        apiPackage = "io.swagger.client.api";

        /*
         * Model Package.  Optional, if needed, this can be used in templates
         */
        modelPackage = "io.swagger.client.model";

        /*
         * Reserved words.  Override this with reserved words specific to your language
         */
        reservedWords = new HashSet<>(
                Arrays.asList(
                        "sample1",  // replace with static values
                        "sample2")
        );

        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("foo", "bar");

        /*
         * Supporting Files.  You can write single files for the generator with the
         * entire object tree available.  If the input file has a suffix of `.mustache
         * it will be processed by the template engine.  Otherwise, it will be copied
         */
        supportingFiles.add(new SupportingFile("build.sbt.mustache", "build.sbt"));

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
        }
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
