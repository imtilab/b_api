package com.imtilab.bittracer.utils

import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.constant.PropertiesKey
import com.imtilab.bittracer.model.Token
import org.apache.commons.lang3.StringUtils

class TokenUtils {

    static String getTokenByTokenSet(String tokenSet, Map tokenBox) {
        tokenSet = tokenSet.trim()
        Token token = null
        // token already generated
        if (tokenBox.containsKey(tokenSet)) {
            token = tokenBox.get(tokenSet)
            if (TokenManager.isExpiredToken(token)) {
                token = TokenManager.issueToken(tokenSet)
            }
        } else {
            token = TokenManager.issueToken(tokenSet)
        }
        if (token != null && StringUtils.isNotEmpty(token.getTokenType()) && StringUtils.isNotEmpty(token.getAccessToken())) {
            tokenBox.put(tokenSet, token)
        }
        String tokenType = ConfigurationResource.instance().getConfigProp(PropertiesKey.DEFAULT_TOKEN_TYPE)
        tokenType = tokenType ?: token.getTokenType()
        tokenType + Constants.SPACE + token.getAccessToken()
    }
}
