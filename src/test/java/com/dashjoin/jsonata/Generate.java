package com.dashjoin.jsonata;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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

      File[] cases = suite.listFiles();
      Arrays.sort(cases);
      for (File cas : cases) {
        // Skip all non-JSON
        if (!cas.getName().endsWith(".json")) continue;

        String name = cas.getName().substring(0, cas.getName().length() - 5);
        String jname = name.replace('-', '_');

        Object jsonCase = new JsonataTest().readJson(cas.getAbsolutePath());
        if (jsonCase instanceof List) {
          for (int i=0; i<((List)jsonCase).size(); i++) {
            b.append("// " + s(((Map)((List) jsonCase).get(i)).get("expr"))+"\n");
        b.append("@Test public void " + jname.replace('.', '_') + "_case_"+i+ "() throws Exception { \n");
        b.append("  new JsonataTest().runSubCase(\"test/test-suite/groups/" + suite.getName()
            + "/" + name + ".json\", "+i+");\n");
        b.append("}\n");
          }
        }
        else {
          b.append("// " + s(((Map) jsonCase).get("expr"))+"\n");
        b.append("@Test public void " + jname.replace('.', '_') + "() throws Exception { \n");
        b.append("  new JsonataTest().runCase(\"test/test-suite/groups/" + suite.getName()
            + "/" + name + ".json\");\n");
        b.append("}\n");
        }
      }
      b.append("}\n");
      Files.write(Path.of("src/test/java/com/dashjoin/jsonata/gen/"
          + suite.getName().replace('-', '_') + "Test.java"), b.toString().getBytes());
      System.out.println(b);
    }
  }
  
  static String s(Object o) {
    if (o == null)
      return null;
    String s = (String)o;
    return s.replace('\n', ' ').replace("\\u", "u");
  }
}
