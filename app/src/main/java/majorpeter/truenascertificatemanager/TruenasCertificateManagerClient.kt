package majorpeter.truenascertificatemanager

import android.content.Context
import androidx.preference.PreferenceManager
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext

class TruenasCertificateManagerClient(private val context: Context) {
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

    private suspend fun httpsOp(path: String, _requestMethod: String = "GET"): ByteArray {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(arrayOf(CertificateHelper(context).getKeyManager()), null, null)

        val url = URL("https://" + getCaDomainName() + path)
        with(url.openConnection() as HttpsURLConnection) {
            requestMethod = _requestMethod
            sslSocketFactory = sslContext.socketFactory

            return inputStream.readBytes()
        }
    }

    fun getCaDomainName(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("ca_domain_name", "") as String
    }
}
