package com.example

import com.example.data.model.Company
import com.example.data.model.Product
import com.example.data.model.TextScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun companyAndProductCreation_isCorrect() {
    val company = Company(id = 1, name = "Lay's", logoPath = "sample_chips.jpg")
    val product = Product(id = 10, companyId = company.id, name = "Classic Salted", imagePath = "sample_chips.jpg")

    assertEquals("Lay's", company.name)
    assertEquals(1L, product.companyId)
    assertEquals("Classic Salted", product.name)
  }

  @Test
  fun textScale_factorsAreOrdered() {
    val scales = TextScale.values()
    for (i in 0 until scales.size - 1) {
      assert(scales[i].scaleFactor < scales[i + 1].scaleFactor)
    }
  }
}
