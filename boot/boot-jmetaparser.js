load('ometa-rhino.js');
load('jmetaoptimizer.js');
load('jmetaparser.js');
load('jmetacompiler.js');

tracing = false;
var ast = JMetaParser.matchAll(readFile("JMetaParser.jmeta"), "file");
print("ok:", ast);

tracing = false;
var res = JMetaCompiler.match(ast, "trans", undefined);
writeFile('../jmeta/JMetaParser.java', res);
print("ok: JMetaParser.java");

