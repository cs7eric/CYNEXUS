package cn.cccs7.cynexus.ai.infra.secret;

public interface SecretResolver {

    String resolve(String secretRef);
}
