package com.dashjoin.jsonata;

import static com.dashjoin.jsonata.Jsonata.jsonata;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import com.dashjoin.jsonata.Jsonata.JFunction;
import com.dashjoin.jsonata.Jsonata.JFunctionCallable;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SerializationTest {

  @Test
  public void testJFunction() throws Exception {
    // return the function and test its serialization
    Jsonata expr = jsonata("$foo");
    expr.registerFunction("foo", new JFunction(new JFunctionCallable() {

      @SuppressWarnings("rawtypes")
      @Override
      public Object call(Object input, List args) throws Throwable {
        return null;
      }
      
    }, null));
    ObjectMapper om = new ObjectMapper();
    System.out.println(expr.evaluate(null).getClass());
    om.writeValueAsString(expr.evaluate(null));
  }

  /**
   * wrapper class that makes Jsonata serializable
   */
  public static class SerializableExpression implements Serializable {

    /**
     * jsonata expression
     */
    public String expression;
    
    /**
     * parsed / transient expression
     */
    transient public Jsonata jsonata;

    /**
     * constructor calls init
     */
    public SerializableExpression(String expression) {
      init(expression);
    }

    /**
     * init the object before calling evaluate
     */
    public void init(String expression) {
      // remember jsonata string
      this.expression = expression;
      
      // parse expression
      jsonata = jsonata(expression);
      
      // register any custom functions
      jsonata.registerFunction("hi", () -> "hello world");
    }

    private static final long serialVersionUID = 7675531659407424684L;

    /**
     * custom serializer
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
      // only write jsonata string
      out.writeUTF(expression);
    }

    /**
     * custom deserializer
     */
    private void readObject(java.io.ObjectInputStream in)
        throws IOException, ClassNotFoundException {
      // read jsonata string and init
      init(in.readUTF());
    }
  }

  /**
   * test RMI / hazelcast serialization
   */
  @Test
  public void testSerializable() throws Exception {
    // sample expression with custom function
    SerializableExpression expr = new SerializableExpression("$hi() & '!'");

    // test output
    Assertions.assertEquals("hello world!", expr.jsonata.evaluate(null));

    // buffer (i.e. network or file transport)
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    
    // write to buffer
    ObjectOutputStream oos = new ObjectOutputStream(buffer);
    oos.writeObject(expr);

    // read from buffer
    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()));
    SerializableExpression clone = (SerializableExpression) ois.readObject();
    
    // clone has same result
    Assertions.assertEquals(expr.jsonata.evaluate(null), clone.jsonata.evaluate(null));
  }
}
