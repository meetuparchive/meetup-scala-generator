Clone this repository, `cd` to the resulting project directory, and then build
the Meetup service client code generator:

`(cd meetup-scala-generator/ && ./sbt package)`

Note there is a local copy of the `sbt` launcher script from [sbt-extras](https://github.com/paulp/sbt-extras)
included for convenience.

This will have produced a jar of the generator, which we can now use for
code generation:

`CP=target/mup-scala-0.0.1.jar swagger-codegen/swagger-codegen generate -i swagger.yaml -l meetupscala -o generated`

This uses the `swagger-codegen` script to run the [swagger-codegen CLI](https://github.com/swagger-api/swagger-codegen),
which is included in the same directory for convenience.

Now you can poke around the generated source in the `generated` directory.
