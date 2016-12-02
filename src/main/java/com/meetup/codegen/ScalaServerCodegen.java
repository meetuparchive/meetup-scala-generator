package com.meetup.codegen;

import io.swagger.codegen.*;

import java.util.*;

public class ScalaServerCodegen extends BaseScalaCodegen {

    public CodegenType getTag() { return CodegenType.SERVER; }

    public String getName() {
        return "meetup-scala-server";
    }

    public ScalaServerCodegen() {
        super();

        invokerPackage = "com.meetup.server";

        modelTemplateFiles.put(
                "model.mustache", // the template to use
                ".scala");        // the extension for each file to write

        /*
         * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
         * as with models, add multiple entries with different extensions for multiple files per
         * class
         */
        apiTemplateFiles.put(
                "server/apiRouter.mustache",   // the template to use
                "Router.scala");       // the extension for each file to write
    }

    @Override
    public void processOpts() {
        super.processOpts();

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
