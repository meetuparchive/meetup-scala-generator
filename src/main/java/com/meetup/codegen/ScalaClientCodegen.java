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

        invokerPackage = "com.meetup.client";

        // set the output folder here
        outputFolder = "generated-code/" + getName();

        modelTemplateFiles.put(
                "model.mustache", // the template to use
                ".scala");        // the extension for each file to write //

        apiTemplateFiles.put(
                "client/api.mustache",   // the template to use
                ".scala");        // the extension for each file to write

        apiTemplateFiles.put(
                "client/ApiInvoker.mustache",   // the template to use
                ".scala");        // the extension for each file to write
    }

    @Override
    public void processOpts() {
        super.processOpts();

        supportingFiles.add(new SupportingFile("client/ApiInvoker.mustache", invokerFolder, "ApiInvoker.scala"));
    }

}
