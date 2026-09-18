package tv.own.owntv.phone

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import tv.own.owntv.ui.components.OwnTVIcon
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * لبنات قشرة الهاتف — لمسٌ لا ريموت.
 *
 * الفارق الجوهريّ عن مكوّنات التلفاز: لا [tv.own.owntv.ui.components.FocusableSurface] ولا حلقات
 * تركيز؛ اللمسة هي الحدث، والمساحة اللمسيّة ≥ 44dp، والنصّ يُقصّ بنقاط لا يلتفّ حرفاً حرفاً
 * (ما رأيناه في الشعار العموديّ على الهاتف).
 */

/** مقاسات الهاتف — أضيق من [tv.own.owntv.ui.theme.Dimens] التي كُتبت لمسافة الأريكة. */
object PhoneDimens {
    val Gutter = 16.dp
    val RowGap = 10.dp
    val Corner = 14.dp
    val Logo = 48.dp
    val Touch = 48.dp
    val BottomBar = 64.dp
}

/** رأس شاشة: عنوان كبير في السطر الأوّل مع محتوى اختياريّ عن اليمين/اليسار. */
@Composable
fun PhoneHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = PhoneDimens.Gutter, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = OwnTVTheme.colors.onSurface)
        trailing?.invoke()
    }
}

/** شرائح أفقيّة (الأقسام): المختارة ممتلئة باللون الأساسيّ. */
@Composable
fun <T> PhoneChips(
    items: List<T>,
    selected: (T) -> Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
) {
    val colors = OwnTVTheme.colors
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = PhoneDimens.Gutter),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = key) { item ->
            val on = selected(item)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (on) colors.primary else colors.surfaceContainerHigh)
                    .clickable { onSelect(item) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    label(item),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) colors.onPrimary else colors.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** صفّ قناة: شعار، اسم، سطر ثانٍ اختياريّ (الآن يُعرض)، وقلب للمفضّلة. */
@Composable
fun PhoneChannelRow(
    name: String,
    logoUrl: String?,
    subtitle: String?,
    favorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    number: Int? = null,
) {
    val colors = OwnTVTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PhoneDimens.Corner))
            .background(colors.surfaceContainerLow)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PhoneLogo(logoUrl, name)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (number != null) "$number · $name" else name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Box(
            modifier = Modifier.size(PhoneDimens.Touch).clip(RoundedCornerShape(999.dp)).clickable(onClick = onToggleFavorite),
            contentAlignment = Alignment.Center,
        ) {
            OwnTVIcon(OwnTVIcon.FAVORITE, tint = if (favorite) colors.favorite else colors.onSurfaceVariant, modifier = Modifier.size(22.dp), filled = favorite)
        }
    }
}

@Composable
fun PhoneLogo(url: String?, name: String, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = PhoneDimens.Logo) {
    val colors = OwnTVTheme.colors
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(10.dp)).background(colors.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = null, modifier = Modifier.size(size - 8.dp))
        } else {
            Text(name.take(1), style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant)
        }
    }
}

/** عنوان قسم داخل الشاشة الرئيسيّة. */
@Composable
fun PhoneSectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = OwnTVTheme.colors.onSurface,
        modifier = modifier.padding(horizontal = PhoneDimens.Gutter, vertical = 8.dp),
    )
}

/** بطاقة إجراء في «حسابي»: أيقونة، عنوان، وصف — لمسة واحدة. */
@Composable
fun PhoneActionCard(
    icon: OwnTVIcon,
    title: String,
    desc: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    val colors = OwnTVTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(PhoneDimens.Corner))
            .background(if (accent) colors.primaryContainer else colors.surfaceContainerLow)
            .then(if (accent) Modifier.border(1.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(PhoneDimens.Corner)) else Modifier)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OwnTVIcon(icon, tint = if (accent) colors.onPrimaryContainer else colors.primary, modifier = Modifier.size(26.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = if (accent) colors.onPrimaryContainer else colors.onSurface)
            if (!desc.isNullOrBlank()) Text(desc, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        OwnTVIcon(OwnTVIcon.CHEVRON, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

/** خطّ فاصل رفيع. */
@Composable
fun PhoneDivider(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(OwnTVTheme.colors.outlineVariant.copy(alpha = 0.6f)))
}

/** لون خلفيّة المشغّل — أسود صريح لا لون السطح. */
val PhoneVideoBlack = Color.Black
