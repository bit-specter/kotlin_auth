package cloud.meis

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get data passed from MainActivity
        val userName = intent.getStringExtra("USER_NAME") ?: "Guest"

        setContent {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val view = LayoutInflater.from(context).inflate(R.layout.activity_home, null)

                    // Find Views
                    val tvTitle = view.findViewById<TextView>(R.id.tvTitle)
                    val tvSubtitle = view.findViewById<TextView>(R.id.tvSubtitle)
                    val btnLogout = view.findViewById<Button>(R.id.btnLogout)

                    // Update UI with Login Data
                    tvTitle.text = "Selamat Datang,"
                    tvSubtitle.text = userName

                    // Animation: Slide up gracefully
                    view.translationY = 50f
                    view.alpha = 0f
                    view.animate()
                        .translationY(0f)
                        .alpha(1f)
                        .setDuration(600)
                        .start()

                    btnLogout?.setOnClickListener {
                        val intent = Intent(context, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }

                    view
                }
            )
        }
    }
}