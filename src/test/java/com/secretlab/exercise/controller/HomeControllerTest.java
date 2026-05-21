package com.secretlab.exercise.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("GET unknown path — returns resource not found")
  void unknownPath_returnsResourceNotFound() throws Exception {
    mockMvc
        .perform(get("/unknown-path"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("resource not found"));
  }

  @Test
  @DisplayName("GET missing static resource — returns resource not found")
  void missingStaticResource_returnsResourceNotFound() throws Exception {
    mockMvc
        .perform(get("/missing-resource.css"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error").value("resource not found"));
  }
}
