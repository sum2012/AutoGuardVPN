package kittoku.osc.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts


internal const val EXTRA_KEY_TYPE = "TYPE"
internal const val EXTRA_KEY_CERT = "CERT"
internal const val EXTRA_KEY_FILENAME = "FILENAME"

internal const val BLANK_ACTIVITY_TYPE_SAVE_CERT = 1

internal class BlankActivity : ComponentActivity() {
    private val createDocument = registerForActivityResult(ActivityResultContracts.CreateDocument("application/x-x509-ca-cert")) { uri ->
        val cert = intent.getByteArrayExtra(EXTRA_KEY_CERT)
        if (uri != null && cert != null) {
            contentResolver.openOutputStream(uri)?.use {
                it.write(cert)
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getIntExtra(EXTRA_KEY_TYPE, -1)
        if (type == BLANK_ACTIVITY_TYPE_SAVE_CERT) {
            val filename = intent.getStringExtra(EXTRA_KEY_FILENAME)
            if (filename != null) {
                createDocument.launch(filename)
            } else {
                finish()
            }
        } else {
            finish()
        }
    }
}
