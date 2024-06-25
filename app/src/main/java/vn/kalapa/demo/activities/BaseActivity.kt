package vn.kalapa.demo.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {
    private var enterScreen = false
    override fun onResume() {
        if (!enterScreen) {
            enterScreen = true
        }
        super.onResume()
    }

    private var tempHandler: (() -> Unit)? = null
    fun openSettingUI(postRequestHandler: (() -> Unit)? = null) {
        tempHandler = postRequestHandler
        startActivityForResult(Intent(this@BaseActivity, SettingActivity::class.java), 18894)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 18894)
            tempHandler?.let { it() }
    }

}