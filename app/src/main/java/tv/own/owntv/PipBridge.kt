package tv.own.owntv

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * [SALAMTV] جسر «صورة داخل صورة» بين النشاط وقشرة الهاتف.
 *
 * النشاط وحده يستطيع دخول PiP (onUserLeaveHint) ووحده يعرف متى دخل أو خرج؛ والقشرة وحدها تعرف
 * إن كان فيديو يعمل. لا يعرف أحدهما الآخر (النكهات)، فيتبادلان الحالة هنا:
 *  · [wanted]  تضعه القشرة true ما دام مشغّل ملء الشاشة يعمل — فمغادرة التطبيق تدخل PiP.
 *  · [active]  يضعه النشاط حين يتبدّل الوضع — فتخفي القشرة أزرارها وتبقي الفيديو وحده.
 * نكهة التلفاز لا تمسّه فيبقى false ولا يتغيّر سلوكها.
 */
object PipBridge {
    @Volatile var wanted: Boolean = false
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()
    fun setActive(v: Boolean) { _active.value = v }
}
