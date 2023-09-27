/**
 * jsonata-java is the JSONata Java reference port
 * 
 * Copyright Dashjoin GmbH. https://dashjoin.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Derived from Javascript code under this license:
/**
 * © Copyright IBM Corp. 2016, 2018 All Rights Reserved
 *   Project name: JSONata
 *   This project is licensed under the MIT License, see LICENSE
 */
package com.dashjoin.jsonata;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.dashjoin.jsonata.Jsonata.Frame;
import com.dashjoin.jsonata.Tokenizer.Token;
import com.dashjoin.jsonata.utils.Signature;

//var parseSignature = require('./signature');
@SuppressWarnings({"unchecked"})
public class Parser {

    boolean dbg = false;
    
    // This parser implements the 'Top down operator precedence' algorithm developed by Vaughan R Pratt; http://dl.acm.org/citation.cfm?id=512931.
    // and builds on the Javascript framework described by Douglas Crockford at http://javascript.crockford.com/tdop/tdop.html
    // and in 'Beautiful Code', edited by Andy Oram and Greg Wilson, Copyright 2007 O'Reilly Media, Inc. 798-0-596-51004-6

    String source;
    boolean recover;

    //var parser = function (source, recover) {
        Symbol node;
        Tokenizer lexer;

        HashMap<String, Symbol> symbolTable = new HashMap<>();
        List<Exception> errors = new ArrayList<>();

        List<Token> remainingTokens() {
            List<Token> remaining = new ArrayList<>();
            if (!node.id.equals("(end)")) {
                Token t = new Token();
                t.type = node.type; t.value = node.value; t.position = node.position;
                remaining.add(t);
            }
            Token nxt = lexer.next(false);
            while (nxt != null) {
                remaining.add(nxt);
                nxt = lexer.next(false);
            }
            return remaining;
        };


