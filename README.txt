# JMeta

JMeta is a parser creating tool for java. It creates Packrat type parsers, als known as PEGs or backtracking parsers. JMeta is heavily inspired by OMeta. See also:
* http://en.wikipedia.org/wiki/Parsing_expression_grammar
* http://pdos.csail.mit.edu/~baford/packrat/
* http://tinlizzie.org/ometa/


## Important features:
* Mix and match java with your parsers.
* Error annotation to allow good error reporting.
* Inherit parsers to extend them (from OMeta).
* Semantic actions, using java or a shorthand notation.
* Support for direct and indirect left recursion.
* Parsing not just for text, but anything 'structured' (Strings, Arrays or ArrayLists; because java is statically typed).
* easy; easier compared to rats and antlr?


## Installation
Compile with `make` install with `make install`. Requires java 1.5 or higher. To bootstrap it uses js-meta, and rhino.

TODO: fix guard as first thing bug
TODO: start using new IntMap as 'inline cache' instead of giving each method a String and doing global caching.
TODO: fix position in line/char (can use the new inline cache when matching "\n" chars).
TODO: forbid rules with build-in name that may not be overridden.
TODO: allow inline classes.
TODO: allow parsers with custom constructors.
TODO: maybe improve syntax a lot more, and add more 'shorthand' stuff?
TODO: we could do without a runtime, by just creating inline classes, unless we inherit a grammar.
TODO: implement a more fancy memoization schema, including argument support.
TODO: experiment with throwing exceptions, instead of returning ERROR, see what is faster/safer.
TODO: some form of 'mutable, stack scoped' variable that auto unwinds as rules backtrack would be nice (but doesn't fit java so well), e.g. `method: n=name @frame={return new Frame(n);} args body { @frame };` where `args` and `body` can access `@frame`. And `@frame` ceises to exist after the `method` rule ends.
TODO: fix error reporting; an error on backtracking must be scoped ...


## Error Reporting
JMeta uses `!` syntax to annotate that a rule from that point on may no longer backtrack, if it does, instead of backtracking, a `SyntaxError` is reported, noting the last rule that was expected to pass, but failed. A rule must fully parse after the first `!` appeared in the rule. It is allowed that the whole rule backtracks. Notice you cannot just put `!` marks everywhere, since backtracking is the feature that makes PEGs work.

Bad Example; simplistic xml parsing:
```
    element: "<" ! n=name props "/>"
           | "<" ! n=name props ">" element* "<" n=name "/>" ;
```
The second alternative will never run, since the first is not allowed to backtrack, while it must backtrack in order to try the second alternative. (Both rule alternatives start with "<")

A much better, yet still simplistic xml parsing, which also ensures proper nesting:
```
    element: "<" n=name props "/" ! ">"
           | "<" ! n=name props ">" element*
             !"a closing '$n'" &("<" m=name ?{return m.equals(n);}) ! "<" name "/>" ;
```
Notice the use of a custom message, and the use of lookahead (&) in order to put the error message at the correct location.


## Tokens
In PEG based parsers, there is no separate tokenizer. This simplyfies things, but does require some thought. Best strategy is whenever you call a rule that you would normally think of as a token, to prepend it with a call to eat all whitespace, by default the `.` rule.
Extending the above xml sample:
```
    element: ."<"   .n=name props ."/" ! .">"
           | ."<" ! .n=name props        .">" element*
             !"a closing '$n'" .&("<" .m=name ?{return m.equals(n);}) ! ."<" .name ! ."/".">" ;
```
Notice the `.` just before the lookahead block (`&...`), and not inside, to keep the position of the error report correct.


## Java Caveats
JMeta does not understand java code at all. It fakes it. This has some consequences:
1. When putting fields and methods inside a parser, make sure to use at least one qualifier, e.g. `public` or `final` or such.
2. When writing semantic expressions, make sure to match the curly braces. Even inside strings. you may need to add a closing brace inside a comment, just to balance the braces.
3. Every variable is of type `Object`, and every rule returns an `Object` you have to cast it, maybe even inspect it using `instanceof`.
4. The parser throws a `SyntaxError` on error, which is not an `Exception`, but an `Error`, so take care to catch it correctly.


## Semantic Actions
Any rule always returns its last evaluated rule or semantic action. You can place semantic actions anywhere, and have many of them. They are like methods that get called with all previously defined variables.

To make it easier to parse text into an Array based AST, JMeta has a shorthand notation, and two list helper functions, that receive `Object`s which may be `Object[]` arrays or `ArrayList`s: `concat(Object head, Object tail)`; `join(Object list, String sep="")`.

example shorthand:
```
  // equivalent
  name: f=nstart rs=nrest* { [f] + rs } ;
  name: f=nstart rs=nrest* { return concat(new Object[] { f }, rs); } ;
```

And example of the `join` method:
```
  string = "\"" xs=(~"\"" _)* "\"" { return join(xs); } ;
```

Notice that in any semantic action you can execute arbitrary java, including assigning to member fields or running methods.


## Parser creation notes
Also see sample below. Since PEGs backtrack, you must be careful when using side-effects. That is, it is best that rules return a value that represents everything about that rule, instead of mutating some instance variable of the parser.

Also, since JMeta is good at parsing Array based structures too, it is recommended you parse in two steps (using two distict parsers):
1) Parse the string content to a (simplistic/verbose) AST. Focus on handling human input, so create a flexible syntax, easy to understand for humans, and provide good syntax error reporting.
2) Analyze the AST, rework it into the final thing you want. Here you report semantic errors. But you can rely on the fact that the AST is produced by your first parser, so is completely valid. Any syntax error here is a bug in either this parser, or the first.


