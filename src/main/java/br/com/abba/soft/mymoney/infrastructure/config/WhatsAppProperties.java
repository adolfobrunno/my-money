package br.com.abba.soft.mymoney.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;

@Getter
@Component
public class WhatsAppProperties {
    @Value("${meta.whatsapp.verify-token:fAkE_vErIfY_tOkEn}")
    private String verifyToken;

    @Value("${meta.whatsapp.access-token:FAKE_ACCESS_TOKEN}")
    private String accessToken;

    @Value("${meta.whatsapp.phone-number-id:000000000000000}")
    private String phoneNumberId;

    @Value("${meta.whatsapp.app-secret:FAKE_APP_SECRET}")
    private String appSecret;

}
