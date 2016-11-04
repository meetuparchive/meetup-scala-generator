package com.meetup.codegen;

import io.swagger.codegen.CodegenType;

public final class TestScalaCodegen {

    static private final class AdHoc extends BaseScalaCodegen {
        private final String name;
        private final CodegenType tag;

        private AdHoc(String name, CodegenType tag) {
            this.name = name;
            this.tag = tag;
        }

        @Override
        public CodegenType getTag() {
            return null;
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public String toString() {
            return "AdHoc{" +
                    "name='" + name + '\'' +
                    ", tag=" + tag +
                    '}';
        }
    }

    public static BaseScalaCodegen get(CodegenType tag) {
        return new AdHoc("test-meetup-scala-" + tag.name(), tag);
    }

    public static BaseScalaCodegen getServer() {
        return get(CodegenType.SERVER);
    }
}
