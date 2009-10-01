load("ometa-rhino.js");

tracing = false;
writeFile('jmetaoptimizer.js', translateCode(readFile('jmetaoptimizer.txt')));
print("ok: optimizer");

writeFile('jmetaparser.js', translateCode(readFile('jmetaparser.txt')));
print("ok: parser");

writeFile('jmetacompiler.js', translateCode(readFile('jmetacompiler.txt')));
print("ok: compiler");

