## Running the Code Generator

Clone this repository, `cd` to the resulting project directory, and then build
the Meetup service client code generator:

`(cd meetup-scala-generator/ && ./sbt package)`

Note there is a local copy of the `sbt` launcher script from [sbt-extras](https://github.com/paulp/sbt-extras)
included for convenience.

This will have produced a jar of the generator, which we can now use for
code generation:

`CP=target/meetup-scala-client-0.0.1.jar swagger-codegen/swagger-codegen generate -i swagger.yaml -l meetup-scala-client -o generated`

This uses the `swagger-codegen` script to run the [swagger-codegen CLI](https://github.com/swagger-api/swagger-codegen),
which is included in the same directory for convenience.

Now you can poke around the generated source in the `generated` directory.

## Interpretation of the OpenAPI Specification

This generator is intended to target API specification files adhering to the [OpenAPI version 2.0 specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md).

### Treatment of `enum`

The OpenAPI specification defers to the [JSON-Schema Draft 4] for its treatment of [`enum`](http://json-schema.org/latest/json-schema-validation.html#anchor76).

```yaml
  psp:
    description: The payment service provider.
    type: string
    enum:
      - apple
      - stripe
```

```scala
sealed abstract class Psp(val value: String) extends Product with Serializable
case object Apple extends Psp("apple")
case object Stripe extends Psp("stripe")

object Psp {
  private val valueMap =
    Map(
      "apple" -> Apple,
      "stripe" -> Stripe
    )

  val values: Set[Psp] = valueMap.values.toSet
  
  def fromValue(value: String): Option[Psp] = valueMap.get(value)
}
```

```yaml
  subscription:
    description: A Subscription.
    properties:
      psp:
        type: string
        enum:
          - apple
          - stripe
```

```scala
final case class Subscription(psp: Option[Subscription.Psp])
 
object Subscription {
  sealed abstract class Psp(val value: String) extends Product with Serializable
  case object Apple extends Psp("apple")
  case object Stripe extends Psp("stripe")

  object Psp {
    private val valueMap =
      Map(
        "apple" -> Apple,
        "stripe" -> Stripe
      )
    
    val values: Set[Psp] = valueMap.values.toSet
    
    def fromValue(value: String): Option[Psp] = valueMap.get(value)
  }
}
```