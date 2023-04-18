package com.imtilab.bittracer.model

import groovy.transform.builder.Builder

import java.time.LocalDateTime

@Builder
class Token {
    String accessToken
    String refreshToken
    String tokenType
    Long expiresIn
    String scope
    int statusCode
    String securityParameters
    String errorDescription
    String error
    LocalDateTime issuedAt

    // issue token request
    String clientId
    String clientSecret

    @Override
    String toString() {
        return "Token{" +
                "accessToken='" + accessToken + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", tokenType='" + tokenType + '\'' +
                ", expiresIn='" + expiresIn + '\'' +
                ", scope='" + scope + '\'' +
                ", statusCode=" + statusCode +
                ", securityParameters='" + securityParameters + '\'' +
                ", errorDescription='" + errorDescription + '\'' +
                ", error='" + error + '\'' +
                '}'
    }
}
