package majorpeter.truenascertificatemanager

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import majorpeter.truenascertificatemanager.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        binding.btnRenew.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                renewCert()
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            updateCertInfo()
        }
    }

    override fun onRestart() {
        super.onRestart()
        CoroutineScope(Dispatchers.IO).launch {
            updateCertInfo()
        }
    }

    private suspend fun updateCertInfo() {
        withContext(Dispatchers.Main) {
            binding.loadingPanel.visibility = View.VISIBLE
            binding.textStatus.text = ""
            binding.textCertData.text = ""
            binding.btnRenew.visibility = View.INVISIBLE
        }

        val client = TruenasCertificateManagerClient(baseContext)
        val chain = client.getCertificateChain()
        val remaining = client.getRemainingDays()
        withContext(Dispatchers.Main) {
            if (remaining.isSuccess) {
                val remaining: Int = remaining.getOrNull()!!
                val requiredCertLifetime = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("required_cert_lifetime", "0")!!.toInt()

                binding.textStatus.text = "%d days remaining".format(remaining)
                if (remaining >= requiredCertLifetime) {
                    binding.textStatus.setTextColor(getColor(R.color.darkgreen))
                } else {
                    binding.textStatus.setTextColor(getColor(R.color.darkred))
                }
                binding.btnRenew.isEnabled = true
            } else {
                binding.textStatus.text = "Cannot connect to CA!"
                binding.textStatus.setTextColor(Color.RED)
                binding.btnRenew.isEnabled = false
            }
            binding.textCertData.text = chain[0].toString()
            binding.btnRenew.visibility = View.VISIBLE
            binding.loadingPanel.visibility = View.GONE
        }
    }

    private suspend fun renewCert() {
        val client = TruenasCertificateManagerClient(baseContext)
        val pfxData = client.renewCert()    //TODO error handling

        val intent: Intent = KeyChain.createInstallIntent()
        intent.putExtra(KeyChain.EXTRA_PKCS12, pfxData)
        startActivity(intent) //TODO get result
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.action_show_certs -> {
                KeyChain.choosePrivateKeyAlias(this, KeyChainAliasCallback { {} }, arrayOf<String>(), null, null, null)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
