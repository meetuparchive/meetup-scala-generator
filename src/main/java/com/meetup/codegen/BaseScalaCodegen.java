package com.meetup.codegen;

import io.swagger.codegen.CodegenConfig;
import io.swagger.codegen.CodegenProperty;
import io.swagger.codegen.DefaultCodegen;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

abstract class BaseScalaCodegen extends DefaultCodegen implements CodegenConfig {

    protected String sourceFolder = "src/main/scala";
    protected String invokerPackage = "io.swagger";
    protected String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

    private static final Set<String> NUMBER_TYPES =
            new HashSet<>(Arrays.asList("Int", "Long", "Float", "Double"));

    public static Set<String> getNumberTypes() {
        Set<String> copy = new HashSet<>();
        copy.addAll(NUMBER_TYPES);
        return copy;
    }

    BaseScalaCodegen() {
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
        typeMapping.put("DateTime", "ZonedDateTime");

        typeMapping.put("int", "Int");

        typeMapping.put("timestamp", "Instant");
        importMapping.put("Instant", "java.time.Instant");

        typeMapping.put("local-time", "LocalTime");
        importMapping.put("LocalTime", "java.time.LocalTime");

        typeMapping.put("local-date-time", "LocalDateTime");
        importMapping.put("LocalDateTime", "java.time.LocalDateTime");
    }

    @Override
    public void processOpts() {
        super.processOpts();
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
    public String toEnumName(CodegenProperty property) {
        return camelize(property.name.split("_"));
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

    static String camelize(String[] parts) {
        StringBuilder sb = new StringBuilder();
        for(String s : parts) {
            sb.append(StringUtils.capitalize(s));
        }
        return sb.toString();
    }
}
