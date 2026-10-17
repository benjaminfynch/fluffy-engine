package com.interview.assignment.model;

public enum Stop {
  STOP_1("Stop1"),
  STOP_2("Stop2"),
  STOP_3("Stop3");

  private final String id;

  Stop(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }

  public static Stop fromId(String id) {
    return switch (id) {
      case "Stop1" -> STOP_1;
      case "Stop2" -> STOP_2;
      case "Stop3" -> STOP_3;
      default -> throw new IllegalArgumentException("Unknown stop ID: " + id);
    };
  }
}
