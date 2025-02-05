package org.amine.security.polling.onlinepollingsystem.system.security.jwtgen

import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import java.io.IOException
import java.security.*

@Service
class KeysGenerator {
    var pubKey: PublicKey? = null

    var priKey: PrivateKey? = null

    @Throws(NoSuchAlgorithmException::class, IOException::class)
    fun createKeys() {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        val secureRandom = SecureRandom()
        keyPairGenerator.initialize(2048, secureRandom)
        val keyPair = keyPairGenerator.generateKeyPair()
        this.priKey = keyPair.private
        this.pubKey = keyPair.public
    }

    @Bean
    @Throws(NoSuchAlgorithmException::class, IOException::class)
    fun getPrivateKey(): PrivateKey? {
        if (priKey == null) {
            createKeys()
            return priKey;
        }
        return priKey
    }

    @Bean
    @Throws(NoSuchAlgorithmException::class, IOException::class)
    fun getPublicKey(): PublicKey? {
        if (pubKey == null) {
            createKeys()
            return pubKey;
        }
        return pubKey
    }
}