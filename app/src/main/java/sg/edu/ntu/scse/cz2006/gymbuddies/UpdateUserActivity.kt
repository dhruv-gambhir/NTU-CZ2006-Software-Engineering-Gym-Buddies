package sg.edu.ntu.scse.cz2006.gymbuddies

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login_chooser.*
import sg.edu.ntu.scse.cz2006.gymbuddies.tasks.CheckFirstRun

/**
 * This activity is just used to ensure that we have processed everything from our database that we need to prevent
 *
 * This checks that
 * - The user has completed their first run sequence (MUST BE AUTHENTICATED)
 *
 * For sg.edu.ntu.scse.cz2006.gymbuddies in Gym Buddies!
 *
 * @author Kenneth Soh
 * @since 2019-09-16
 */
class UpdateUserActivity : AppCompatActivity() {

    /**
     * Function that is called when an activity is created
     * @param savedInstanceState Bundle? The Android saved instance state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_chooser)

        val auth = FirebaseAuth.getInstance().currentUser
        if (auth == null) {
            Log.e(TAG, "User not valid, exiting")
            startActivity(Intent(this, LoginChooserActivity::class.java))
            finish()
            return
        }

        message.text = "Updating user data..."

        val sp = PreferenceManager.getDefaultSharedPreferences(this)
        if (!sp.contains("nearby-gyms")) sp.edit().putInt("nearby-gyms", 10).apply() // Default to 10 nearby gyms

        CheckFirstRun(this, object: CheckFirstRun.Callback {
            override fun isFirstRun(success: Boolean) {
                Log.d(TAG, "isFirstRun: $success")
                if (success) goEditProfile() else startActivity(Intent(this@UpdateUserActivity, MainActivity::class.java))
                finish()
            }

            override fun isError() {
                Log.w(TAG, "Error detected, logging out")
                val logout = Intent(this@UpdateUserActivity, LoginChooserActivity::class.java).apply { putExtra("logout", true) }
                startActivity(logout)
                finish()
            }

        }).execute(auth.uid)
        return
    }

    /**
     * Internal function that is called when the user is a new user and has not setup their profile yet to go over to the [ProfileEditActivity] activity
     */
    private fun goEditProfile() {
        val intent = Intent(this, ProfileEditActivity::class.java).apply {
            putExtra("firstrun", true)
        }
        startActivity(intent)
    }

    companion object {
        /**
         * Activity Tag for logs
         */
        private const val TAG = "ProfileCheck"
    }
}
