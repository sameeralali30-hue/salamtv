package tv.own.owntv.features.setup

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import tv.own.owntv.BuildConfig
import java.io.IOException

/**
 * Subscriber sign-in against the SalamTV panel.
 *
 * The subscriber types a username and a password — never a server address. This call is the only
 * one that goes to the built-in endpoint; it answers "which host do you belong to?", and everything
 * afterwards (catalogue, playback) goes straight to the host it names.
 *
 * ═══ Why a bootstrap call and not `server_info` ═══
 * `XtreamClient.base(s)` builds every URL from the stored source URL, and the client never reads
 * the `server_info` block a panel returns. So a per-subscriber host cannot be advertised in the
 * Xtream response the way other panels do it — it has to be resolved once, here, before the source
 * row is written.
 *
 * ═══ Three decisions ═══
 *
 * ① The panel's Arabic message wins over any string in the app.
 *    "Your subscription expired — contact your reseller" is a business message that changes with
 *    the operator's policy. Baking English equivalents here would mean two sources of truth, and
 *    the one the subscriber sees would be the stale one.
 *
 * ② A transport failure is never reported as bad credentials.
 *    Telling someone their password is wrong when the server is simply unreachable sends them to
 *    the reseller for a problem the reseller cannot see.
 *
 * ③ Nothing here is logged but the outcome.
 *    No username, no password, no host — a bug report should never carry a subscriber's account.
 */
