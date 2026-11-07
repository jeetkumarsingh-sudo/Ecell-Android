package dcrustm.ecell.mobile.ui.onboarding.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dcrustm.ecell.mobile.R
import dcrustm.ecell.mobile.ui.onboarding.welcome.OptimizedResourceImage

@Composable
fun SignInScreen(
    modifier: Modifier = Modifier,
    onContinueWithGoogleClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start,
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        OptimizedResourceImage(
            resourceId = R.drawable.team,
            contentDescription = "The ecell team with the VC",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(end = 20.dp)
                .clip(RoundedCornerShape(bottomEnd = 60.dp)),
            maxDimensionPx = 900
        )

        Image(
            painter = painterResource(R.drawable.logo_color),
            contentDescription = "Official ECell Logo",
            modifier = Modifier
                .size(160.dp)
                .padding(horizontal = 20.dp)
        )

        Text(
            text = "Start your journey today",
            textAlign = TextAlign.Start,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .width(240.dp)
                .padding(bottom = 20.dp)
                .padding(horizontal = 20.dp)
        )

        Text(
            text = "Sign in with your Google account to get started",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        Spacer(modifier.height(32.dp))

        Button(
            onClick = onContinueWithGoogleClick,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black
            ),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.9f)
                .height(54.dp)
                .padding(horizontal = 20.dp)
        ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Connecting to Google…")
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceAround,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_google),
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Text("Continue with Google", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}
