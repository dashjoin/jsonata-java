import java.util.Map; import java.util.List;
import static com.dashjoin.jsonata.Jsonata.jsonata;

public class Main {

    /**
     * Feed JSON in internal representation format.
     */
    public static void main(String[] args) {

        var data = Map.of("example",
            List.of(
                Map.of("value", 4),
                Map.of("value", 7),
                Map.of("value", 13)
            )
        );
        
        var expression = jsonata("$sum(example.value)");
        var result = expression.evaluate(data);  // returns 24
        System.out.println(result);
    }
}
