package majorpeter.truenascertificatemanager

import android.content.Context
import android.security.KeyChain
import androidx.preference.PreferenceManager
import java.net.Socket
import java.net.URL
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509ExtendedKeyManager

class TruenasCertificateManagerClient(var context: Context) {
    val certificateAlias = "tnscm"

    suspend fun getRemainingDays(): Result<Int> {
        return try {
            val resp = httpsOp("/remaining")
            Result.success(String(resp).toInt())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun renewCert(): ByteArray {
        return httpsOp("/renew", "POST")
    }

    fun getCertificateChain(): Array<X509Certificate> {
        return KeyChain.getCertificateChain(context, certificateAlias) as Array<X509Certificate>
    }

    private suspend fun httpsOp(path: String, _requestMethod: String = "GET"): ByteArray {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(getKeyManager()), null, null)

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val caDomain = prefs.getString("ca_domain_name", "")

        val url = URL("https://$caDomain$path")
        with(url.openConnection() as HttpsURLConnection) {
            requestMethod = _requestMethod
            sslSocketFactory = sslContext.socketFactory

            return inputStream.readBytes()
        }
    }

    private fun getKeyManager(): X509ExtendedKeyManager {
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
