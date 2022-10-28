package majorpeter.truenascertificatemanager

import android.content.Context
import android.security.KeyChain
import java.net.Socket
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.X509ExtendedKeyManager

class CertificateHelper(private val context: Context) {
    companion object {
        const val certificateAlias = "tnscm"
    }

    fun getCertificateChain(): Array<X509Certificate> {
        return KeyChain.getCertificateChain(context, certificateAlias) as Array<X509Certificate>
    }

    fun getRemainingDays(): Result<Int> {
        val chain = getCertificateChain()
        if (chain.isEmpty()) {
            return Result.failure(java.lang.Exception("No certificates"))
        }

        val deltaMs = chain[0].notAfter.time - Date().time
        return Result.success((deltaMs / (24 * 3600 * 1e3)).toInt())
    }

    fun getKeyManager(): X509ExtendedKeyManager {
        return object : X509ExtendedKeyManager() {
            override fun getClientAliases(
                keyType: String?,
                issuers: Array<out Principal>?
            ): Array<String> {
                return emptyArray()
            }

            override fun chooseClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String {
                return ""
            }

            override fun getServerAliases(
                keyType: String?,
                issuers: Array<out Principal>?
            ): Array<String> {
                return emptyArray()
            }

            override fun chooseServerAlias(
                keyType: String?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String {
                return ""
            }

            override fun getCertificateChain(_alias: String?): Array<X509Certificate> {
                return getCertificateChain()
            }

            override fun getPrivateKey(_alias: String?): PrivateKey {
                return KeyChain.getPrivateKey(context, certificateAlias) as PrivateKey
            }
        }
    }
}
