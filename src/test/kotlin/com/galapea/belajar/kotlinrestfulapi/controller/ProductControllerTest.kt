package com.galapea.belajar.kotlinrestfulapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.galapea.belajar.kotlinrestfulapi.model.CreateProductRequest
import com.galapea.belajar.kotlinrestfulapi.model.ProductResponse
import com.galapea.belajar.kotlinrestfulapi.service.ProductService
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultHandlers
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*
import javax.validation.ConstraintViolationException
import javax.validation.Validation


@WebMvcTest(controllers = [ProductController::class])
class ProductControllerTest(
    @Autowired val mockMvc: MockMvc,
    @Autowired val objectMapper: ObjectMapper
) {

    @MockBean
    lateinit var productService: ProductService

    @Test
    fun whenPostCreateProduct_thenReturnNewProduct() {
        val createProductRequest = CreateProductRequest(id = "x123", name = "A gift", price = 6990, quantity = 3)
        val productResponse = ProductResponse(
            id = createProductRequest.id,
            name = createProductRequest.name,
            price = createProductRequest.price,
            quantity = createProductRequest.quantity,
            createdAt = Date(),
            updatedAt = null
        )
        whenever(productService.create(createProductRequest)).thenReturn(productResponse)
        mockMvc.perform(
            post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createProductRequest))
        )
            .andDo { MockMvcResultHandlers.print() }
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.data.id").value("x123"))
            .andExpect(jsonPath("$.data.name").value("A gift"))
    }

    @Test
    fun givenInvalidRequest_whenPostCreateProduct_thenReturnStatus400() {
        val createProductRequest = CreateProductRequest(id = "x123", name = "A gift", price = 0, quantity = -1)
        val validatorFactory = Validation.buildDefaultValidatorFactory()
        val validator = validatorFactory.validator
        val constraints = validator.validate(createProductRequest)
        whenever(productService.create(createProductRequest)).then { throw ConstraintViolationException(constraints) }

        val mvcResult = mockMvc.perform(
            post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createProductRequest))
        )
            .andExpect(status().is4xxClientError)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code", `is`(400)))
            .andExpect(jsonPath("$.status", `is`("Bad Request")))
            .andExpect(jsonPath("$.data").isNotEmpty)
            .andExpect(jsonPath("$.data.code", containsString("validation_failure")))
            .andExpect(jsonPath("$.data.message", containsString("Validation failed.")))
            .andExpect(jsonPath("$.data.errors[*].path", containsInAnyOrder("price", "quantity")))
            .andExpect(
                jsonPath(
                    "$.data.errors[*].message", containsInAnyOrder(
                        "must be greater than or equal to 1",
                        "must be greater than or equal to 0"
                    )
                )
            )
            .andReturn()
        println(mvcResult.response.contentAsString)
    }
}