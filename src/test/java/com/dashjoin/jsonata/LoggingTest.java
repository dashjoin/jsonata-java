package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that debug output goes through java.util.logging and is off by default
 */
public class LoggingTest {

  static final Logger root = Logger.getLogger("com.dashjoin.jsonata");

  final List<LogRecord> records = new ArrayList<>();

  final Handler handler = new Handler() {
    @Override
    public void publish(LogRecord record) {
      records.add(record);
    }

    @Override
    public void flush() {}

    @Override
    public void close() {}
  };

  Level oldLevel;

  @BeforeEach
  public void setup() {
    oldLevel = root.getLevel();
    handler.setLevel(Level.ALL);
    root.addHandler(handler);
  }

  @AfterEach
  public void teardown() {
    root.removeHandler(handler);
    root.setLevel(oldLevel);
  }

  @Test
  public void testNoTraceByDefault() {
    root.setLevel(Level.INFO);
    Assertions.assertEquals(3, eval("$x + 2", "x", 1));
    Assertions.assertTrue(records.isEmpty(), "unexpected log records: " + records.size());
  }

  @Test
  public void testTraceEnabled() {
    root.setLevel(Level.FINE);
    Assertions.assertEquals(3, eval("$x + 2", "x", 1));

    Assertions.assertTrue(records.stream().allMatch(r -> r.getLevel() == Level.FINE));
    Assertions.assertTrue(records.stream().anyMatch(
        r -> r.getLoggerName().equals(Parser.class.getName()) && r.getMessage().startsWith("advance ")));
    Assertions.assertTrue(records.stream().anyMatch(
        r -> r.getLoggerName().equals(Jsonata.class.getName()) && r.getMessage().startsWith("eval expr=")));
    Assertions.assertTrue(records.stream().anyMatch(
        r -> r.getMessage().equals("variable name=x val=1")));
  }

  static Object eval(String expr, String name, Object value) {
    Jsonata jsonata = jsonata(expr);
    Jsonata.Frame frame = jsonata.createFrame();
    frame.bind(name, value);
    return jsonata.evaluate(null, frame);
  }
}
