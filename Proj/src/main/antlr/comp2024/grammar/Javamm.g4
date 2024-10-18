grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUAL : '=';
SEMI   : ';' ;
COMMA  : ',' ;
DOT : '.' ;
ELLIPSIS : '...' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LBRACK : '[' ;
RBRACK : ']' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;
NEG : '!' ;
AND : '&&' ;
LT : '<' ;

CLASS : 'class' ;
INT : 'int' ;
BOOL : 'boolean' ;
STR : 'String' ;
VOID : 'void' ;
STATIC : 'static' ;
IMPORT: 'import' ;
EXTENDS : 'extends' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

TRU : 'true' ;
FAL : 'false' ;

NEW : 'new' ;
THIS : 'this' ;
LEN : 'length' ;
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;

INTEGER : '0' | [1-9][0-9]* ;
ID : [a-zA-Z_$] [a-zA-Z0-9_$]* ;

WS : [ \t\n\r\f]+ -> skip ;

EL_COMMENT : '//' ~[\r\n]* -> skip ;
ML_COMMENT: '/*' .*? '*/' -> skip ;

 program
    : (importDecl)* classDecl EOF
    ;

 importDecl
    : op=IMPORT name+=ID ( DOT name+=ID )* SEMI
    ;

 classDecl
    : CLASS name=ID ( EXTENDS ext=ID )? LCURLY ( varDecl )* ( methodDecl )* RCURLY
    ;

 varDecl
    : type name=(ID | LEN) SEMI
    ;

 methodDecl locals [boolean isStatic=false, boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})? (STATIC {$isStatic=true;})? type name=ID LPAREN ( par ( COMMA par )* )? RPAREN LCURLY ( varDecl )* ( stmt )* RCURLY
    ;

 par
    : type name=(ID | LEN) #Param
    ;

 type locals [boolean isArray=false]
    : name=INT LBRACK RBRACK {$isArray=true;} #IntArrayType
    | name+=INT name+=ELLIPSIS #IntVarArgsType
    | name=BOOL #BoolType
    | name=INT #IntType
    | name=STR #StrType
    | name=STR LBRACK RBRACK {$isArray=true;} #StrArrayType
    | name=ID #ObjType
    | name=VOID #VoidType
    ;

 stmt
    : LCURLY ( stmt )* RCURLY #BlockStmt
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfElseStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #ExprStmt
    | expr op=EQUAL expr SEMI #AssignStmt
    | RETURN expr SEMI #ReturnStmt
    ;

 expr
    : expr (DOT method=LEN | DOT method=ID LPAREN (expr ( COMMA expr )*)? RPAREN) #MemberMethodAccess
    | expr LBRACK expr RBRACK #ArrayAccess
    | LPAREN expr RPAREN #Parentheses
    | op=NEG expr #Negation
    | NEW qualifier=INT LBRACK expr RBRACK #NewArrayExpr
    | NEW qualifier=ID LPAREN RPAREN #NewObjExpr
    | expr op=(MUL | DIV) expr #ArithmeticExpr
    | expr op=(ADD | SUB ) expr #ArithmeticExpr
    | expr op=LT expr #RelationalExpr
    | expr op=AND expr #LogicalExpr
    | LBRACK ( expr ( COMMA expr )* )? RBRACK #ArrayInitExpr
    | value=INTEGER #IntegerLiteral
    | value=(TRU | FAL) #BoolLiteral
    | value=(ID | LEN) #Identifier
    | value=THIS #This
    ;
