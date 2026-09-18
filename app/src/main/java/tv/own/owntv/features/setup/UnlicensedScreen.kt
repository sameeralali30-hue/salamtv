package tv.own.owntv.features.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.R
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * لوحة المشغّل بلا رخصة سارية — شاشة تحجب كلّ شيء (ADR-5).
 *
 * تُعرض حين ترفض [SubscriptionWatcher.licensed] تذكرة المركز أو تغيب. لا زرّ ولا مخرج:
 * ليس للمشترك ما يفعله، والمشغّل هو من يجدّد رخصته. الرسالة تسمّي المشغّل لا SalamTV
 * كي لا يظنّ المشترك أنّ العطل في تطبيقه.
 */
@Composable
fun UnlicensedScreen(modifier: Modifier = Modifier) {
    val colors = OwnTVTheme.colors
    Box(modifier.fillMaxSize().background(colors.background), contentAlignment = Alignment.Center) {
        tv.own.owntv.features.salamtv.AuroraBackground()
        Column(
            modifier = Modifier
                .width(460.dp)
                .background(colors.surface.copy(alpha = 0.72f), RoundedCornerShape(24.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            tv.own.owntv.features.salamtv.BrandGradientTitle(size = 40)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.salamtv_unlicensed_title),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.salamtv_unlicensed_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
