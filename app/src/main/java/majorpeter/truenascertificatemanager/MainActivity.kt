package majorpeter.truenascertificatemanager

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.security.KeyChain
import android.security.KeyChainAliasCallback
import android.util.Log
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
    companion object {
        private const val TAG = "MainActivity"
    }
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

        initBackgroundJob()

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

        val chain = CertificateHelper(this).getCertificateChain()
        val client = TruenasCertificateManagerClient(this)
        val remaining = client.getRemainingDays()

        withContext(Dispatchers.Main) {
            if (remaining.isSuccess) {
                val remainingDays: Int = remaining.getOrNull()!!
                val requiredCertLifetime = PreferenceManager.getDefaultSharedPreferences(baseContext).getString("required_cert_lifetime", "0")!!.toInt()

                binding.textStatus.text = getString(R.string.days_remaining).format(remainingDays)
                if (remainingDays >= requiredCertLifetime) {
                    binding.textStatus.setTextColor(getColor(R.color.darkgreen))
                } else {
                    binding.textStatus.setTextColor(getColor(R.color.darkred))
                }
                binding.btnRenew.isEnabled = true
            } else {
                binding.textStatus.setText(R.string.cannot_connect_to_ca)
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

    private fun initBackgroundJob() {
        val jobKey = 100

        val info = JobInfo.Builder(jobKey, ComponentName(this, BackgroundCheckService::class.java))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)
            .setPersisted(false)
            .setPeriodic(24 * 3600 * 1000)
            //.setMinimumLatency(1).setOverrideDeadline(1)    // run immediately for debugging
            .build()

        val scheduler = getSystemService(JOB_SCHEDULER_SERVICE) as JobScheduler
        if (scheduler.schedule(info) != JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG, "Failed to schedule job")
        } else {
            Log.i(TAG, "Scheduled")
        }
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
                KeyChain.choosePrivateKeyAlias(this, KeyChainAliasCallback { run {} }, arrayOf<String>(), null, null, null)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
