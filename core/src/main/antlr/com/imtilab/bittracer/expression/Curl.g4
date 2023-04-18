grammar Curl;

@header{
    package com.imtilab.bittracer.antlr.expression;
}

curl
    : 'curl' '--location' '--request' method=('GET'|'POST'|'PUT'|'DELETE') URL (HEADER+)? (DATA)? (FORM+)?  # CURLCOMMAND
    ;

DATA: ('--data' | '--data-raw') WS '\''STRINGVALUE'\'';
FORM: ('--form' | '--form-data') WS '\''STRINGVALUE'\'';

URL: '\''STRINGVALUE'\'';

HEADER
    : '--header' WS '\''STRINGVALUE':'STRINGVALUE'\''
    | '--header' WS '\''STRINGVALUE';'WS?'\'';

fragment STRINGVALUE: ((JAPANIES | ALPHANUMERIC | SPECIALCHARACTER |WS)+)?;
fragment ALPHANUMERIC: [a-zA-Z0-9_-];
fragment SPECIALCHARACTER: [[\]?=(){}.%:!/@|,&;#$"*~+ `∫];
fragment JAPANIES: [\u3000-\uff9f];
WS: [ \n\t\r\\] + -> skip;
