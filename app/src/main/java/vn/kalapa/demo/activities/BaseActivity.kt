package vn.kalapa.demo.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity(){
    private var enterScreen = false
    override fun onResume() {
        if (!enterScreen) {
            enterScreen = true
        }
        super.onResume()
    }

    fun openSettingUI() {
        startActivity(Intent(this@BaseActivity, SettingActivity::class.java))
    }

}