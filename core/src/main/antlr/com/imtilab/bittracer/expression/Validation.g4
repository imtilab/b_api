grammar Validation;

@header{
    package com.imtilab.bittracer.antlr.expression;
}

//parse
// : block EOF                    #parseStatement
// ;

//block
// : ( statement | functionDecl )* ( Return expression ';' )?
// ;

statement
// : assignment                   #assignmentStatement
 : functionalStatement                 #functionCallStatement
// | ifStatement                  #ifStatementStatement
// | forStatement                 #forStatementStatement
// | whileStatement               #whileStatementStatement
 ;

//assignment
// : Identifier indexes? '=' expression
// ;

functionalStatement
// : Identifier '(' exprList? ')' #identifierFunctionCall
// | Println '(' expression? ')'  #printlnFunctionCall
// | Print '(' expression ')'     #printFunctionCall
 : Assert expression                                    #assertFunctionCall
 | FLOOR '(' expression ')'                                     #floorFunctionCall
 | CEIL '(' expression ')'                                      #ceilFunctionCall
 | ROUND '(' expression ')'                                     #roundFunctionCall
 | Size value                                           #sizeFunctionCall
 | Length value                                         #lengthFunctionCall
 | current = (CurrentDateTime | CurrentDate)            #currentDateFunctionCall
 | Date value Pattern? value                            #dateFunctionCall
 | DateTime value Pattern? value                        #dateTimeFunctionCall
 // DATETIME dateKey PATTERN datePattern timeZone TO PATTERN toDatePattern toTimeZone
 | DateTime value Pattern? value value To Pattern? value value    #dateTimeConversionFunctionCall
 | Date value Pattern? value To Pattern? value          #dateConvertionFunctionCall
 | Validate (Schema|DataType) JsonObj                   #validateJsonSchema
 | DataType jsonKey String                              #validateDatatype
 | Validate Pattern value String                        #validatePattern
 | Exist jsonKey (','jsonKey)*                          #existKey
 | Exist Any jsonKey (','jsonKey)*                          #existAnyKey
 | (Not Exist | NotExist) jsonKey (','jsonKey)*         #notExistKey
 | Sort value By jsonKey (','jsonKey)* order=(Ascending| Decending)? #sortFunctionCall
 | MIN value                                            #minValueStatement
 | MAX value                                            #maxValueStatement
 ;

//ifElseStat
// : If condition Do exIf=expression  (Else Do exElse = expression)?                                                  #visitIfElseStatement
// ;

//ifStat
// : If condition Do expression                                       #ifStatement
// ;
//
//elseIfStat
// : Else If expression Do block
// ;
//
//elseStat
// : Else Do expression                                                #elseStatment
// ;

//functionDecl
// : Def Identifier '(' idList? ')' block End
// ;

forStatement
 : For Each value expression                #forEach
 | For Number To value expression           #forRange
 | For First Number expression        #forFirst
 | For Last Number value expression         #forLast
 ;
//
//whileStatement
// : While expression Do block End
// ;

//idList
// : Identifier ( ',' Identifier )*
// ;

exprList
 : expression ( ',' expression )*
 ;

condition
  : expression                                          #conditionExpression;

expression
 : '-' expression                                       #unaryMinusExpression
 | '!' expression                                       #notExpression
 | <assoc=right> expression '^' expression              #powerExpression
 | expression op=( '*' | '/' | '%' ) expression         #multExpression
 | expression op=( '+' | '-' ) expression
 (dateTimeOp=(Year|Month|Week|Day|Hour|Minute) ('s'|'S')?)?     #addExpression
 | expression op=( '>=' | '<=' | '>' | '<' ) expression #compExpression
 | expression op=( '==' | '!=' ) expression             #eqExpression
 | expression '&&' expression                           #andExpression
 | expression '||' expression                           #orExpression
 | expression '?' expression ':' expression             #ternaryExpression
 | expression In expression                             #inExpression
 | expression Not In expression                             #notInExpression