## annotated example
```
// single line comments
/* multi line comments

   A simple calculator example, save as `Calculator.jmeta`

   compile to java file: `jmeta Calculator` or `java -jar /usr/share/java/jmeta.jar Calculator`
   compile to class file: `javac -cp /usr/share/java/jmeta-runtime.jar Calculator.java`
   run: `java -cp/usr/share/java/jmeta-runtime.jar:. Calculator "10 + 10"`
*/
// this parser will turn into: `public class Calculator extends BaseParser { ...`
public parser Calculator {
    // defining a java method; all fields or methods have to start with at least one of 'public' 'private' 'final' etc ...
    public static void main(String[] args) {
        Object[] ast = (Object[]) new Calculator().parse(args[0]);
        System.out.println(new Interpreter().parse(ast));
    }

    // this is jmeta syntax; the 'start' rule is the default rule to start with
    // notible:
    //  `!` means a syntax error occured if the rule backtracks after this point
    //  `.` means any whitespace (it runs the build-in rule `whitespace`)
    //  `end` matches end of input, equivalent to `~_` (not anything)
    //  `e=expr` means, parse an expression and assign it to the variable `e`
    //  `{ e }`  means run a semantic action, in this case, return `e`
    //  `{ ['ADD, l, r] }` is shorthand for: `{ return new Object[] {"ADD", l, r}; }`
    start: ! e=expr . end      { e };
    expr:
        | l=expr ."+"! r=expr1 { ['ADD, l, r] }
        | l=expr ."-"! r=expr1 { ['SUB, l, r] }
        | expr1
    ;
    expr1:
        | l=expr1 ."*"! r=value { ['MUL, l, r] }
        | l=expr1 ."/"! r=value { ['DIV, l, r] }
        | l=expr1 ."%"! r=value { ['MOD, l, r] }
        | value
    ;
    value:
        | ."(" ! e=expr .")" { e }
        | . n=num              { ['INT, n] }
    ;
    num: ds=digit+ { return Integer.parseInt(join(ds)); } ;
}

// a second parser in the same file (only one can be public, just like classes)
// notice this parser does not process text, but a tree like nesting of Arrays and ArrayLists
// the `[` opens up such a list, and starts parsing inside of it the matching `]` backs up one level.
// `end` in this context means end of list, not necesairly end of input
parser Interpreter {
    start: destruct ;

    // a trick, parse anything and then apply the corresponding rule
    destruct: r=_ res=apply(r) end   { res } ;
    val: [ res=destruct ]            { res } ;

    // second trick, the `:` colon is optional, this reads like ocaml matching rules
    // notice the cast to Integer, since everyting is typed Object in the parser
    ADD l=val r=val { return (Integer)l + (Integer)r; } ;
    SUB l=val r=val { return (Integer)l - (Integer)r; } ;
    MUL l=val r=val { return (Integer)l * (Integer)r; } ;
    DIV l=val r=val { return (Integer)l / (Integer)r; } ;
    MOD l=val r=val { return (Integer)l % (Integer)r; } ;
    INT v=_ ;
}
```

