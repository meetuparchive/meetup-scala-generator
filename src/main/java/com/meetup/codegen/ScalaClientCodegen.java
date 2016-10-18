package com.meetup.codegen;

import io.swagger.codegen.CliOption;
import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;

import java.util.Arrays;
import java.util.List;

public class ScalaClientCodegen extends BaseScalaCodegen {

    protected String invokerPackage = "io.swagger";
    protected String invokerFolder = (sourceFolder + '/' + invokerPackage).replace(".", "/");

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

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "api.mustache",   // the template to use
                ".scala");       // the extension for each file to write

        /*
         * Supporting Files.  You can write single files for the generator with the
         * entire object tree available.  If the input file has a suffix of `.mustache
         * it will be processed by the template engine.  Otherwise, it will be copied
         */
        supportingFiles.add(new SupportingFile("build.sbt.mustache", "build.sbt"));
        supportingFiles.add(new SupportingFile("Codec.mustache", invokerFolder, "Codec.scala"));
        supportingFiles.add(new SupportingFile("Serializer.mustache", invokerFolder, "Serializer.scala"));
    }

    @Override
    public void processOpts() {
        super.processOpts();
        for (Arg arg : Arg.values()) {
            Object _value = additionalProperties.get(arg.argument);
            String value = _value == null ? arg.value : _value.toString();
            additionalProperties.put(arg.argument, value);
        }

        // TODO this should be a generator option
        additionalProperties.put("json4s", "true");
        additionalProperties.put("jsonTypePackage", "org.json4s");
        additionalProperties.put("jsonType", "JValue");
        supportingFiles.add(new SupportingFile("parserJson4s.mustache", invokerFolder, "Parser.scala"));
    }

}
