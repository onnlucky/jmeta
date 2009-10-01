load('ometa-rhino.js');
load('jmetaoptimizer.js');
load('jmetaparser.js');
load('jmetacompiler.js');

tracing = false;
var ast = JMetaParser.matchAll(readFile("JMetaCompiler.jmeta"), "file");
print("ok:", ast);

tracing = false;
var res = JMetaCompiler.match(ast, "trans", undefined);
writeFile('../jmeta/JMetaCompiler.java', res);
print("ok: JMetaCompiler.java");

