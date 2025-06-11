package com.hackathon.temantidur.data.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.hackathon.temantidur.domain.AuthRepository
import com.hackathon.temantidur.domain.AuthResult
import com.hackathon.temantidur.domain.User
import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class AuthRepositoryImpl(
    private val db: FirebaseDatabase,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override fun register(username: String, email: String, password: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()
        resultLd.value = AuthResult.Loading

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            resultLd.value = AuthResult.Error("Semua field harus diisi")
            return resultLd
        }

        if (username.length < 3) {
            resultLd.value = AuthResult.Error("Username minimal 3 karakter")
            return resultLd
        }

        if (password.length < 6) {
            resultLd.value = AuthResult.Error("Password minimal 6 karakter")
            return resultLd
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            resultLd.value = AuthResult.Error("Format email tidak valid")
            return resultLd
        }

        checkUsernameAvailability(username) { isAvailable, error ->
            if (error != null) {
                resultLd.value = AuthResult.Error("Gagal memeriksa ketersediaan username: $error")
                return@checkUsernameAvailability
            }

            if (!isAvailable) {
                resultLd.value = AuthResult.Error("Username sudah digunakan")
                return@checkUsernameAvailability
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        if (firebaseUser != null) {
                            // Update display name di Firebase Auth
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(username)
                                .build()

                            firebaseUser.updateProfile(profileUpdates)
                                .addOnCompleteListener { profileTask ->
                                    if (profileTask.isSuccessful) {
                                        // Kirim email verifikasi
                                        firebaseUser.sendEmailVerification()
                                            .addOnCompleteListener { emailTask ->
                                                if (emailTask.isSuccessful) {
                                                    // Simpan data tambahan ke Realtime Database
                                                    saveUserToDatabase(firebaseUser, username, resultLd)
                                                } else {
                                                    Log.e("AuthRepository", "Gagal mengirim email verifikasi", emailTask.exception)
                                                    resultLd.value = AuthResult.Error("Gagal mengirim email verifikasi: ${emailTask.exception?.message}")
                                                }
                                            }
                                    } else {
                                        Log.e("AuthRepository", "Gagal mengupdate profil", profileTask.exception)
                                        resultLd.value = AuthResult.Error("Gagal mengupdate profil: ${profileTask.exception?.message}")
                                    }
                                }
                        } else {
                            resultLd.value = AuthResult.Error("Gagal membuat akun")
                        }
                    } else {
                        val errorMessage = when (task.exception?.message) {
                            "The email address is already in use by another account." -> "Email sudah terdaftar"
                            "The email address is badly formatted." -> "Format email tidak valid"
                            "The given password is invalid. [ Password should be at least 6 characters ]" -> "Password minimal 6 karakter"
                            else -> task.exception?.message ?: "Registrasi gagal"
                        }
                        Log.e("AuthRepository", "Registrasi gagal", task.exception)
                        resultLd.value = AuthResult.Error(errorMessage)
                    }
                }
        }

        return resultLd
    }

    override fun login(email: String, password: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()
        resultLd.value = AuthResult.Loading

        if (email.isEmpty() || password.isEmpty()) {
            resultLd.value = AuthResult.Error("Email dan password harus diisi")
            return resultLd
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            resultLd.value = AuthResult.Error("FORMAT_EMAIL_INVALID")
            return resultLd
        }

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val firebaseUser = auth.currentUser
                    if (firebaseUser != null) {
                        if (!firebaseUser.isEmailVerified) {
                            getUserFromDatabase(firebaseUser.uid) { user ->
                                if (user != null) {
                                    updateLastLoginTime(firebaseUser.uid)
                                    resultLd.value = AuthResult.Success(
                                        "Login berhasil! Namun email Anda belum diverifikasi. Silakan cek email untuk verifikasi.",
                                        user
                                    )
                                } else {
                                    resultLd.value = AuthResult.Error("DATA_USER_NOT_FOUND")
                                }
                            }
                        } else {
                            getUserFromDatabase(firebaseUser.uid) { user ->
                                if (user != null) {
                                    updateLastLoginTime(firebaseUser.uid)
                                    resultLd.value = AuthResult.Success(
                                        "Selamat datang kembali, ${user.username}!",
                                        user
                                    )
                                } else {
                                    resultLd.value = AuthResult.Error("DATA_USER_NOT_FOUND")
                                }
                            }
                        }
                    } else {
                        resultLd.value = AuthResult.Error("LOGIN_FAILED_UNKNOWN")
                    }
                } else {
                    val exception = task.exception

                    Log.e("AuthRepository", "Login exception: ${exception?.message}", exception)
                    Log.e("AuthRepository", "Exception class: ${exception?.javaClass?.name}")
                    if (exception is FirebaseAuthException) {
                        Log.e("AuthRepository", "Firebase errorCode: ${exception.errorCode}")
                    }

                    val errorCode = when (exception) {
                        is FirebaseAuthInvalidUserException -> {
                            when (exception.errorCode) {
                                "ERROR_USER_NOT_FOUND" -> "EMAIL_NOT_FOUND"
                                "ERROR_USER_DISABLED" -> "ACCOUNT_DISABLED"
                                else -> "LOGIN_FAILED_UNKNOWN"
                            }
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            when (exception.errorCode) {
                                "ERROR_INVALID_EMAIL" -> "EMAIL_FORMAT_INVALID"
                                "ERROR_INVALID_CREDENTIAL" -> "CREDENTIAL_INVALID"
                                else -> "LOGIN_FAILED_UNKNOWN"
                            }
                        }
                        is FirebaseAuthException -> {
                            when (exception.errorCode) {
                                "ERROR_TOO_MANY_REQUESTS" -> "TOO_MANY_ATTEMPTS"
                                else -> "LOGIN_FAILED_UNKNOWN"
                            }
                        }
                        else -> {
                            if (exception?.message?.contains("network", true) == true) {
                                "NETWORK_ERROR"
                            } else {
                                "LOGIN_FAILED_UNKNOWN"
                            }
                        }
                    }

                    resultLd.value = AuthResult.Error(errorCode)
                }
            }

        return resultLd
    }

    override fun verifyOtp(tempUserId: String, otp: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()
        resultLd.value = AuthResult.Error("Metode verifikasi OTP tidak diperlukan dengan Firebase Auth")
        return resultLd
    }

    override fun resendOtp(tempUserId: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()

        // Kirim ulang email verifikasi
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        resultLd.value = AuthResult.Success("Email verifikasi berhasil dikirim ulang")
                    } else {
                        resultLd.value = AuthResult.Error("Gagal mengirim ulang email verifikasi")
                    }
                }
        } else {
            resultLd.value = AuthResult.Error("Pengguna tidak ditemukan")
        }

        return resultLd
    }

    // Implement the missing changeEmail method from the interface
    override fun changeEmail(newEmail: String): LiveData<AuthResult> {
        return changeEmailWithVerification(newEmail)
    }

    override fun changePassword(currentPassword: String, newPassword: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()
        resultLd.value = AuthResult.Loading

        val user = auth.currentUser
        if (user == null) {
            resultLd.value = AuthResult.Error("Pengguna belum login")
            return resultLd
        }

        if (newPassword.length < 6) {
            resultLd.value = AuthResult.Error("Password baru minimal 6 karakter")
            return resultLd
        }

        // Re-authenticate user before changing password
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)
        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                resultLd.value = AuthResult.Success("Password berhasil diubah")
                            } else {
                                Log.e("AuthRepository", "Gagal mengubah password", updateTask.exception)
                                resultLd.value = AuthResult.Error(updateTask.exception?.message ?: "Gagal mengubah password")
                            }
                        }
                } else {
                    val errorMessage = when (reauthTask.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Password saat ini salah."
                        else -> reauthTask.exception?.message ?: "Gagal re-autentikasi"
                    }
                    Log.e("AuthRepository", "Gagal re-autentikasi", reauthTask.exception)
                    resultLd.value = AuthResult.Error(errorMessage)
                }
            }
        return resultLd
    }

    override fun changeUsername(newUsername: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()
        resultLd.value = AuthResult.Loading

        val user = auth.currentUser
        if (user == null) {
            resultLd.value = AuthResult.Error("Pengguna belum login")
            return resultLd
        }

        // Validasi input
        if (newUsername.isEmpty()) {
            resultLd.value = AuthResult.Error("Username tidak boleh kosong")
            return resultLd
        }

        if (newUsername.length < 3) {
            resultLd.value = AuthResult.Error("Username minimal 3 karakter")
            return resultLd
        }

        if (newUsername.length > 20) {
            resultLd.value = AuthResult.Error("Username maksimal 20 karakter")
            return resultLd
        }

        // Validasi karakter username
        if (!newUsername.matches(Regex("^[a-zA-Z0-9._]+$"))) {
            resultLd.value = AuthResult.Error("Username hanya boleh mengandung huruf, angka, underscore, dan titik")
            return resultLd
        }

        Log.d("AuthRepository", "Changing username to: $newUsername")

        // Cek ketersediaan username baru
        checkUsernameAvailability(newUsername) { isAvailable, error ->
            if (error != null) {
                resultLd.value = AuthResult.Error("Gagal memeriksa ketersediaan username: $error")
                return@checkUsernameAvailability
            }

            if (!isAvailable) {
                resultLd.value = AuthResult.Error("Username sudah digunakan")
                return@checkUsernameAvailability
            }

            // Dapatkan username lama untuk menghapus mapping
            getUserFromDatabase(user.uid) { currentUser ->
                if (currentUser == null) {
                    resultLd.value = AuthResult.Error("Data pengguna tidak ditemukan")
                    return@getUserFromDatabase
                }

                val oldUsername = currentUser.username

                // Update display name di Firebase Auth
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build()

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            // Update username di database dan mapping
                            updateUsernameInDatabase(user.uid, oldUsername, newUsername, resultLd)
                        } else {
                            Log.e("AuthRepository", "Gagal mengupdate profil Firebase Auth", profileTask.exception)
                            resultLd.value = AuthResult.Error("Gagal mengupdate profil: ${profileTask.exception?.message}")
                        }
                    }
            }
        }

        return resultLd
    }

    private fun updateUsernameInDatabase(
        uid: String,
        oldUsername: String,
        newUsername: String,
        resultLd: MutableLiveData<AuthResult>
    ) {
        Log.d("AuthRepository", "Updating username in database from '$oldUsername' to '$newUsername'")

        val updates = mutableMapOf<String, Any?>()

        // Update username di tabel users
        updates["users/$uid/username"] = newUsername

        // Hapus mapping username lama
        updates["user_usernames/${oldUsername.lowercase()}"] = null

        // Tambah mapping username baru
        updates["user_usernames/${newUsername.lowercase()}"] = uid

        db.reference.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("AuthRepository", "Username updated successfully in database")
                resultLd.value = AuthResult.Success("Username berhasil diubah menjadi $newUsername")
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Error updating username in database", e)
                resultLd.value = AuthResult.Error("Gagal mengubah username: ${e.message}")
            }
    }

    // Method tambahan untuk logout
    fun logout(): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()

        auth.signOut()
        resultLd.value = AuthResult.Success("Logout berhasil")

        return resultLd
    }

    private fun checkUsernameAvailability(username: String, callback: (Boolean, String?) -> Unit) {
        Log.d("AuthRepository", "Checking username availability for: $username")

        val usernameRef = db.getReference("user_usernames").child(username.lowercase())

        usernameRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val usernameExists = snapshot.exists()
                    Log.d("AuthRepository", "Username '$username' exists: $usernameExists")
                    callback(!usernameExists, null)
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Error checking username availability", e)
                    callback(false, e.message)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("AuthRepository", "Database error when checking username: ${error.message}")
                callback(false, error.message)
            }
        })
    }

    private fun saveUserToDatabase(
        firebaseUser: FirebaseUser,
        username: String,
        resultLd: MutableLiveData<AuthResult>
    ) {
        Log.d("AuthRepository", "Saving user to database: ${firebaseUser.uid}")

        val user = User(
            uid = firebaseUser.uid,
            username = username,
            email = firebaseUser.email ?: "",
            createdAt = System.currentTimeMillis(),
            isEmailVerified = firebaseUser.isEmailVerified,
            profileImageUrl = firebaseUser.photoUrl?.toString() ?: "",
            lastLoginAt = 0L
        )

        val userMap = mapOf(
            "uid" to user.uid,
            "username" to user.username,
            "email" to user.email,
            "createdAt" to user.createdAt,
            "isEmailVerified" to user.isEmailVerified,
            "profileImageUrl" to user.profileImageUrl,
            "lastLoginAt" to user.lastLoginAt
        )

        val updates = mutableMapOf<String, Any>()
        updates["users/${firebaseUser.uid}"] = userMap
        updates["user_usernames/${username.lowercase()}"] = firebaseUser.uid

        db.reference.updateChildren(updates)
            .addOnSuccessListener {
                Log.d("AuthRepository", "User data and username mapping saved successfully")
                resultLd.value = AuthResult.OtpRequired(
                    "Registrasi berhasil! Email verifikasi telah dikirim ke ${user.email}. Silakan cek email Anda dan klik link verifikasi.",
                    firebaseUser.uid
                )
            }
            .addOnFailureListener { e ->
                Log.e("AuthRepository", "Error saving user data", e)
                resultLd.value = AuthResult.Error("Gagal menyimpan data pengguna: ${e.message}")
            }
    }

    private fun getUserFromDatabase(uid: String, callback: (User?) -> Unit) {
        db.getReference("users").child(uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    callback(user)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("AuthRepository", "Error getting user data", error.toException())
                    callback(null)
                }
            })
    }

    private fun updateLastLoginTime(uid: String) {
        db.getReference("users").child(uid).child("lastLoginAt")
            .setValue(System.currentTimeMillis())
    }

    // Method alternative untuk change email jika ingin tetap menggunakan ViewModel
    override fun changeEmailWithVerification(newEmail: String): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()
        resultLd.value = AuthResult.Loading

        val user = auth.currentUser
        if (user == null) {
            resultLd.value = AuthResult.Error("Pengguna belum login")
            return resultLd
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
            resultLd.value = AuthResult.Error("Format email tidak valid")
            return resultLd
        }

        if (user.email == newEmail) {
            resultLd.value = AuthResult.Error("Email baru sama dengan email saat ini")
            return resultLd
        }

        Log.d("AuthRepository", "Sending email verification for new email: $newEmail")

        user.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthRepository", "Email verification sent successfully")
                    resultLd.value = AuthResult.Success(
                        "Email verifikasi telah dikirim ke $newEmail. Silakan buka email Anda dan klik tautan verifikasi untuk menyelesaikan perubahan email."
                    )
                } else {
                    val errorMessage = when (task.exception?.message) {
                        "The email address is already in use by another account." -> "Email sudah digunakan oleh akun lain"
                        "The email address is badly formatted." -> "Format email tidak valid"
                        else -> task.exception?.message ?: "Gagal mengirim email verifikasi"
                    }
                    Log.e("AuthRepository", "Failed to send email verification", task.exception)
                    resultLd.value = AuthResult.Error(errorMessage)
                }
            }
        return resultLd
    }

    // Method untuk memeriksa apakah email sudah berubah setelah verifikasi
    override fun checkEmailUpdate(): LiveData<AuthResult> {
        val resultLd = MutableLiveData<AuthResult>()

        val user = auth.currentUser
        if (user == null) {
            resultLd.value = AuthResult.Error("Pengguna belum login")
            return resultLd
        }

        // Reload user untuk mendapatkan data terbaru
        user.reload().addOnCompleteListener { reloadTask ->
            if (reloadTask.isSuccessful) {
                val currentEmail = user.email

                // Update email di database jika berubah
                db.getReference("users").child(user.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val savedEmail = snapshot.child("email").value as? String

                            if (currentEmail != savedEmail && currentEmail != null) {
                                // Update email di database
                                db.getReference("users").child(user.uid).child("email")
                                    .setValue(currentEmail)
                                    .addOnSuccessListener {
                                        resultLd.value = AuthResult.Success("Email berhasil diperbarui")
                                    }
                                    .addOnFailureListener { e ->
                                        resultLd.value = AuthResult.Error("Gagal memperbarui email di database: ${e.message}")
                                    }
                            } else {
                                resultLd.value = AuthResult.Success("Email sudah terbaru")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            resultLd.value = AuthResult.Error("Gagal memeriksa status email: ${error.message}")
                        }
                    })
            } else {
                resultLd.value = AuthResult.Error("Gagal memuat ulang data pengguna")
            }
        }

        return resultLd
    }

    override fun getCurrentEmail(): LiveData<AuthResult> {
        val result = MutableLiveData<AuthResult>()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            result.value = AuthResult.Error("User tidak ditemukan")
            return result
        }

        currentUser.reload()
            .addOnCompleteListener { reloadTask ->
                if (reloadTask.isSuccessful) {
                    val currentEmail = FirebaseAuth.getInstance().currentUser?.email
                    if (currentEmail != null) {
                        result.value = AuthResult.Success(currentEmail)
                    } else {
                        result.value = AuthResult.Error("Email tidak tersedia")
                    }
                } else {
                    val fallbackEmail = currentUser.email
                    if (fallbackEmail != null) {
                        result.value = AuthResult.Success(fallbackEmail)
                    } else {
                        result.value = AuthResult.Error("Gagal mengambil email: ${reloadTask.exception?.message}")
                    }
                }
            }

        return result
    }
}