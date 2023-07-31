import java.util.Map; import java.util.List;
import static com.dashjoin.jsonata.Jsonata.jsonata;
import com.dashjoin.jsonata.Functions;
import com.dashjoin.jsonata.json.Json;

public class MainJson {

    /**
     * Built-in JSON parser usage.
     */
    public static void main(String[] args) {

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

        var data = Json.parseJson(json);

        System.out.println(Functions.string(data,true));
        
        var expression = jsonata("$sum(example.value)");
        var result = expression.evaluate(data);  // returns 24
        System.out.println(result);
    }
}
