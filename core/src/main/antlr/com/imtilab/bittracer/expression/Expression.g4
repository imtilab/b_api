
grammar Expression;
@header{
    package com.imtilab.bittracer.antlr.expression;
}

expr
    : NULL                                          # Null
    | ARRAY_DATATYPE                                # ArrayDataType
    | DATATYPE                                      # DataType
    | BOOLEAN_VALUE                                 # BooleanValue
    | STRINGVALUE                                   # StringValue
    | expr  operator = (MULTIPLY | DIVIDE) expr     # MulDiv
    | expr  operator = (PLUS | MINUS) expr          # AddSub
    | FUNCTION '('expr((','(expr))+)?')'            # Function
    | '(' expr ')'                                  # Parenthesis
    | LIST                                          # List
    | EMPTY_OBJECT                                  # EmptyObject
    | BETWEEN_NUMBER                                # CloneSeparator // for between operator
    | sign = (PLUS | MINUS) ? NUMBER_OR_PERCENTAGE  # Number
    | KEY                                           # Key
    ;
//null
NULL: N U L L;
fragment N : 'n' | 'N';
fragment U : 'u' | 'U';
fragment L : 'l' | 'L';

//Values
ARRAY_DATATYPE: 'Array of ' + DATATYPE;
DATATYPE: 'String'|'Integer'|'Long'|'Double'|'Float'|'Number'|'Object'|'List'|'Boolean'|'Decimal';
LIST: '['((NUMBER|STRINGVALUE|BOOLEAN_VALUE)((','(NUMBER|STRINGVALUE|BOOLEAN_VALUE))+)?)?']';
EMPTY_OBJECT: '{'(SPACE+)?'}';
BOOLEAN_VALUE: 'true' | 'false';
BETWEEN_NUMBER: NUMBER':'NUMBER;
NUMBER_OR_PERCENTAGE: NUMBER '%'?;

//operator
PLUS: '+';
MINUS: '-';
DIVIDE: '/';
MULTIPLY: '*';

//functions
FUNCTION: ROUND | ROUND_UP | ROUN_DOWN | ALTER_VALUE | EVALFROM | TO_PRIMITIVE | TO_PRIMITIVE_LIST | CONCAT
| CONCAT_WITH_LIST | FILTER_EMPTY | TO_STRING | TO_DOUBLE | TO_LONG | TO_INT | FILTER_BY_PROPERTY
|DATE_CONVERT_TO_TIMEZONE | REPLACE;
ROUND: 'ROUND';
ROUND_UP: 'ROUND_UP';
ROUN_DOWN: 'ROUND_DOWN';
ALTER_VALUE: 'ALTER_VALUE';
// Two parameter first will be a keword 'REQUEST' | 'RESPONSE' | 'EXPECTED' | 'UPSTREAM', secode one will be a key.
EVALFROM: 'EVALFROM';
TO_PRIMITIVE: 'TO_PRIMITIVE';
TO_STRING: 'TO_STRING';
TO_DOUBLE: 'TO_DOUBLE';
TO_LONG: 'TO_LONG';
TO_INT: 'TO_INT';
TO_PRIMITIVE_LIST: 'TO_PRIMITIVE_LIST';
CONCAT: 'CONCAT';
CONCAT_WITH_LIST: 'CONCAT_WITH_LIST';
FILTER_EMPTY: 'FILTER_EMPTY';
FILTER_BY_PROPERTY: 'FILTER_BY_PROPERTY';
DATE_CONVERT_TO_TIMEZONE: 'DATE_CONVERT_TO_TIMEZONE';
REPLACE: 'REPLACE';

//json key
KEY: KEYMOLICULE+ ;
fragment KEYMOLICULE :KEYATOM(('.'(KEYATOM|'-'INT))+)?;
fragment KEYATOM: ('[]'|'['INT']'|ALPHANUMERIC+(SPECIALCHARACTERINKEY ALPHANUMERIC)?)('[]'|'['INT']')?;

//word or String
STRINGVALUE: '"' ((JAPANIES | ALPHANUMERIC | SPECIALCHARACTER)+)? '"';
SPACE: ' ';

//numerical
NUMBER: DIGIT+ ('.' DIGIT+)?;
INT : DIGIT+;
fragment DIGIT : [0-9] ;
fragment ALPHANUMERIC: [a-zA-Z0-9_];
fragment SPECIALCHARACTERINKEY: [\-];
fragment SPECIALCHARACTER: [[\]?=(){}.%:!/@|,&;#'\-+ ];
fragment JAPANIES: [\u3000-\uff9f※];

WS : (' ' | '\t')+ -> skip;