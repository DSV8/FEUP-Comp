grammar IPmm;

@header {
    package pt.up.fe.comp2024;
}

IP : IP_num '.' IP_num '.' IP_num '.' IP_num ;
IP_num : [0-9] | [0-9][0-9] | '1'[0-9][0-9] | '2'[0-4][0-9] | '2''5'[0-5] ;

WS : [ \t\n\r\f]+ -> skip ;

ips_set
    : ip_adress+ EOF
    ;

ip_adress
    : adress=IP
    ;