import java.util.Map; import java.util.List;
import static com.dashjoin.jsonata.Jsonata.jsonata;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {

    /**
     * Interoperability with Jackson Databind library.
     */
    public static void main(String[] args) throws Throwable {

        String json = "{\n" + //
                "  \"example\":   [\n" + //
                "    {\n" + //
                "      \"value\": 4\n" + //
                "    },\n" + //
                "    {\n" + //
                "      \"value\": 7\n" + //
                "    },\n" + //
                "    {\n" + //
                "      \"value\": 13\n" + //
                "    }\n" + //
                "  ]\n" + //
                "}";

        var data = new ObjectMapper().readValue(json, Object.class);
        
        var expression = jsonata("$sum(example.value)");
        var result = expression.evaluate(data);  // returns 24
        System.out.println(result);
    }
}
