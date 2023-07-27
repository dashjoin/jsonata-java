package com.dashjoin.jsonata;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.dashjoin.jsonata.Jsonata.Fn2;
import com.dashjoin.jsonata.Jsonata.FnVarArgs;
import com.dashjoin.jsonata.Jsonata.JLambda;
import com.dashjoin.jsonata.Utils.JList;

//import com.fasterxml.jackson.databind.ObjectMapper;

public class Main {


    Number sub(Number a, Number bbb) {
        return a.doubleValue()-bbb.doubleValue();
    }

    String doubleMe(String s) {
        return s+s;
    }

    Number cos(Number n) {
        return Math.cos(n.doubleValue());
    }

    public static void main(String[] args) throws Throwable {
        //String s = "$doubleStr('hello')";
        //String s = "$cosSquare(1)";
        String s = "$ext2b(1)";
        //s = "$obj2({'a':42, 'b':$},true)";
        //s = "$sum([1,2])";
        if (args.length>0) s=args[0];
        //s = "$ = [1..4]";
        Jsonata jsonata = Jsonata.jsonata(s);
        //jsonata.registerFunction();
        Jsonata.Frame bindings = jsonata.createFrame();
        //Jsonata.Frame bindings = new Jsonata.Frame(null);
        Main main = new Main();
        bindings.bind("ext1", (Fn2<Number,Number,Number>) (a,b)-> { return  a.doubleValue()+b.doubleValue(); } );
        bindings.bind("ext1b", (Number a, Number b)-> { return  a.doubleValue()+b.doubleValue(); } );
        bindings.bind("ext2", main::sub );
        bindings.bind("ext2b", Jsonata.function("ext2b", main::sub, "<nn:n>") );

        bindings.bind("doubleStr", main::doubleMe);
        bindings.bind("mycos", main::cos);
        bindings.bind("cosSquare", (Number n)-> Math.pow(Math.cos(n.doubleValue()), 2.0) );
        bindings.bind("PI", Math.PI);
        bindings.bind("doit", (List<?> a, List<?> b)-> a.size()+b.size());
        
        bindings.bind("doit2", Jsonata.function("doit2", (List<?> a, List<?> b)-> a.size()+(b!=null ? b.size() : 0), "<xx?:n>"));

        bindings.bind("doit3", Jsonata.function("doit3", (FnVarArgs<Number>) (params) -> { System.out.println(params); return params.size(); }, null));

        bindings.bind("obj1", (Map<?,?> a) -> Functions.string(a, false) );
        bindings.bind("obj2", (Fn2<Object,Boolean,String>)Functions::string );


        //bindings.bind("ext2", (FnVar) (params)-> { return ((Number)params[0]).doubleValue()+((Number)params[1]).doubleValue(); } );
        Object result = jsonata.evaluate(List.of(1,2,3,4), bindings);
        System.out.println("Result = "+Functions.string(result, false));    
    }
}