// | expression ContainOnly expression           #arrayContainingInAnyOrder
 | expression StartsWith expression                     #startWithExpression
 | expression StartsWithAny expression                  #startWithAnyExpression
 | expression EndsWith expression                       #endWithExpression
 | expression EndsWithAny expression                    #endWithAnyExpression
 | expression Contains expression                       #containsWithExpression
 | expression ContainsWithAny expression                #containsWithAnyExpression
 | value                                                #valueExpression
 | '(' expression ')' indexes?                          #expressionExpression
 | forStatement                                         #forExpression
 | If condition Do exIf=expression  (Else Do exElse = expression)?  #ifElseExpression
// | ifElseStat                                           #ifElseExpression
 ;

value
 : Number                                               #numberExpression
 | Bool                                                 #boolExpression
 | Null                                                 #nullExpression
 | Concats value(','value)*                             #concatExpression
 | value Replace (flg=(First | Last | IgnoreCase ))? value By value     #replaceExpression
 | functionalStatement                                  #functionCallExpression
 | List indexes?                                        #listExpression
 | JsonObj                                               #jsonValueExpression
 | from = (REQUEST | RESPONSE | UPSTREAM)? jsonKey      #jsonKeyExpression
 | String indexes?                                      #stringExpression
 | type = (TypeInteger | TypeString | TypeBoolean | TypeDouble | TypeLong | TypeFloat) value         #typeConvertionExpression
 | FORMATNUM value As value                            #formatNumberExpression
 | FORMATSTR value As value                            #formatExpression;

List
 : [[] Space* ( String | Bool | Number | JsonObj) Space* (',' Space* ( String | Bool | Number | JsonObj) Space* )* [\]] | EmptyList
 | EmptyList
 ;

JsonObj
 : '{}'
 |'{' (String Space*':'Space*(String|Bool|Number|List|JsonObj))(Space*','Space*(String Space*':'Space*(String|Bool|Number|List|JsonObj)))* '}' ;


EmptyList: '[]';

indexes
 : ( '[' expression ']' )+
 ;

//Println  : 'println';
//Print    : 'print';
//Input    : 'input';
Assert   : 'assert';
MIN      : M I N;
MAX      : M A X;
CurrentDateTime: C U R R E N T D A T E T I M E;
CurrentDate    : C U R R E N T D A T E;
Size     : S I Z E;
Length   : L E N G T H;
Concats   : C O N C A T;
Date     : D A T E;
DateTime : D A T E T I M E;
Pattern  : P A T T E R N;
Sort     : S O R T;
By       : B Y;
Ascending: A S C;
Decending: D E S C;
StartsWith: S T A R T S W I T H;
StartsWithAny: S T A R T S W I T H A N Y;
EndsWith  : E N D S W I T H;
EndsWithAny  : E N D S W I T H A N Y;
Contains : C O N T A I N S;
ContainsWithAny : C O N T A I N S W I T H A N Y;
Replace  : R E P L A C E;
IgnoreCase: I G N O R E C A S E;
Def      : 'def';
If       : 'if';
Else     : 'else';
Return   : 'return';
For      : F O R;
While    : 'while';
To       : T O;
Do       : D O;
//End      : 'end';
Each     : E A C H;
Last     : L A S T;
First    : F I R S T;
In       : I N;
As       : A S;
Null     : N U L L;
REQUEST  : R E Q U E S T;
RESPONSE : A P I R E S P O N S E;
UPSTREAM : U P S T R E A M;
Validate : V A L I D A T E;
Schema: S C H E M A;
DataType: D A T A T Y P E;
Compare : C O M P A R E;
Exist   : E X I S T;
Any     : A N Y;
Not   : N O T;
NotExist   : N O T E X I S T;
TypeInteger : I N T E G E R;
TypeString  : S T R I N G;
TypeBoolean : B O O L E A N;
TypeDouble  : D O U B L E;
TypeLong    : L O N G;
TypeFloat   : F L O A T;
Year : Y E A R;
Month : M O N T H;
Week : W E E K;
Day : D A Y;
Hour : H O U R;
Minute : M I N U T E;

