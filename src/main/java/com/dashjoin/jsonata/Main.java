package com.dashjoin.jsonata;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {
    public static void main(String[] args) throws Throwable {

        String s = "$join(['a','b','c'], '#')";
        //s = "$count([1..(1e4-1)])";
        //s = "{ 'number': [1..10].$string() }"; // FIXME
        //s = "[1..10].($ * $).$sum()";
        // s = "($a := [1..10].($ * $); $sum($a) )";
        // s = "$substringBefore(\"Alalala\", \"la\")";
        // s = "$substring(\"Alalala\", 1,4)";
        // s = "$pad('xxx', -5, 'abrac')";
        // // group matches:
        // s = "$match(\"Alalalcl\", /l(a|c)/)";
        // s = "$match(\"Alalalcl\", /l(a|c)(l|x)/)";

        // s = "[0,1,2].$boolean()";

        s = "$replace('abcdef', /c.*/, 'xy')";
        s = "$replace('mad hatter', /hat/i, function($match) { 'foo' })";
        // s = "$split('abcdef', /c./)";
        // s = "$split('abcdef', 'cd')";

        //s = "[1,2,3,4].($*$) ~> $sum";
        //s = "$string({'a':[1,2]}, true)";
        //s = "$number('3.1e1')";

        //s = "$map(['11','1e5','0.00001'], $number ~> $sqrt )";

        //s = "(  $data := {    \"one\": [1,2,3,4,5],    \"two\": [5,4,3,2,1]  };  $add := function($x){$x*$x};  $map($data.two, $add) ~> $sum )  ";
        //s = "(  $data := {    \"one\": [1,2,3,4,5],    \"two\": [5,4,3,2,1]  };  $add := function($x){$x*$x};  $data.one )  ";

        //s = "$zip([1,2], [3,4])";
        //s = "$zip([1,2], [3,4,5], [-1])";

        //s = "$keys([{'a':true},{'b':true}])";

        //s = "$each({'a':1, 'b':2}, $string)";

        //s = "$formatBase(100*$random(), 2)";

        //s = "$clone({'a':[1.0,2,3.5]}).a";

        //s = "$eval('[1,2].$string()')";
        //s = "false > 5";
        //s = "10e300 * 10e100";
        s="{'a':()}";
        Jsonata jsonata = new Jsonata(s, false);
        Object result = jsonata.evaluate(null, null);
        System.out.println("Result = "+new ObjectMapper().writeValueAsString(result));    
    }
}
