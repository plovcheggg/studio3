==PREFS==
js.formatter.spaces.after.commas=1
js.formatter.spaces.after.assignment.operator=1
js.formatter.spaces.after.arithmetic.operator=1
js.formatter.spaces.after.conditional.operator=1
js.formatter.indent.case.body=true
js.formatter.indent.switch.body=true
js.formatter.spaces.before.for.semicolon.operator=0
js.formatter.spaces.before.parentheses=0
js.formatter.spaces.after.semicolon.operator=1
js.formatter.spaces.before.postfix.operator=0
js.formatter.spaces.after.postfix.operator=0
js.formatter.spaces.before.relational.operator=1
js.formatter.spaces.before.case.colon.operator=1
js.formatter.spaces.before.unary.operator=0
js.formatter.wrap.comments=false
js.formatter.indent.group.body=true
js.formatter.line.after.function.declaration=1
js.formatter.spaces.after.concatenation.operator=1
js.formatter.wrap.comments.length=80
js.formatter.spaces.before.conditional.operator=1
js.formatter.formatter.tabulation.size=4
js.formatter.spaces.after.relational.operator=1
js.formatter.spaces.after.case.colon.operator=1
js.formatter.spaces.before.prefix.operator=0
js.formatter.indent.blocks=true
js.formatter.spaces.before.concatenation.operator=1
js.formatter.line.preserve=1
js.formatter.brace.position.case.block=same.line
js.formatter.brace.position.switch.block=same.line
js.formatter.formatter.tabulation.char=editor
js.formatter.indent.function.body=true
js.formatter.line.after.function.declaration.expression=0
js.formatter.formatter.indentation.size=4
js.formatter.newline.before.name.value.pairs=true
js.formatter.spaces.after.parentheses=0
js.formatter.spaces.after.for.semicolon.operator=1
js.formatter.newline.before.if.in.elseif=false
js.formatter.spaces.before.key.value.operator=1
js.formatter.spaces.before.commas=0
js.formatter.spaces.after.unary.operator=0
js.formatter.spaces.before.arithmetic.operator=1
js.formatter.spaces.before.assignment.operator=1
js.formatter.newline.before.dowhile=false
js.formatter.newline.before.else=false
js.formatter.spaces.before.semicolon.operator=0
js.formatter.newline.before.finally=false
js.formatter.newline.before.catch=false
js.formatter.spaces.after.prefix.operator=0
js.formatter.brace.position.function.declaration=same.line
js.formatter.spaces.after.key.value.operator=1
js.formatter.brace.position.blocks=same.line
js.formatter.spaces.before.declaration.parentheses.opening=0
js.formatter.spaces.after.declaration.parentheses.opening=0
js.formatter.spaces.before.declaration.parentheses.closing=0
js.formatter.spaces.before.conditional.parentheses.opening=1
js.formatter.spaces.after.conditional.parentheses.opening=0
js.formatter.spaces.before.conditional.parentheses.closing=0
js.formatter.spaces.before.invocation.parentheses.opening=0
js.formatter.spaces.after.invocation.parentheses.opening=0
js.formatter.spaces.before.invocation.parentheses.closing=0
js.formatter.spaces.before.loop.parentheses.opening=1
js.formatter.spaces.after.loop.parentheses.opening=0
js.formatter.spaces.before.loop.parentheses.closing=0
js.formatter.spaces.before.array.access.parentheses.opening=0
js.formatter.spaces.after.array.access.parentheses.opening=0
js.formatter.spaces.before.array.access.parentheses.closing=0
==CONTENT==
if(2 > $a)
$a = 2;
else
for(var d = a[0]; e < j; ++s) {
while(d > 0){
a[0]--;
}
}
function foo(a,b,c){
print(a+b+c);
}

var k = new Array(a, b, c);
alert(k.getCount(3));
==FORMATTED==
if (2 > $a)
    $a = 2;
else
    for (var d = a[0]; e < j; ++s) {
        while (d > 0) {
            a[0]--;
        }
    }
function foo(a, b, c) {
    print(a + b + c);
}

var k = new Array(a, b, c);
alert(k.getCount(3)); 