package com.meetup.codegen;

import io.swagger.codegen.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.*;

public class ScalaServerCodegen extends BaseScalaCodegen {

    /**
     * Arguments supported by this generator.
     */
    private enum Arg {
        ARTIFACT_NAME("server", "The artifact name"),
        ARTIFACT_ORGANIZATION("com.meetup", "The artifact organization"),
        ARTIFACT_VERSION("1.0.0-SNAPSHOT", "The artifact version"),
        INCLUDE_SERIALIZATION("true", "To include or not include serializers in the model classes");

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

    public ScalaServerCodegen() {
        super();

        // set the output folder here
        outputFolder = "generated-code/" + getName();

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
                "server/apiRouter.mustache",   // the template to use
                "Router.scala");       // the extension for each file to write

        apiTemplateFiles.put(
                 "api.mustache",   // the template to use
                 ".scala");       // the extension for each file to write

        /*
         * Template Location.  This is the location which templates will be read from.  The generator
         * will use the resource stream to attempt to read the templates.
         */
        templateDir = "meetup-scala";

        /**
         * Api Package.  Optional, if needed, this can be used in templates
         */
        apiPackage = "com.meetup.service";

        /*
         * Model Package.  Optional, if needed, this can be used in templates
         */
        modelPackage = "io.swagger.client.model";
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
        // TODO this should be shared --------->
        invokerPackage =
                additionalProperties.get(Arg.ARTIFACT_ORGANIZATION.argument).toString() +
                        "." + additionalProperties.get(Arg.ARTIFACT_NAME.argument).toString();
        invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

        modelPackage = invokerPackage + ".api.model";
        apiPackage = invokerPackage + ".api.api";
        additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);

        final boolean includeSerialization = Boolean.parseBoolean((String)additionalProperties.get(Arg.INCLUDE_SERIALIZATION.argument));
        additionalProperties.put(Arg.INCLUDE_SERIALIZATION.argument, includeSerialization);

        if (includeSerialization) {
            supportingFiles.add(new SupportingFile("Codec.mustache", invokerFolder, "Codec.scala"));
            supportingFiles.add(new SupportingFile("Serializer.mustache", invokerFolder, "Serializer.scala"));
            supportingFiles.add(new SupportingFile("parserJson4s.mustache", invokerFolder, "Parser.scala"));

            additionalProperties.put("json4s", "true");
            additionalProperties.put("jsonTypePackage", "org.json4s");
            additionalProperties.put("jsonType", "JValue");
        }
        // TODO <---------

        // Now add supporting files as their location depends on the above logic.
        supportingFiles.add(new SupportingFile("server/build.sbt.mustache", "build.sbt"));
        supportingFiles.add(new SupportingFile("server/build.properties.mustache", "project/build.properties"));
        supportingFiles.add(new SupportingFile("server/plugins.sbt.mustache", "project/plugins.sbt"));
        supportingFiles.add(new SupportingFile("server/Makefile.mustache", "Makefile"));
        supportingFiles.add(new SupportingFile("server/Service.mustache", invokerFolder, "Service.scala"));
        supportingFiles.add(new SupportingFile("server/Runner.mustache", invokerFolder, "Runner.scala"));
        supportingFiles.add(new SupportingFile("server/router.mustache", invokerFolder, "Router.scala"));
        supportingFiles.add(new SupportingFile("server/RainbowsHandler.mustache", invokerFolder, "RainbowsHandler.scala"));
        supportingFiles.add(new SupportingFile("server/RequestLoggingHandler.mustache", invokerFolder, "RequestLoggingHandler.scala"));
        supportingFiles.add(new SupportingFile("server/Server.mustache", invokerFolder, "Server.scala"));
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

                        scalaPath = scalaPath + toParamName(itemWithoutBrackets);
                    } else {
                        scalaPath = scalaPath + "\"" + items[i] + "\"";
                    }
                }
            }

            op.vendorExtensions.put("x-meetup-scala-op-case", scalaPath);
        }

        return objs;
    }

}
