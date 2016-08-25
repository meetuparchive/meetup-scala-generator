package com.meetup.codegen

import java.util.{Arrays => JArrays, HashMap => JHashMap, List => JList, Map => JMap}
import java.util.Collections
import io.swagger.models.ModelImpl
import io.swagger.models.properties.{IntegerProperty, StringProperty}
import org.scalatest.{FunSpec, Matchers}
import scala.collection.JavaConverters._

class ScalaModelEnumTest extends FunSpec with Matchers {

  val codeGen = new ScalaClientCodegen()

  val generatedModel = {
    val stringEnumProperty = new StringProperty
    stringEnumProperty.setEnum(JArrays.asList("apple", "stripe"))

    val intEnumProperty = new IntegerProperty
    intEnumProperty.setEnum(JArrays.asList(1, 2))

    val model = new ModelImpl()
      .property("some_strings", stringEnumProperty)
      .property("some_ints", intEnumProperty)

    val m = codeGen.fromModel("test", model)

    val modelProps = new JHashMap[String, AnyRef]()
    modelProps.put("model", m)

    val root = new JHashMap[String, AnyRef]()
    root.put("imports", Collections.emptyList[Map[String, String]]())
    root.put("models", JArrays.asList(modelProps))

    codeGen.postProcessModels(root)

    m
  }

  describe("a generated model with enum properties") {
    it("should have the expected number of properties") {
      generatedModel.vars.size() shouldBe 2
    }

    it("should have only enum properties") {
      generatedModel.vars.asScala.forall(_.isEnum == true) shouldBe true
    }

    it("should have an enum whose baseType is \"String\"") {
      val property = generatedModel.vars.get(0)
      property.baseType shouldBe "String"
    }

    it("should have an enum whose baseType is \"Integer\"") {
      val property = generatedModel.vars.get(1)
      property.baseType shouldBe "Integer"
    }

    it("should have enum instance names properly mangled") {
      val property = generatedModel.vars.get(1)

      val rawAllowedValues =
        property
          .allowableValues.get("values")
          .asInstanceOf[JList[AnyRef]]
          .asScala
          .map(_.toString)

      val expectedNames =
        rawAllowedValues.map { v =>
          codeGen.toEnumVarName(v, property.baseType)
        }.toSet

      val actualNames =
        property
          .allowableValues.get("enumVars")
          .asInstanceOf[JList[JMap[String, String]]]
          .asScala
          .toList
          .map(_.get("name"))

      actualNames.forall(expectedNames.contains) shouldBe true
    }
  }
}