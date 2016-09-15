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

## Models and JSON

In addition to generating models, the generator also generates bidirectional JSON codecs for each.
Having run the commands above you should have a `generated` directory that contains the generated source,
which is itself an SBT project. Let's start a REPL to check things out:

`(cd generated && ../sbt console)`

Now construct a plan and serialize it to a JSON string:

```scala
val plan: Plan = Plan("3 group unlimited plan", Entitlements(maxGroups = Some(3), maxUsers = None))
val planJson: String = Serializer.serialize(plan)
```

This constructs a plan that would permit the organizer to create at most 3 MUGs with no limit on the membership count of each.
Pretty-printed, we have:

```scala
{
  "entitlements": {
    "maxGroups": 3
  },
  "name": "3 group unlimited plan"
}
```

Now that we have this JSON blob in the `planJson` variable, let's go backwards and use it to deserialize a `Plan` instance:

```scala
val planResult: Either[String, Plan] = Serializer.deserialize[Plan](planJson)
```

There are two things to note here. First, the result type is `Either[String, Plan]` rather than simply `Plan`. This is because
deserialization can fail, either because the input string is not valid JSON, or it is, but its structure isn't comprehensible
as a serialized plan. Second, we had to explicitly pass the `Plan` type parameter to the `deserialize` call. At a high-level
this is because the implementation relies on static, compile-time proof that a `Plan` can be deserialized. 

`Serializer` is a convenience module that relies on two other components: the `Parser` module functions and the `Codec` instances
for each model. Let's use those directly to get a sense of what the `Serializer` does:

```scala
val planJValue: JValue = Codec.encode(plan)
val planJson: String = Parser.deparse(planJValue)
```

Here we pass the `plan` value, the same `Plan` instance we created above, to `Codec.encode`. `Codec` is both a module and a
typeclass, instances of which are parameterized on a given type and provide bidirectional serialization between that type
and a structured JSON AST, not a raw string. `Codec.encode` simply resolves the `Codec` instance for the type of the given
value and encodes it in this AST. Next we pass that resulting value to `Parser.deparse`, which can safely convert the
structured AST value into a valid JSON string. Now let's convert it back to a `Plan` instance:

```scala
val parsedJValue: Either[String, JValue] = Parser.parse(planJson)
val parsedPlan: Either[String, Plan] = parsedJValue.right.flatMap(Codec.decode[Plan])
```

First we use `Parser.parse` to attempt to parse a raw string into a structure JSON representation
(currently a `JValue` instance). This returns an `Either[String, JValue]` because it may fail to parse. Next we
`flatMap` over that value using `Codec.decode` to decode the `JValue` as a `Plan`. As this too can fail, it results in
an `Either[String, JValue]`, so we use `flatMap` to end up with a "flattened" structure.

## Interpretation of the OpenAPI Specification

This generator is intended to target API specification files adhering to the [OpenAPI version 2.0 specification](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md).

### Treatment of `enum`

The OpenAPI specification defers to the [JSON-Schema Draft 4] for its treatment of [`enum`](http://json-schema.org/latest/json-schema-validation.html#anchor76).
We currently support both standalone definitions defined as enums themselves as well as inline enums.

Enum values must be heterogeneous. Supported value types are a subset of that allowed in JSON-schema, for simplicity's sake.
Specifically, these are: `boolean`, `integer`, `number`, and `string`.

The code generation strategy for both standalone and embedded enums is the same:

- sealed type hierarchy
- instances as case objects with both the `toString` and `value` members rendering the original value.
- a companion with the instances available as a `Set`, as well as a function, `fromValue`, for translating a raw value
- into an instance

Consider this standalone example:

```yaml
  psp:
    description: The payment service provider.
    type: string
    enum:
      - apple
      - stripe
```

This would result in the following scala code:

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

Consider this embedded example:

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

This would result in the following scala code:

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

Note that we infer the root enum type from the property name. We also place the hierarchy within the companion object of
the class generated to represent the containing model in the specification.

### Handling Subtype Polymorphism

*Note: proper support is still pending - please refrain from use at this time.*

The OpenAPI version 2.0 specification explicitly supports subtype polymorphism, or "inheritance", through a combination of its interpretation of the JSON-Schema
[`allOf`](http://json-schema.org/latest/json-schema-validation.html#anchor82) key word and the OpenAPI [`discriminator`](https://github.com/OAI/OpenAPI-Specification/blob/master/versions/2.0.md#fixed-fields-13) field.
The following defines an subtype relationship between to sibling models, `Triangle` and `Rectangle`, and their parent, `Shape`:

```yaml
  Shape:
    type: object
    discriminator: shapeType
    properties:
      name:
        type: string
      shapeType:
        type: string
    required:
      - name
      - shapeType
  Triangle:
    description: A representation of a triangle
    allOf:
    - $ref: "#/definitions/Shape"
    - type: object
      properties:
        base:
          type: integer
          description: The base length
        height:
          type: integer
          description: The height
      required:
        - base
        - height
  Rectangle:
    description: A representation of a rectangle
    allOf:
    - $ref: "#/definitions/Shape"
    - type: object
      properties:
        length:
          type: integer
          description: The length of the rectangle
        width:
          type: integer
          description: The width of the rectangle
      required:
        - length
        - width
```

Which will result in the following class hierarchy:

```scala
trait Shape {
  def name: String

  def shapeType: String
}
```

```scala
/**
 * A representation of a triangle
 */
final case class Triangle (
  name: String,
  shapeType: String,
  /* The base length */
  base: Int,
  /* The height */
  height: Int) extends Shape
```

```scala
/**
 * A representation of a rectangle
 */
final case class Rectangle (
  name: String,
  shapeType: String,
  /* The length of the rectangle */
  length: Int,
  /* The width of the rectangle */
  width: Int) extends Shape
```