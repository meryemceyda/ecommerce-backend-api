
package com.meryem.ecommerce.dto;

public class AuthResponseDto {

    private String accessToken;
    private String tokenType;

    public AuthResponseDto(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
