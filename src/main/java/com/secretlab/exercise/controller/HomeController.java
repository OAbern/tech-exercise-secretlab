package com.secretlab.exercise.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

  private static final String LATEST_MODIFIED_TIME = "2026-05-21 15:40:00";

  @GetMapping("/")
  public String home() {
    return "Last Push Time : " + LATEST_MODIFIED_TIME;
  }
}
