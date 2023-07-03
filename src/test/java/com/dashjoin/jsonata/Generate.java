package com.dashjoin.jsonata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Generate {

  public static void main(String[] args) throws IOException {

    new File("src/test/java/com/dashjoin/jsonata/gen").mkdirs();
    File suites = new File("test/test-suite/groups");
    for (File suite : suites.listFiles()) {

      StringBuffer b = new StringBuffer();
      b.append("package com.dashjoin.jsonata.gen;\n");
      b.append("import org.junit.Test;\n");
      b.append("import com.dashjoin.jsonata.JsonataTest;\n");
      b.append("public class " + suite.getName().replace('-', '_') + "Test {\n");

      for (File cas : suite.listFiles()) {
        String name = cas.getName().substring(0, cas.getName().length() - 5);
        name = name.replace('-', '_');
        b.append("@Test public void " + name.replace('.', '_') + "() throws Exception { \n");
        b.append("  new JsonataTest().runCase(\"test/test-suite/groups/" + suite.getName()
            + "/" + name + ".json\");\n");
        b.append("}\n");
      }
      b.append("}\n");
      Files.write(Path.of("src/test/java/com/dashjoin/jsonata/gen/"
          + suite.getName().replace('-', '_') + "Test.java"), b.toString().getBytes());
      System.out.println(b);
    }
  }
}