class SubscriberLoginClient(
    private val client: OkHttpClient,
    private val clientId: tv.own.owntv.core.metadata.OwnTVClientId,
) {

    /** What the panel answered. [base] is the Xtream source URL to store for this subscriber. */
    data class Account(
        val base: String,
        val username: String,
        val node: String,
        val plan: String,
        val status: String,
        val expiresAt: Long?,
        val maxConnections: Int,
        val devices: Int,
        val deviceLimit: Int,
        val resetHours: Int,
    )

    /** A refusal the subscriber can act on: [message] is the panel's own wording. */
    class LoginException(val code: String, message: String) : IOException(message)

    suspend fun login(username: String, password: String): Account = withContext(Dispatchers.IO) {
        // The install id already in core: 128 random bits, minted on first use, derived from
        // nothing — not ANDROID_ID, not hardware, not the account. It identifies an *install*,
        // which is the only device identity Android still allows: the MAC address has read back
        // as 02:00:00:00:00:00 for every app since Android 6, and is per-network randomised since
        // Android 10. The panel binds the subscription to it and answers with a token that then
        // rides inside every request path.
        val installId = runCatching { clientId.get() }.getOrDefault("")

        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("device_id", installId)
            .toString()
            .toRequestBody(JSON)

        val request = Request.Builder()
            .url(BuildConfig.SALAMTV_LOGIN_URL)
            .header("Accept", "application/json")
            .post(payload)
            .build()

        val text = try {
            client.newCall(request).execute().use { it.body.string() }
        } catch (e: IOException) {
            // ② unreachable ≠ wrong password.
            Log.w(TAG, "login transport failure: ${e.javaClass.simpleName}")
            throw LoginException(CODE_OFFLINE, "")
        }

        val json = runCatching { JSONObject(text) }.getOrElse {
            Log.w(TAG, "login: malformed response")
            throw LoginException(CODE_OFFLINE, "")
        }

        if (!json.optBoolean("ok")) {
            val code = json.optString("error").ifBlank { "unknown" }
            // ① the panel's message, verbatim.
            val msg = json.optString("message")
            Log.w(TAG, "login refused: $code")
            throw LoginException(code, msg)
        }

        val base = json.optString("base").trimEnd('/')
        if (base.isBlank()) throw LoginException(CODE_OFFLINE, "")

        Log.i(TAG, "login ok")   // ③ outcome only
        Account(
            base = base,
            username = json.optString("username").ifBlank { username },
            node = json.optString("node"),
            plan = json.optString("plan"),
            status = json.optString("status"),
            expiresAt = json.optLong("exp_date", 0L).takeIf { it > 0 },
            maxConnections = json.optInt("max_connections", 1),
            devices = json.optInt("devices", 0),
            deviceLimit = json.optInt("device_limit", 1),
            resetHours = json.optInt("reset_hours", 48),
        )
    }


    /**
     * Release this device's binding on the panel.
     *
     * Removing the playlist locally is not enough: the binding lives on the server, so a subscriber
     * who "signed out" and moved to a new phone would still be refused there until the reset window
     * elapsed. Signing out has to free the seat, or it is a trap rather than a feature.
     *
     * Best-effort by design — a sign-out must never be blocked by a network failure. The local
     * playlist goes either way; the worst case is a stale binding the subscriber can clear with
     * "Free my devices", or the operator from the panel.
     */
    /**
     * يفعّل رمزاً اشتراه المشترك من وكيل.
     *
     * الرمز يُضيف أياماً إلى حسابٍ قائم، فلا معنى له بلا بيانات دخول —
     * ولذلك ترسل الشاشة الثلاثة معاً.
     *
     * تعيد رسالة اللوحة كما هي، نجحت أم فشلت: هي التي تعرف الفرق بين
     * «رمز غير صحيح» و«استعملتَه من قبل»، والتطبيق لا يملك أن يخمّنه.
     */
    suspend fun redeem(username: String, password: String, code: String): Result<String> =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("username", username)
                .put("password", password)
                .put("code", code)
                .toString()
                .toRequestBody(JSON)

            val url = BuildConfig.SALAMTV_LOGIN_URL.substringBeforeLast('/') + "/app_redeem.php"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .post(payload)
                .build()

            val text = try {
                client.newCall(request).execute().use { it.body.string() }
            } catch (e: IOException) {
                Log.w(TAG, "redeem transport failure: ${e.javaClass.simpleName}")
                return@withContext Result.failure(LoginException(CODE_OFFLINE, ""))
            }

            val json = runCatching { JSONObject(text) }.getOrElse {
                return@withContext Result.failure(LoginException(CODE_OFFLINE, ""))
            }
            val msg = json.optString("message")
            if (json.optBoolean("ok")) {
                Log.i(TAG, "redeem ok")
                Result.success(msg)
            } else {
                Log.w(TAG, "redeem refused")
                Result.failure(LoginException("refused", msg))
            }
        }

    /**
     * إنشاء حساب من داخل التطبيق، ومعه الرمز إن كان بيده.
     *
     * ⚠ الحساب والرمز شيئان: الرمز حاملٌ لقيمة لا يعرف صاحبه، والأيام تُضاف
     *   إلى حساب. فمن اشترى رمزاً ولا حساب له كان عليه أن يفتح المتصفّح
     *   ويسجّل هناك ثمّ يعود — ومن لم يفهم ذلك أدخل اسماً لم يُنشئه بعد،
     *   فقيل له «اسم المستخدم أو كلمة المرور غير صحيحة»، وهي رسالة صادقة
     *   لا تدلّه على شيء.
     *
     * اللوحة تُنشئ وتُفعّل في طلب واحد، وتُبقي الحساب إن فشل الرمز وحده —
     * فلا يفقد الاسم الذي اختاره بسبب حرفٍ أخطأ فيه.
     *
     * @return رسالة اللوحة عند النجاح؛ ونصّها هو ما يُعرض كما هو عند الرفض.
     */
    suspend fun register(
        username: String,
        password: String,
        code: String,
    ): Result<String> = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("code", code)
            .toString()
            .toRequestBody(JSON)

        val url = BuildConfig.SALAMTV_LOGIN_URL.substringBeforeLast('/') + "/app_register.php"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(payload)
            .build()

        val text = try {
            client.newCall(request).execute().use { it.body.string() }
        } catch (e: IOException) {
            Log.w(TAG, "register transport failure: ${e.javaClass.simpleName}")
            return@withContext Result.failure(LoginException(CODE_OFFLINE, ""))
        }

        val json = runCatching { JSONObject(text) }.getOrElse {
            return@withContext Result.failure(LoginException(CODE_OFFLINE, ""))
        }
        val msg = json.optString("message")
        if (json.optBoolean("ok")) {
            Log.i(TAG, "register ok (redeemed=${json.optBoolean("redeemed")})")
            Result.success(msg)
        } else {
            Log.w(TAG, "register refused")
            Result.failure(LoginException("refused", msg))
        }
    }

    /**
     * حالة الاشتراك: ما يُعرض للمشترك، وما يُقاس به تغيّر خطّته.
     *
     * [Status.rev] بصمة تجمع الخطّة ونهاية الاشتراك وحالة التفعيل وسقف
     * الجودة. تغيُّرها وحده يعني أنّ الكتالوج لم يعد صحيحاً — فيُعاد بناؤه.
     * وثباتها يعني ألّا شيء يُفعل، وهو الغالب: ردٌّ في مئتَي بايت بدل سحب
     * مئتَي قناة كلّ بضع دقائق لاكتشاف أنّ شيئاً لم يتغيّر.
     *
     * يعيد null عند أيّ تعذّر — لأنّ المتصل يفرّق بين «تغيّر شيء» و«لا
     * أدري»، ولا يجوز أن يُفهم انقطاعُ الشبكة تغييراً في الخطّة.
     */
    data class Status(
        val rev: String,
        val username: String,
        val state: String,          // active | expired | pending | disabled
        val active: Boolean,
        val daysLeft: Int,          // -1 = اشتراك مفتوح
        val planName: String,
        val maxQuality: Int,        // 0 = بلا حدّ
        val devices: Int,
        /** بالثواني منذ الحقبة، أو null. اللوحة ترسل رقماً لا نصّاً: نصّ بلا
         *  منطقة زمنية يُقرأ على الجهاز بمنطقته فيتقدّم الانتهاء أو يتأخّر. */
        val subStart: Long?,
        val subEnd: Long?,
    )

    suspend fun account(username: String, password: String): Status? = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody(JSON)

        val url = BuildConfig.SALAMTV_LOGIN_URL.substringBeforeLast('/') + "/app_account.php"
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(payload)
            .build()

        return@withContext runCatching {
            client.newCall(request).execute().use { resp ->
                val j = JSONObject(resp.body.string())
                if (!j.optBoolean("ok")) return@use null
                Status(
                    rev = j.optString("rev"),
                    username = j.optString("username"),
                    state = j.optString("state"),
                    active = j.optBoolean("active"),
                    daysLeft = j.optInt("days_left"),
                    planName = j.optString("plan_name"),
                    maxQuality = j.optInt("max_quality"),
                    devices = j.optInt("devices", 1),
                    subStart = j.optLong("sub_start").takeIf { it > 0 },
                    subEnd = j.optLong("sub_end").takeIf { it > 0 },
                )
            }
        }.getOrElse {
            Log.w(TAG, "account check failed: ${it.javaClass.simpleName}")
            null
        }
    }

    suspend fun logout(username: String, password: String): Boolean = withContext(Dispatchers.IO) {
        val installId = runCatching { clientId.get() }.getOrDefault("")
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .put("device_id", installId)
            .toString()
            .toRequestBody(JSON)

        val url = BuildConfig.SALAMTV_LOGIN_URL.replace("app_login.php", "app_logout.php")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(payload)
            .build()

        return@withContext runCatching {
            client.newCall(request).execute().use { resp ->
                JSONObject(resp.body.string()).optBoolean("ok")
            }
        }.getOrElse {
            Log.w(TAG, "logout failed; local playlist removed anyway")
            false
        }
    }

    /**
     * Clear every device on the account, for the subscriber who changed phones and can no longer
     * sign out from the old one. Rate-limited on the panel (48 h by default) — the refusal text is
     * the panel's and is shown as-is.
     */
    suspend fun freeDevices(username: String, password: String): String? = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("username", username)
            .put("password", password)
            .toString()
            .toRequestBody(JSON)

        val url = BuildConfig.SALAMTV_LOGIN_URL.replace("app_login.php", "app_device_reset.php")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .post(payload)
            .build()

        return@withContext runCatching {
            client.newCall(request).execute().use { resp ->
                val j = JSONObject(resp.body.string())
                if (j.optBoolean("ok")) null else j.optString("message")
            }
        }.getOrElse { "" }   // blank ⇒ caller shows its offline wording
    }

    companion object {
        private const val TAG = "SalamTVLogin"
        private val JSON = "application/json".toMediaType()

        /** No network, no answer, or an answer we could not parse. */
        const val CODE_OFFLINE = "offline"

        /** The subscription is already bound to another device. */
        const val CODE_DEVICE_LIMIT = "device_limit"
    }
}
