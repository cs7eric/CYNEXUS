package cn.cccs7.cynexus.ai.infra.secret;

import org.springframework.stereotype.Component;

@Component
public class EnvSecretResolver implements SecretResolver {

    @Override
    public String resolve(String secretRef) {
        if (secretRef == null || !secretRef.startsWith("env://")) {
            return null;
        }
        return System.getenv(secretRef.substring("env://".length()));
    }
}
