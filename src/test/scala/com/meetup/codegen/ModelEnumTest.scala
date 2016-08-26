package com.meetup.codegen

import java.util.{Arrays => JArrays, HashMap => JHashMap, List => JList, Map => JMap}
import java.util.Collections
import io.swagger.models.ModelImpl
import io.swagger.models.properties.{IntegerProperty, StringProperty}
import org.scalatest.{FunSpec, Matchers}
import scala.collection.JavaConverters._

class ModelEnumTest extends FunSpec with Matchers {

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

  val properties = generatedModel.vars.asScala.toList

  describe("a generated model with enum properties") {
    it("should have the expected number of properties") {
      properties.length shouldBe 2
    }

    it("should have only enum properties") {
      properties.forall(_.isEnum == true) shouldBe true
    }

    it("should have an enum whose baseType is \"String\"") {
      properties.exists(_.baseType == "String") shouldBe true
    }

    it("should have an enum whose baseType is \"Integer\"") {
      properties.exists(_.baseType == "Integer") shouldBe true
    }
  }

  describe("generated enum properties") {
    it("should have enum names properly mangled") {
      properties.forall(p => p.datatypeWithEnum == codeGen.toEnumName(p)) shouldBe true
    }

    it("should have enum instance names properly mangled") {
      properties.foreach { property =>
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
            .map(_.get("name"))

        actualNames.forall(expectedNames.contains) shouldBe true}
    }
  }
}