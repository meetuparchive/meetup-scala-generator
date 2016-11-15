package com.meetup.codegen;

import io.swagger.codegen.CodegenType;
import io.swagger.codegen.SupportingFile;

public class ScalaClientCodegen extends BaseScalaCodegen {

    public CodegenType getTag() { return CodegenType.CLIENT; }

    public String getName() {
        return "meetup-scala-client";
    }

    public ScalaClientCodegen() {
        super();

        // set the output folder here
        outputFolder = "generated-code/" + getName();

        modelTemplateFiles.put(
                "model.mustache", // the template to use
                ".scala");        // the extension for each file to write

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "api.mustache",   // the template to use
                ".scala");        // the extension for each file to write

    }

    @Override
    public void processOpts() {
        super.processOpts();

        supportingFiles.add(new SupportingFile("build.sbt.mustache", "build.sbt"));
        supportingFiles.add(new SupportingFile("Codec.mustache", invokerFolder, "Codec.scala"));
        supportingFiles.add(new SupportingFile("Serializer.mustache", invokerFolder, "Serializer.scala"));
    }

}
