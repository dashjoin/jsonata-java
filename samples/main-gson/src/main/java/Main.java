import java.util.Map; import java.util.List;
import static com.dashjoin.jsonata.Jsonata.jsonata;
import com.google.gson.Gson;

public class Main {

    /**
     * Interoperability with GSON library.
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
        
        Gson gson = new Gson();
        var data = gson.fromJson(json, Object.class); 

        var expression = jsonata("$sum(example.value)");
        var result = expression.evaluate(data);  // returns 24
        System.out.println(result);
    }
}
