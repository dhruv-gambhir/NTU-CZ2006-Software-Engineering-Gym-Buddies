package sg.edu.ntu.scse.cz2006.gymbuddies

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_profile_edit.*
import kotlinx.android.synthetic.main.row_pref_days.*
import sg.edu.ntu.scse.cz2006.gymbuddies.datastruct.User
import sg.edu.ntu.scse.cz2006.gymbuddies.tasks.GetProfilePicFromFirebaseAuth
import sg.edu.ntu.scse.cz2006.gymbuddies.tasks.UpdateFirebaseFirestoreDocument
import sg.edu.ntu.scse.cz2006.gymbuddies.tasks.UploadProfilePic
import sg.edu.ntu.scse.cz2006.gymbuddies.util.InputHelper
import java.util.*
import kotlin.collections.ArrayList

class ProfileEditActivity : AppCompatActivity() {

    private var firstRun = false
    private var profileImage: Bitmap? = null
    private var profileUri: Uri? = null
    private lateinit var uid: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_edit)

        firstRun = intent.getBooleanExtra("firstrun", false)
        Log.d(TAG, "FirstRun: $firstRun")

        // Assume auth always succeeed
        val auth = FirebaseAuth.getInstance().currentUser!!
        uid = auth.uid

        if (firstRun) {
            supportActionBar?.title = "Create New Profile"
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            Log.i(TAG, "Create new profile flow called")
            if (!auth.displayName.isNullOrEmpty()) etName.setText(auth.displayName)
            if (auth.photoUrl != null && auth.photoUrl.toString().toLowerCase(Locale.getDefault()) != "null") {
                // Set Uri of user
                profileUri = auth.photoUrl
                GetProfilePicFromFirebaseAuth(this, object: GetProfilePicFromFirebaseAuth.Callback {
                    override fun onComplete(bitmap: Bitmap?) {
                        // Update image drawable
                        profileImage = bitmap
                        val roundBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap)
                        roundBitmap.isCircular = true
                        profile_pic.setImageDrawable(roundBitmap)
                    }
                }).execute(profileUri)
            } else {
                Log.i(TAG, "No image found, showing default image")
                profile_pic.setImageResource(R.mipmap.ic_launcher) // Default Image
            }
        } else {
            // TODO: Retrieve existing data if not first run from database
        }
        fab.setOnClickListener {
            InputHelper.hideSoftKeyboard(this)
            // Check if uploading image
            if (loadImage.visibility == View.VISIBLE) Snackbar.make(coordinator, "Currently uploading profile picture. Wait for it to finish before saving profile", Snackbar.LENGTH_LONG).show()
            else {
                if (validate()) addOrUpdate()
                else Snackbar.make(coordinator, "Please complete your profile before continuing", Snackbar.LENGTH_LONG).show()
            }
        }
        profile_pic.setOnClickListener {
            val photoIntent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
            startActivityForResult(photoIntent, REQUEST_PROFILE_PIC)
        }
    }

    private fun validate(): Boolean {
        // Check all fields set up and filled
        til_etName.isErrorEnabled = false
        val name = etName.text.toString()
        val prefLocation = location.selectedItem.toString()
        val gender = findViewById<RadioButton>(radio_gender.checkedRadioButtonId).text
        val timeRange = findViewById<RadioButton>(radio_time.checkedRadioButtonId).text
        val selectedDays = getSelectedDays()
        Log.d(TAG, "Validating: \"$name\" | $prefLocation | $gender | $timeRange | Selected Days: ${selectedDays.joinToString(",")} (${selectedDays.size})")

        // Validation
        if (name.isEmpty()) {
            til_etName.error = "Please enter a valid name"
            til_etName.isErrorEnabled = true
            return false
        }
        if (selectedDays.isEmpty()) return false

        return true
    }

    private fun addOrUpdate() {
        Log.i(TAG, "Doing add/update of profile")
        val name = etName.text.toString()
        val prefLocation = location.selectedItem.toString()
        val gender = findViewById<RadioButton>(radio_gender.checkedRadioButtonId).text.toString()
        val timeRange = findViewById<RadioButton>(radio_time.checkedRadioButtonId).text.toString()
        val selectedDays = getSelectedDays()
        if (firstRun) {
            val user = User(name=name, prefLocation = prefLocation, gender = gender, prefTime = timeRange, prefDay = User.PrefDays(selectedDays), profilePicUri = profileUri.toString())
            user.flags.firstRun = false
            val db = FirebaseFirestore.getInstance()
            val ref = db.collection("users").document(uid)
            UpdateFirebaseFirestoreDocument(ref, user, object: UpdateFirebaseFirestoreDocument.Callback {
                override fun onComplete(success: Boolean) {
                    Log.i(TAG, "Insertion Status: $success")
                    if (success) {
                        val userProfileBuilder = UserProfileChangeRequest.Builder().apply {
                            setDisplayName(name)
                            if (user.profilePicUri.isNotEmpty()) setPhotoUri(Uri.parse(user.profilePicUri))
                        }
                        val currentUser = FirebaseAuth.getInstance().currentUser!!
                        Snackbar.make(coordinator, "Creating Profile...", Snackbar.LENGTH_LONG).show()
                        currentUser.updateProfile(userProfileBuilder.build()).addOnCompleteListener {
                            currentUser.reload().addOnCompleteListener {
                                startActivity(Intent(this@ProfileEditActivity, MainActivity::class.java))
                                finish()
                            }
                        }
                    } else {
                        Snackbar.make(coordinator, "An error occurred updating profile", Snackbar.LENGTH_LONG).show()
                    }
                }
            } ).execute()
        } else {
            Snackbar.make(coordinator, "Updating of profile is coming soon", Snackbar.LENGTH_LONG).show()
            // TODO: Update data in firebase if update
        }
    }

    private fun getSelectedDays(): ArrayList<Int> {
        val list = ArrayList<Int>()
        // Add accordingly (1 - Mon, 2 - Tues ... 7 - Sun
        if (cb_day1.isChecked) list.add(1)
        if (cb_day2.isChecked) list.add(2)
        if (cb_day3.isChecked) list.add(3)
        if (cb_day4.isChecked) list.add(4)
        if (cb_day5.isChecked) list.add(5)
        if (cb_day6.isChecked) list.add(6)
        if (cb_day7.isChecked) list.add(7)
        return list
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_PROFILE_PIC && resultCode == Activity.RESULT_OK && data != null) {
            val bitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(data.data as Uri))
            // Upload image into cloud before setting
            loadImage.visibility = View.VISIBLE
            val storage = FirebaseStorage.getInstance()
            val ref = storage.reference.child("profile/${uid}.jpg")
            UploadProfilePic(this, ref, bitmap, object: UploadProfilePic.Callback {
                override fun onSuccess(success: Boolean, imageUri: Uri?) {
                    if (success) {
                        // Update stuff
                        profileImage = bitmap
                        profileUri = imageUri
                        val roundBitmap = RoundedBitmapDrawableFactory.create(resources, bitmap)
                        roundBitmap.isCircular = true
                        profile_pic.setImageDrawable(roundBitmap)
                    }
                    loadImage.visibility = View.GONE
                }
            }).execute()
        }
    }

    override fun onBackPressed() {
        if (firstRun) {
            // Log user out
            finish()
            val logout = Intent(this, LoginChooserActivity::class.java).apply { putExtra("logout", true) }
            startActivity(logout)
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val TAG = "ProfileEdit"
        private const val REQUEST_PROFILE_PIC = 1
    }

}