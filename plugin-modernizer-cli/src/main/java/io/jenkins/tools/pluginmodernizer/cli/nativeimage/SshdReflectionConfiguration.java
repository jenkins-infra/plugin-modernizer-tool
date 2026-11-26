package io.jenkins.tools.pluginmodernizer.cli.nativeimage;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;

/**
 * Apache SSHD instantiates its security provider registrars by class name and calls the {@code getInstance}
 * factory methods of the JCA classes by reflection.
 */
@RegisterForReflection(
        targets = {
            Cipher.class,
            CertificateFactory.class,
            KeyAgreement.class,
            KeyFactory.class,
            KeyPairGenerator.class,
            Mac.class,
            MessageDigest.class,
            Signature.class
        },
        classNames = {
            "org.apache.sshd.common.util.security.SunJCESecurityProviderRegistrar",
            "org.apache.sshd.common.util.security.bouncycastle.BouncyCastleSecurityProviderRegistrar"
        },
        methods = true,
        fields = false)
public class SshdReflectionConfiguration {}