Or       : '||';
And      : '&&';
Equals   : '==';
NEquals  : '!=';
GTEquals : '>=';
LTEquals : '<=';
Pow      : '^';
Excl     : '!';
GT       : '>';
LT       : '<';
Add      : '+';
Subtract : '-';
Multiply : '*';
Divide   : '/';
Modulus  : '%';
OBrace   : '{';
CBrace   : '}';
OBracket : '[';
CBracket : ']';
OParen   : '(';
CParen   : ')';
SColon   : ';';
Assign   : '=';
Comma    : ',';
QMark    : '?';
Colon    : ':';
FLOOR    : F L O O R;
CEIL     : C E I L;
ROUND    : R O U N D;
FORMATNUM : F O R M A T N U M;
FORMATSTR : F O R M A T S T R;

Bool
 : T R U E
 | F A L S E
 ;

Number
 : Int ( '.' Digit* )?
 | ([0-9]+)'.'([0-9]+)E('+'|'-')?([0-9]+)
 ;

//json key
//JsonKey: KEYMOLICULE+ ;
//fragment KEYMOLICULE :KEYATOM(('.'(KEYATOM))+)?;
//fragment KEYATOM: ('[]'|'['Int']'|[a-zA-Z0-9]+([a-zA-Z0-9_\-])?)('[]'|'['Int']')?;

jsonKey
 : JsonQuery
 | (indexes'.')*? Identifier('.'Identifier)*? ('.'indexes)*?
 | jsonKey(JsonQuery)?('.'jsonKey(JsonQuery)?)
 | IntJsonKey
  ;

IntJsonKey
  : (JsonQuery | Identifier)* '.' Int [A-Za-z]* ;

JsonQuery
 : Identifier? EmptyList
 | Identifier?'[*]'
 | Identifier?'['Int(':'Int)?']'
 | Identifier?'['((Int':')|(':'Int))']'
 | Identifier?'['Int(','Int)*?']'
 | Identifier?'[?]'
 | Identifier?'[?('JsonCondition (Space? (And|Or) Space? JsonCondition)*')]';

JsonCondition
    : (JsonKeyQuery | String | Number | Bool | Null | List) Space*
    (Equals|NEquals|LT|GT|LTEquals|GTEquals|'=~'|In|'nin'|'subsetof'|'anyof'|'noneof'|'size'|'emty') Space*
    (JsonKeyQuery | String | Number | Bool | Null | List) Space*;

JsonKeyQuery
    : ('@.'|'$.') Identifier(JsonQuery)?('.'Identifier(JsonQuery)?)*
    ;

Identifier
 : [a-zA-Z_0-9] [a-zA-Z_0-9\-]*
 ;

String
 : ["] ( ~["\r\n\\] | '\\' ~[\r\n] )* ["]
 | ['] ( ~['\r\n\\] | '\\' ~[\r\n] )* [']
 ;

//PatternFormat
// : [/] ( ~["\r\n\\] | '\\' ~[\r\n] )* [/]
// ;

//Schema
// : '{' ( ~[\r\n\\] | '\\' ~[\r\n] )*'}'
// ;

Comment
 : ( '//' ~[\r\n]* | '/*' .*? '*/' ) -> skip
 ;

Space
 : [ \t\r\n\u000C] -> skip
 ;

fragment Int
 : [1-9] Digit*
 | '0'
 ;

fragment Digit
 : [0-9]
 ;

 // Alphabet
 fragment A : 'a' | 'A';
 fragment B : 'b' | 'B';
 fragment C : 'c' | 'C';
 fragment D : 'd' | 'D';
 fragment E : 'e' | 'E';
 fragment F : 'f' | 'F';
 fragment G : 'g' | 'G';
 fragment H : 'h' | 'H';
 fragment I : 'i' | 'I';
 fragment J : 'j' | 'J';
 fragment K : 'k' | 'K';
 fragment L : 'l' | 'L';
 fragment M : 'm' | 'M';
 fragment N : 'n' | 'N';
 fragment O : 'o' | 'O';
 fragment P : 'p' | 'P';
 fragment Q : 'q' | 'Q';
 fragment R : 'r' | 'R';
 fragment S : 's' | 'S';
 fragment T : 't' | 'T';
 fragment U : 'u' | 'U';
 fragment V : 'v' | 'V';
 fragment W : 'w' | 'W';
 fragment X : 'x' | 'X';
 fragment Y : 'y' | 'Y';
 fragment Z : 'z' | 'Z';