    public static<T> T clone(T object) {
        try {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bOut);
            out.writeObject(object);
            out.close();
    
            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bOut.toByteArray()));
            T copy = (T)in.readObject();
            in.close();
    
            return copy;
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    class Symbol implements Cloneable {

        //Symbol s;
        String id;
        String type;
        Object value;
        int bp;
        int lbp;

        int position;

        boolean keepArray; // [

        boolean descending; // ^
        Symbol expression; // ^
        public List<Symbol> seekingParent;
        public List<Exception> errors;

        List<Symbol> steps;
        Symbol slot;
        Symbol nextFunction;
        public boolean keepSingletonArray;
        public boolean consarray;
        public int level;
        public Object focus;
        public Object token;
        public boolean thunk;

        // Procedure:
        Symbol procedure;
        List<Symbol> arguments;
        Symbol body;
        List<Symbol> predicate;
        public List<Symbol> stages;
        public Object input;
        public Frame environment;
        public Object tuple;
        public Object expr;
        public Symbol group;
        public Object name;

        // Infix attributes
        Symbol lhs, rhs;
        // where rhs = list of Symbol pairs
        public List<Symbol[]> lhsObject, rhsObject;
        // where rhs = list of Symbols
        List<Symbol> rhsTerms;
        List<Symbol> terms;

        // Ternary operator:
        Symbol condition, then, _else;

        List<Symbol> expressions;

        // processAST error handling
        public JException error;
        public Object signature;

        // Prefix attributes
        Symbol pattern, update, delete;

        // Ancestor attributes
        public String label;
        public Object index;
        public boolean _jsonata_lambda;
        public Symbol ancestor;


        Symbol nud() {
            // error - symbol has been invoked as a unary operator
            final JException _err = new JException("S0211", position, value);

            if (recover) {
                /*
                err.remaining = remainingTokens();
                err.type = "error";
                errors.add(err);
                return err;
                */
                return new Symbol("(error)") {
                    //JException err = _err;
                };
            } else {
                throw _err;
            }
        }

        Symbol led(Symbol left) {
            throw new Error("led not implemented");
        }

        //class Symbol {
        Symbol() {}
        Symbol(String id) { this(id, 0); }
        Symbol(String id, int bp) {
            this.id = id; this.value = id;
            this.bp = bp;
/* use register(Symbol) ! Otherwise inheritance doesn't work
            Symbol s = symbolTable.get(id);
            //bp = bp != 0 ? bp : 0;
            if (s != null) {
                if (bp >= s.lbp) {
                    s.lbp = bp;
                }
            } else {
                s = new Symbol();
                s.value = s.id = id;
                s.lbp = bp;
                symbolTable.put(id, s);
            }

*/
            //return s;
        }

        public Symbol create() {
            // We want a shallow clone (do not duplicate outer class!)
            try {
                Symbol cl = (Symbol) this.clone();
                //System.err.println("cloning "+this+" clone="+cl);
                return cl;
            } catch (CloneNotSupportedException e) {
                // never reached
                if (dbg) e.printStackTrace();
                return null;
            }
        }

        @Override public String toString() {
            return this.getClass().getSimpleName()+" "+id+" value="+value;
        }
    }

    void register(Symbol t) {

        //if (t instanceof Infix || t instanceof InfixR) return;

        Symbol s = symbolTable.get(t.id);
        if (s != null) {
            if (dbg) System.out.println("Symbol in table "+t.id+" "+s.getClass().getName()+" -> "+ t.getClass().getName());
            //symbolTable.put(t.id, t);
            if (t.bp >= s.lbp) {
                if (dbg) System.out.println("Symbol in table "+t.id+" lbp="+s.lbp+" -> "+t.bp);
                s.lbp = t.bp;
            }
        } else {
            s = (Symbol) t.create();
            s.value = s.id = t.id;
            s.lbp = t.bp;
            symbolTable.put(t.id, s);
        }
    }
        
        public Symbol handleError(JException err) {
            if (recover) {
                err.remaining = remainingTokens();
                errors.add(err);
                //Symbol symbol = symbolTable.get("(error)");
                Symbol node = new Symbol();
                // FIXME node.error = err;
                //node.type = "(error)";
                return node;
            } else {
                throw err;
            }
        }
        //}

        Symbol advance() { return advance(null); }
        Symbol advance(String id) { return advance(id, false); }
        Symbol advance(String id, boolean infix) {
            if (id!=null && !node.id.equals(id)) {
                String code;
                if (node.id.equals("(end)")) {
                    // unexpected end of buffer
                    code = "S0203";
                } else {
                    code = "S0202";
                }
                JException err = new JException(
                    code,
                    node.position,
                    node.value,
                    id
                );
                return handleError(err);
            }
            Token next_token = lexer.next(infix);
            if (dbg) System.out.println("nextToken "+(next_token!=null ? next_token.type : null));
            if (next_token == null) {
                node = symbolTable.get("(end)");
                node.position = source.length();
                return node;
            }
            Object value = next_token.value;
            String type = next_token.type;
            Symbol symbol;
            switch (type) {
                case "name":
                case "variable":
                    symbol = symbolTable.get("(name)");
                    break;
                case "operator":
                    symbol = symbolTable.get(""+value);
                    if (symbol==null) {
                        return handleError(new JException(
                            "S0204", next_token.position, value));
                    }
                    break;
                case "string":
                case "number":
                case "value":
                    symbol = symbolTable.get("(literal)");
                    break;
                case "regex":
                    type = "regex";
                    symbol = symbolTable.get("(regex)");
                    break;
                /* istanbul ignore next */
                default:
                    return handleError(new JException(
                        "S0205", next_token.position, value));
            }

            node = symbol.create();
            //Token node = new Token(); //Object.create(symbol);
            node.value = value;
            node.type = type;
            node.position = next_token.position;
            if (dbg) System.out.println("advance "+node);
            return node;
        }

        // Pratt's algorithm
        Symbol expression(int rbp) {
            Symbol left;
            Symbol t = node;
            advance(null, true);
            left = t.nud();
            while (rbp < node.lbp) {
                t = node;
                advance(null, false);
                if (dbg) System.out.println("t="+t+", left="+left.type);
                left = t.led(left);
            }
            return left;
        };

        class Terminal extends Symbol {
            Terminal(String id) {
                super(id, 0);
            }
            @Override Symbol nud() {
                return this;
            }
        }

        /*
            var terminal = function (id) {
            var s = symbol(id, 0);
            s.nud = function () {
                return this;
            };
        };
        */

        // match infix operators
        // <expression> <operator> <expression>
        // left associative
        class Infix extends Symbol {

            Infix(String id) { this(id,0); }
            Infix(String id, int bp) {
                super(id, bp!=0 ? bp : (id!=null ? Tokenizer.operators.get(id) : 0));
            }

            @Override
            Symbol led(Symbol left) {
                lhs = left; rhs = expression(bp);
                type = "binary";
                return this;
            }
        }


        class InfixAndPrefix extends Infix {

            Prefix prefix;

            InfixAndPrefix(String id) { this(id,0); }
            InfixAndPrefix(String id, int bp) {
                super(id, bp);
                prefix = new Prefix(id);
            }

            @Override Symbol nud() {
                return prefix.nud();
                // expression(70);
                // type="unary";
                // return this;
            }

            @Override public Object clone() throws CloneNotSupportedException {
                Object c = super.clone();
                // IMPORTANT: make sure to allocate a new Prefix!!!
                ((InfixAndPrefix)c).prefix = new Prefix(((InfixAndPrefix)c).id);
                return c;
            }
        }

        // match infix operators
        // <expression> <operator> <expression>
        // right associative
        class InfixR extends Symbol {

            InfixR(String id, int bp) {
                super(id, bp);
            }

            //abstract Object led();
        }

        // match prefix operators
        // <operator> <expression>
        class Prefix extends Symbol {
            //public List<Symbol[]> lhs;

            Prefix(String id) {
                super(id);
                //type = "unary";
            }

            //Symbol _expression;

            @Override
            Symbol nud() {
                expression = expression(70);
                type = "unary";
                return this;
            }
        }

    public Parser() {

        register(new Terminal("(end)"));
        register(new Terminal("(name)"));
        register(new Terminal("(literal)"));
        register(new Terminal("(regex)"));
        register(new Symbol(":"));
        register(new Symbol(";"));
        register(new Symbol(","));
        register(new Symbol(")"));
        register(new Symbol("]"));
        register(new Symbol("}"));
        register(new Symbol("..")); // range operator
        register(new Infix(".")); // map operator
        register(new Infix("+")); // numeric addition
        register(new InfixAndPrefix("-")); // numeric subtraction
        // unary numeric negation

        register(new Infix("*") {
            // field wildcard (single level)
            @Override Symbol nud() {
                type = "wildcard";
                return this;
            }
        }); // numeric multiplication
        register(new Infix("/")); // numeric division
        register(new Infix("%") {
            // parent operator
            @Override Symbol nud() {
                type = "parent";
                return this;
            }
        }); // numeric modulus
        register(new Infix("=")); // equality
        register(new Infix("<")); // less than
        register(new Infix(">")); // greater than
        register(new Infix("!=")); // not equal to
        register(new Infix("<=")); // less than or equal
        register(new Infix(">=")); // greater than or equal
        register(new Infix("&")); // string concatenation

        register(new Infix("and") {
            // allow as terminal
            @Override Symbol nud() { return this; }
        }); // Boolean AND
        register(new Infix("or") {
            // allow as terminal
            @Override Symbol nud() { return this; }
        }); // Boolean OR
        register(new Infix("in") {
            // allow as terminal
            @Override Symbol nud() { return this; }
        }); // is member of array
        // merged Infix: register(new Terminal("and")); // the 'keywords' can also be used as terminals (field names)
        // merged Infix: register(new Terminal("or")); //
        // merged Infix: register(new Terminal("in")); //
        // merged Infix: register(new Prefix("-")); // unary numeric negation
        register(new Infix("~>")); // function application

        register(new InfixR("(error)", 10) {
            @Override
            Symbol led(Symbol left) {
                throw new UnsupportedOperationException("TODO", null);
            }
        });

        // field wildcard (single level)
        // merged with Infix *
        // register(new Prefix("*") {
        //     @Override Symbol nud() {
        //         type = "wildcard";
        //         return this;
        //     }
        // });

        // descendant wildcard (multi-level)

        register(new Prefix("**") {
            @Override Symbol nud() {
                type = "descendant";
                return this;
            }
        });

        // parent operator
        // merged with Infix %
        // register(new Prefix("%") {
        //     @Override Symbol nud() {
        //         type = "parent";
        //         return this;
        //     }
        // });

        // function invocation
        register(new Infix("(", Tokenizer.operators.get("(")) {

            @Override Symbol led(Symbol left) {
            // left is is what we are trying to invoke
            this.procedure = left;
            this.type = "function";
            this.arguments = new ArrayList<>();
            if (!node.id.equals(")")) {
                for (; ;) {
                    if (node.type.equals("operator") && node.id.equals("?")) {
                        // partial function application
                        this.type = "partial";
                        this.arguments.add(node);
                        advance("?");
                    } else {
                        this.arguments.add(expression(0));
                    }
                    if (!node.id.equals(",")) break;
                    advance(",");
                }
            }
            advance(")", true);
            // if the name of the function is 'function' or λ, then this is function definition (lambda function)
            if (left.type.equals("name") && (left.value.equals("function") || left.value.equals("\u03BB"))) {
                // all of the args must be VARIABLE tokens
                //int index = 0;
                for (Symbol arg : arguments) {
                //this.arguments.forEach(function (arg, index) {
                    if (!arg.type.equals("variable")) {
                        return handleError(new JException("S0208",
                            arg.position,
                            arg.value//,
                            //index + 1
                        )
                        );
                    }
                    //index++;
                }
                this.type = "lambda";
                // is the next token a '<' - if so, parse the function signature
                if (node.id.equals("<")) {
                    int depth = 1;
                    String sig = "<";
                    while (depth > 0 && !node.id.equals("{") && !node.id.equals("(end)")) {
                        Symbol tok = advance();
                        if (tok.id.equals(">")) {
                            depth--;
                        } else if (tok.id.equals("<")) {
                            depth++;
                        }
                        sig += tok.value;
                    }
                    advance(">");
                    this.signature = new Signature(sig, "lambda");
                }
                // parse the function body
                advance("{");
                this.body = expression(0);
                advance("}");
            }
            return this;
            }
        //});

        // parenthesis - block expression
        // Note: in Java both nud and led are in same class!
        //register(new Prefix("(") {

            @Override Symbol nud() {
                if (dbg) System.out.println("Prefix (");
                List<Symbol> expressions = new ArrayList<>();
                while (!node.id.equals(")")) {
                    expressions.add(Parser.this.expression(0));
                    if (!node.id.equals(";")) {
                        break;
                    }
                    advance(";");
                }
                advance(")", true);
                this.type = "block";
                this.expressions = expressions;
                return this;    
            }
        });

        // array constructor

        // merged: register(new Prefix("[") {        
        register(new Infix("[", Tokenizer.operators.get("[")) {

            @Override Symbol nud() {
                List<Symbol> a = new ArrayList<>();
                if (!node.id.equals("]")) {
                    for (; ;) {
                        var item = Parser.this.expression(0);
                        if (node.id.equals("..")) {
                            // range operator
                            var range = new Symbol();
                            range.type = "binary"; range.value = ".."; range.position = node.position; range.lhs = item;
                            advance("..");
                            range.rhs = expression(0);
                            item = range;
                        }
                        a.add(item);
                        if (!node.id.equals(",")) {
                            break;
                        }
                        advance(",");
                    }
                }
                advance("]", true);
                this.expressions = a;
                this.type = "unary";
                return this;   
            }
        //});

        // filter - predicate or array index
        //register(new Infix("[", Tokenizer.operators.get("[")) {

            @Override Symbol led(Symbol left) {
                if (node.id.equals("]")) {
                    // empty predicate means maintain singleton arrays in the output
                    var step = left;
                    while (step!=null && step.type.equals("binary") && step.value.equals("[")) {
                        step = ((Infix)step).lhs;
                    }
                    step.keepArray = true;
                    advance("]");
                    return left;
                } else {
                    this.lhs = left;
                    this.rhs = expression(Tokenizer.operators.get("]"));
                    this.type = "binary";
                    advance("]", true);
                    return this;
                }
                }
        });

        // order-by
        register(new Infix("^", Tokenizer.operators.get("^")) {

            @Override Symbol led(Symbol left) {
                advance("(");
                List<Symbol> terms = new ArrayList<>();
                for (; ;) {
                    final Symbol term = new Symbol();
                    term.descending = false;

                    if (node.id.equals("<")) {
                        // ascending sort
                        advance("<");
                    } else if (node.id.equals(">")) {
                        // descending sort
                        term.descending = true;
                        advance(">");
                    } else {
                        //unspecified - default to ascending
                    }
                    term.expression = Parser.this.expression(0);
                    terms.add(term);
                    if (!node.id.equals(",")) {
                        break;
                    }
                    advance(",");
                }
                advance(")");
                this.lhs = left;
                this.rhsTerms = terms;
                this.type = "binary";
                return this;
            }
        });

        register(new Infix("{", Tokenizer.operators.get("{")) {

        // merged register(new Prefix("{") {

            @Override Symbol nud() {
                return objectParser(null);
            }
        // });

        // register(new Infix("{", Tokenizer.operators.get("{")) {

            @Override Symbol led(Symbol left) {
                return objectParser(left);
            }
        });

        // bind variable
        register(new InfixR(":=", Tokenizer.operators.get(":=")) {

            @Override Symbol led(Symbol left) {
                if (!left.type.equals("variable")) {
                    return handleError(new JException(
                        "S0212",
                        left.position,
                        left.value
                    ));
                }
                this.lhs = left;
                this.rhs = expression(Tokenizer.operators.get(":=") - 1); // subtract 1 from bindingPower for right associative operators
                this.type = "binary";
                return this;    
            }
        });

        // focus variable bind
        register(new Infix("@", Tokenizer.operators.get("@")) {

            @Override Symbol led(Symbol left) {
                this.lhs = left;
                this.rhs = expression(Tokenizer.operators.get("@"));
                if(!this.rhs.type.equals("variable")) {
                    return handleError(new JException("S0214",
                        this.rhs.position,
                        "@"
                    ));
                }
                this.type = "binary";
                return this;
            }
        });

        // index (position) variable bind
        register(new Infix("#", Tokenizer.operators.get("#")) {

            @Override Symbol led(Symbol left) {
                this.lhs = left;
                this.rhs = expression(Tokenizer.operators.get("#"));
                if(!this.rhs.type.equals("variable")) {
                    return handleError(new JException("S0214",
                        this.rhs.position,
                        "#"
                    ));
                }
                this.type = "binary";
                return this;
            }
        });

        // if/then/else ternary operator ?:
        register(new Infix("?", Tokenizer.operators.get("?")) {

            @Override Symbol led(Symbol left) {
                this.type = "condition";
                this.condition = left;
                this.then = expression(0);
                if (node.id.equals(":")) {
                    // else condition
                    advance(":");
                    this._else = expression(0);
                }
                return this;
            }
        });

        // object transformer
        register(new Prefix("|") {

            @Override Symbol nud() {
                this.type = "transform";
                this.pattern = Parser.this.expression(0);
                advance("|");
                this.update = Parser.this.expression(0);
                if (node.id.equals(",")) {
                    advance(",");
                    this.delete = Parser.this.expression(0);
                }
                advance("|");
                return this;
                }
        });
    }

    // tail call optimization
    // this is invoked by the post parser to analyse lambda functions to see
    // if they make a tail call.  If so, it is replaced by a thunk which will
    // be invoked by the trampoline loop during function application.
    // This enables tail-recursive functions to be written without growing the stack
    Symbol tailCallOptimize(Symbol expr) {
        Symbol result;
        if (expr.type.equals("function") && expr.predicate==null) {
            var thunk = new Symbol(); thunk.type = "lambda"; thunk.thunk = true; thunk.arguments = List.of(); thunk.position = expr.position;
            thunk.body = expr;
            result = thunk;
        } else if (expr.type.equals("condition")) {
            // analyse both branches
            expr.then = tailCallOptimize(expr.then);
            if (expr._else != null) {
                expr._else = tailCallOptimize(expr._else);
            }
            result = expr;
        } else if (expr.type.equals("block")) {
            // only the last expression in the block
            var length = expr.expressions.size();
            if (length > 0) {
                if (!(expr.expressions instanceof ArrayList))
                     expr.expressions = new ArrayList<>(expr.expressions);
                expr.expressions.set(length - 1, tailCallOptimize(expr.expressions.get(length - 1)));
            }
            result = expr;
        } else {
            result = expr;
        }
        return result;
    }

    int ancestorLabel = 0;
    int ancestorIndex = 0;
    List<Symbol> ancestry = new ArrayList<>();

    Symbol seekParent(Symbol node, Symbol slot) {
        switch (node.type) {
            case "name":
            case "wildcard":
                slot.level--;
                if(slot.level == 0) {
                    if (node.ancestor == null) {
                        node.ancestor = slot;
                    } else {
                        // reuse the existing label
                        ancestry.get((int)slot.index).slot.label = node.ancestor.label;
                        node.ancestor = slot;
                    }
                    node.tuple = true;
                }
                break;
            case "parent":
                slot.level++;
                break;
            case "block":
                // look in last expression in the block
                if(node.expressions.size() > 0) {
                    node.tuple = true;
                    slot = seekParent(node.expressions.get(node.expressions.size() - 1), slot);
                }
                break;
            case "path":
                // last step in path
                node.tuple = true;
                var index = node.steps.size() - 1;
                slot = seekParent(node.steps.get(index--), slot);
                while (slot.level > 0 && index >= 0) {
                    // check previous steps
                    slot = seekParent(node.steps.get(index--), slot);
                }
                break;
            default:
                // error - can't derive ancestor
                throw new JException("S0217",
                    node.position,
                    node.type
                );
        }
        return slot;
    };


    void pushAncestry(Symbol result, Symbol value) {
        if (value==null) return; // Added NPE check
        if (value.seekingParent!=null || value.type.equals("parent")) {
            List<Symbol> slots = (value.seekingParent != null) ? value.seekingParent : new ArrayList<>();
            if (value.type.equals("parent")) {
                slots.add(value.slot);
            }
            if (result.seekingParent==null) {
                result.seekingParent = slots;
            } else {
                result.seekingParent.addAll(slots);
            }
        }
    }

    void resolveAncestry(Symbol path) {
        var index = path.steps.size() - 1;
        var laststep = path.steps.get(index);
        var slots = (laststep.seekingParent != null) ? laststep.seekingParent : new ArrayList<Symbol>();
        if (laststep.type.equals("parent")) {
            slots.add(laststep.slot);
        }
        for(var is = 0; is < slots.size(); is++) {
            var slot = slots.get(is);
            index = path.steps.size() - 2;
            while (slot.level > 0) {
                if (index < 0) {
                    if(path.seekingParent == null) {
                        path.seekingParent = new ArrayList<>(Arrays.asList(slot));
                    } else {
                        path.seekingParent.add(slot);
                    }
                    break;
                }
                // try previous step
                var step = path.steps.get(index--);
                // multiple contiguous steps that bind the focus should be skipped
                while(index >= 0 && step.focus!=null && path.steps.get(index).focus!=null) {
                    step = path.steps.get(index--);
                }
                slot = seekParent(step, slot);
            }
        }
    }

    // post-parse stage
    // the purpose of this is to add as much semantic value to the parse tree as possible
    // in order to simplify the work of the evaluator.
    // This includes flattening the parts of the AST representing location paths,
    // converting them to arrays of steps which in turn may contain arrays of predicates.
    // following this, nodes containing '.' and '[' should be eliminated from the AST.
    Symbol processAST(Symbol expr) {
        Symbol result = expr;
        if (expr==null) return null;
        if (dbg) System.out.println(" > processAST type="+expr.type+" value='"+expr.value+"'");
        switch (expr.type != null ? expr.type : "(null)") {
            case "binary": {
                switch (""+expr.value) {
                    case ".":
                        var lstep = processAST(((Infix)expr).lhs);

                        if (lstep.type.equals("path")) {
                            result = lstep;
                        } else {
                            result = new Infix(null);
                            result.type = "path";
                            result.steps = new ArrayList<>(Arrays.asList(lstep));
                            //result = {type: 'path', steps: [lstep]};
                        }
                        if(lstep.type.equals("parent")) {
                            result.seekingParent = new ArrayList<>(Arrays.asList(lstep.slot));
                        }
                        var rest = processAST(((Infix)expr).rhs);
                        if (rest.type.equals("function") &&
                            rest.procedure.type.equals("path") &&
                            rest.procedure.steps.size() == 1 &&
                            rest.procedure.steps.get(0).type.equals("name") &&
                            result.steps.get(result.steps.size() - 1).type.equals("function")) {
                            // next function in chain of functions - will override a thenable
                            result.steps.get(result.steps.size() - 1).nextFunction = (Symbol)rest.procedure.steps.get(0).value;
                        }
                        if (rest.type.equals("path")) {
                            result.steps.addAll(rest.steps);
                        } else {
                            if(rest.predicate != null) {
                                rest.stages = rest.predicate;
                                rest.predicate = null;
                                //delete rest.predicate;
                            }
                            result.steps.add(rest);
                        }
                        // any steps within a path that are string literals, should be changed to 'name'
                        for (var step : result.steps) {
                            if (step.type.equals("number") || step.type.equals("value")) {
                                // don't allow steps to be numbers or the values true/false/null
                                throw new JException("S0213",
                                    step.position,
                                    step.value
                                );
                            }
                            //System.out.println("step "+step+" type="+step.type);
                            if (step.type.equals("string"))
                                step.type = "name";
                                // for (var lit : step.steps) {
                                //     System.out.println("step2 "+lit+" type="+lit.type);
                                //     lit.type = "name";
                                // }
                        }
                                
                        // any step that signals keeping a singleton array, should be flagged on the path
                        if (result.steps.stream().filter(step ->
                            step.keepArray == true
                        ).count() > 0) {
                            result.keepSingletonArray = true;
                        }
                        // if first step is a path constructor, flag it for special handling
                        var firststep = result.steps.get(0);
                        if (firststep.type.equals("unary") && (""+firststep.value).equals("[")) {
                            firststep.consarray = true;
                        }
                        // if the last step is an array constructor, flag it so it doesn't flatten
                        var laststep = result.steps.get(result.steps.size() - 1);
                        if (laststep.type.equals("unary") && (""+laststep.value).equals("[")) {
                            laststep.consarray = true;
                        }
                        resolveAncestry(result);
                        break;
                    case "[":
                            if (dbg) System.out.println("binary [");
                            // predicated step
                            // LHS is a step or a predicated step
                            // RHS is the predicate expr
                            result = processAST(((Infix)expr).lhs);
                            var step = result;
                            var type = "predicate";
                            if (result.type.equals("path")) {
                                step = result.steps.get(result.steps.size() - 1);
                                type = "stages";
                            }
                            if (step.group != null) {
                                throw new JException(
                                    "S0209",
                                    //stack: (new Error()).stack,
                                    expr.position
                                );
                            }
                            // if (typeof step[type] === 'undefined') {
                            //     step[type] = [];
                            // }
                            if (type.equals("stages")) {
                                if (step.stages==null)
                                    step.stages = new ArrayList<>();
                            } else {
                                if (step.predicate==null)
                                    step.predicate = new ArrayList<>();
                            }

                            var predicate = processAST(((Infix)expr).rhs);
                            if(predicate.seekingParent != null) {
                                final var _step = step;
                                predicate.seekingParent.forEach(slot -> {
                                    if(slot.level == 1) {
                                        seekParent(_step, slot);
                                    } else {
                                        slot.level--;
                                    }
                                });
                                pushAncestry(step, predicate);
                            }
                            Symbol s = new Symbol();
                            s.type = "filter"; s.expr = predicate; s.position = expr.position;

                            // FIXED:
                            // this logic is required in Java to fix
                            // for example test: flattening case 045
                            // otherwise we lose the keepArray flag
                            if (expr.keepArray)
                                step.keepArray = true;

                            if (type.equals("stages"))
                                step.stages.add(s);
                            else
                                step.predicate.add(s);
                            //step[type].push({type: 'filter', expr: predicate, position: expr.position});
                            break;
                    case "{":
                            // group-by
                            // LHS is a step or a predicated step
                            // RHS is the object constructor expr
                            result = processAST(expr.lhs);
                            if (result.group != null) {
                                throw new JException("S0210",
                                    //stack: (new Error()).stack,
                                    expr.position
                                );
                            }
                            // object constructor - process each pair
                            result.group = new Symbol();
                            result.group.lhsObject = expr.rhsObject.stream().map(pair -> {
                                    return new Symbol[] {processAST(pair[0]), processAST(pair[1])};
                                }).collect(Collectors.toList());
                            result.group.position = expr.position;
                        break;
                    
                    case "^":
                        // order-by
                        // LHS is the array to be ordered
                        // RHS defines the terms
                        result = processAST(expr.lhs);
                        if (!result.type.equals("path")) {
                            Symbol _res = new Symbol();
                            _res.type = "path"; _res.steps = new ArrayList<>(); _res.steps.add(result);
                            result = _res;
                        }
                        var sortStep = new Symbol(); sortStep.type = "sort"; sortStep.position = expr.position;                        
                        sortStep.terms = expr.rhsTerms.stream().map(terms -> {
                            Symbol expression = processAST(terms.expression);
                            pushAncestry(sortStep, expression);
                            Symbol res = new Symbol();
                            res.descending = terms.descending;
                            res.expression = expression;
                            return res;
                        }).collect(Collectors.toList());
                        result.steps.add(sortStep);
                        resolveAncestry(result);
                        break;
                    case ":=":
                        result = new Symbol();
                        result.type = "bind"; result.value = expr.value; result.position = expr.position;
                        result.lhs = processAST(expr.lhs);
                        result.rhs = processAST(expr.rhs);
                        pushAncestry(result, result.rhs);
                        break;
                    case "@":
                        result = processAST(expr.lhs);
                        step = result;
                        if (result.type.equals("path")) {
                            step = result.steps.get(result.steps.size() - 1);
                        }
                        // throw error if there are any predicates defined at this point
                        // at this point the only type of stages can be predicates
                        if(step.stages != null || step.predicate != null) {
                            throw new JException("S0215",
                                //stack: (new Error()).stack,
                                expr.position
                            );
                        }
                        // also throw if this is applied after an 'order-by' clause
                        if(step.type.equals("sort")) {
                            throw new JException("S0216",
                                //stack: (new Error()).stack,
                                expr.position
                            );
                        }
                        if(expr.keepArray) {
                            step.keepArray = true;
                        }
                        step.focus = expr.rhs.value;
                        step.tuple = true;
                        break;
                    case "#":
                        result = processAST(expr.lhs);
                        step = result;
                        if (result.type.equals("path")) {
                            step = result.steps.get(result.steps.size() - 1);
                        } else {
                            Symbol _res = new Symbol();
                            _res.type = "path"; _res.steps = new ArrayList<>(); _res.steps.add(result);
                            result = _res;
                            if (step.predicate != null) {
                                step.stages = step.predicate;
                                step.predicate = null;
                            }
                        }
                        if (step.stages == null) {
                            step.index = expr.rhs.value; // name of index variable = String
                        } else {
                            Symbol _res = new Symbol();
                            _res.type = "index"; _res.value = expr.rhs.value; _res.position = expr.position;
                            step.stages.add(_res);
                        }
                        step.tuple = true;
                        break;
                    case "~>":
                        result = new Symbol(); result.type = "apply"; result.value = expr.value; result.position = expr.position;
                        result.lhs = processAST(expr.lhs);
                        result.rhs = processAST(expr.rhs);
                        break;                    
                    default:
                        Infix _result = new Infix(null);
                        _result.type = expr.type; _result.value = expr.value; _result.position = expr.position;
                        _result.lhs = processAST((expr).lhs);
                        _result.rhs = processAST((expr).rhs);
                        pushAncestry(_result, _result.lhs);
                        pushAncestry(_result, _result.rhs);
                        result = _result;
                        break;
                }
                break; // binary
            }

            case "unary": {
                result = new Symbol();
                result.type = expr.type; result.value = expr.value; result.position = expr.position;
                // expr.value might be Character!
                String exprValue = ""+expr.value;
                if (exprValue.equals("[")) {
                    if (dbg) System.out.println("unary [ "+result);
                    // array constructor - process each item
                    final Symbol _result = result;
                    result.expressions = expr.expressions.stream().map(item -> {
                        Symbol value = processAST(item);
                        pushAncestry(_result, value);
                        return value;
                    }
                    ).collect(Collectors.toList());
                } else if (exprValue.equals("{")) {
                    // object constructor - process each pair
                    //throw new Error("processAST {} unimpl");
                    final Symbol _result = result;
                    result.lhsObject = expr.lhsObject.stream().map(pair -> {
                        Symbol key = processAST(pair[0]);
                        pushAncestry(_result, key);
                        Symbol value = processAST(pair[1]);
                        pushAncestry(_result, value);
                        return new Symbol[] {key, value};
                    }).collect(Collectors.toList());
                } else {
                    // all other unary expressions - just process the expression
                    result.expression = processAST(expr.expression);
                    // if unary minus on a number, then pre-process
                    if (exprValue.equals("-") && result.expression.type.equals("number")) {
                        result = result.expression;
                        result.value = Utils.convertNumber( -((Number)result.value).doubleValue() );
                        if (dbg) System.out.println("unary - value="+result.value);
                    } else {
                        pushAncestry(result, result.expression);
                    }
                }
                break; // unary
            }

            case "function":
            case "partial":
                result = new Symbol();
                result.type = expr.type; result.name = expr.name; result.value = expr.value; result.position = expr.position;
                final Symbol _result = result;
                result.arguments = expr.arguments.stream().map(arg -> {
                    Symbol argAST = processAST(arg);
                    pushAncestry(_result, argAST);
                    return argAST;
                }).collect(Collectors.toList());
                result.procedure = processAST(expr.procedure);
                break;
            case "lambda":
                result = new Symbol();
                result.type = expr.type;
                result.arguments = expr.arguments;
                result.signature = expr.signature;
                result.position = expr.position;
                var body = processAST(expr.body);
                result.body = tailCallOptimize(body);
                break;
            case "condition":
                result = new Symbol();
                result.type = expr.type; result.position = expr.position;
                result.condition = processAST(expr.condition);
                pushAncestry(result, result.condition);
                result.then = processAST(expr.then);
                pushAncestry(result, result.then);
                if (expr._else != null) {
                    result._else = processAST(expr._else);
                    pushAncestry(result, result._else);
                }
                break;
            case "transform":
                result = new Symbol();
                result.type = expr.type; result.position = expr.position;
                result.pattern = processAST(expr.pattern);
                result.update = processAST(expr.update);
                if (expr.delete != null) {
                    result.delete = processAST(expr.delete);
                }
                break;
            case "block":
                result = new Symbol();
                result.type = expr.type; result.position = expr.position;
                // array of expressions - process each one
                final Symbol __result = result;
                result.expressions = expr.expressions.stream().map(item -> {
                    Symbol part = processAST(item);
                    pushAncestry(__result, part);
                    if (part.consarray || (part.type.equals("path") && part.steps.get(0).consarray)) {
                        __result.consarray = true;
                    }
                    return part;
                }).collect(Collectors.toList());
                // TODO scan the array of expressions to see if any of them assign variables
                // if so, need to mark the block as one that needs to create a new frame
                break;
            case "name":
                result = new Symbol();
                result.type = "path"; result.steps = new ArrayList<>(); result.steps.add(expr);
                if (expr.keepArray) {
                    result.keepSingletonArray = true;
                }
                break;
            case "parent":
                result = new Symbol();
                result.type = "parent";
                result.slot = new Symbol();
                result.slot.label = "!"+ancestorLabel++;
                result.slot.level = 1;
                result.slot.index = ancestorIndex++;
                //slot: { label: '!' + ancestorLabel++, level: 1, index: ancestorIndex++ } };
                ancestry.add(result);
                break;
            case "string":
            case "number":
            case "value":
            case "wildcard":
            case "descendant":
            case "variable":
            case "regex":
                result = expr;
                break;
            case "operator":
                // the tokens 'and' and 'or' might have been used as a name rather than an operator
                if (expr.value.equals("and") || expr.value.equals("or") || expr.value.equals("in")) {
                    expr.type = "name";
                    result = processAST(expr);
                } else /* istanbul ignore else */ if ((""+expr.value).equals("?")) {
                    // partial application
                    result = expr;
                } else {
                    throw new JException("S0201",
                        //stack: (new Error()).stack,
                        expr.position,
                        expr.value
                    );
                }
                break;
            case "error":
                result = expr;
                if (expr.lhs!=null) {
                    result = processAST(expr.lhs);
                }
                break;
            default:
                var code = "S0206";
                /* istanbul ignore else */
                if (expr.id.equals("(end)")) {
                    code = "S0207";
                }
                var err = new JException(code,
                    expr.position,
                    expr.value
                );
                if (recover) {
                    errors.add(err);
                    Symbol ret = new Symbol();
                    ret.type = "error"; ret.error = err;
                    return ret;
                } else {
                    //err.stack = (new Error()).stack;
                    throw err;
                }
        }
        if (expr.keepArray) {
            result.keepArray = true;
        }
        return result;
    }

    Symbol objectParser(Symbol left) {

        Symbol res = left!=null ? new Infix("{") : new Prefix("{");

        List< Symbol[] > a = new ArrayList<>();
        if (!node.id.equals("}")) {
            for (; ;) {
                var n = Parser.this.expression(0);
                advance(":");
                var v = Parser.this.expression(0);
                Symbol[] pair = new Symbol[] { n, v };
                a.add( pair ); // holds an array of name/value expression pairs
                if (!node.id.equals(",")) {
                    break;
                }
                advance(",");
            }
        }
        advance("}", true);
        if (left==null) { //typeof left === 'undefined') {
            // NUD - unary prefix form
            ((Prefix)res).lhsObject = a;
            ((Prefix)res).type = "unary";
        } else {
            // LED - binary infix form
            ((Infix)res).lhs = left;
            ((Infix)res).rhsObject = a;
            ((Infix)res).type = "binary";
        }
        return res;
    }

    public Symbol parse(String jsonata) {
        source = jsonata;

        // now invoke the tokenizer and the parser and return the syntax tree
        lexer = new Tokenizer(source);
        advance();
        // parse the tokens
        var expr = expression(0);
        if (!node.id.equals("(end)")) {
            var err = new JException("S0201",
                node.position,
                node.value
            );
            handleError(err);
        }

        expr = processAST(expr);

        if(expr.type.equals("parent") || expr.seekingParent != null) {
            // error - trying to derive ancestor at top level
            throw new JException("S0217",
                expr.position,
                expr.type);
        }

        if (errors.size() > 0) {
            expr.errors = errors;
        }

        return expr;
    }
}
