package com.dpcoutreach.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val preferences by lazy { getSharedPreferences("dpc-outreach", MODE_PRIVATE) }

    private lateinit var backendUrlInput: EditText
    private lateinit var dpcIdInput: EditText
    private lateinit var phoneInput: EditText
    private lateinit var timestampInput: EditText
    private lateinit var durationInput: EditText
    private lateinit var outcomeSpinner: Spinner
    private lateinit var submitCallButton: Button
    private lateinit var statusText: TextView
    private lateinit var contactsContainer: LinearLayout

    private var pendingCall: PendingCall? = null
    private var pendingDialerCall: LoggedCall? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingCall = savedInstanceState?.let {
            val phone = it.getString(STATE_PENDING_PHONE)
            val timestamp = it.getLong(STATE_PENDING_TIMESTAMP, -1L)
            if (phone != null && timestamp >= 0L) PendingCall(phone, timestamp) else null
        } ?: preferences.getString(KEY_PENDING_PHONE, null)?.let { phone ->
            PendingCall(phone, preferences.getLong(KEY_PENDING_TIMESTAMP, 0L))
        }
        setContentView(createContent())
        setStatus("Enter the backend URL and DPC ID, then load the contact directory.")
    }

    override fun onSaveInstanceState(outState: Bundle) {
        pendingCall?.let {
            outState.putString(STATE_PENDING_PHONE, it.phoneNumber)
            outState.putLong(STATE_PENDING_TIMESTAMP, it.startedAtMillis)
        }
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DIALER && pendingCall != null) {
            checkCallLog()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_LOG &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            checkCallLog()
        } else if (requestCode == REQUEST_CALL_LOG) {
            setStatus("Call-log permission was denied. Enter the call timestamp and duration manually.")
        }
    }

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun createContent(): View {
        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(245, 247, 250))
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(32))
        }
        scrollView.addView(content)

        content.addView(text("DPC Outreach", 28, Color.rgb(16, 42, 67), true))
        content.addView(text("Call log demo", 15, Color.rgb(82, 96, 109)))

        content.addView(sectionTitle("Backend"))
        backendUrlInput = input(
            "Backend URL (emulator: http://10.0.2.2:8080)",
            preferences.getString(KEY_BACKEND_URL, "http://10.0.2.2:8080") ?: "http://10.0.2.2:8080",
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
        )
        content.addView(backendUrlInput)
        dpcIdInput = input(
            "DPC ID (shown in backend demo startup log)",
            preferences.getString(KEY_DPC_ID, "") ?: "",
            InputType.TYPE_CLASS_NUMBER
        )
        content.addView(dpcIdInput)
        content.addView(actionButton("Load contacts", ::loadContacts))

        content.addView(sectionTitle("Contact directory (optional)"))
        content.addView(text(
            "Selecting a contact fills in its number. You can also enter any number below.",
            14,
            Color.rgb(82, 96, 109)
        ))
        contactsContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(contactsContainer)

        content.addView(sectionTitle("Call and submit"))
        phoneInput = input("Phone number", "", InputType.TYPE_CLASS_PHONE)
        content.addView(phoneInput)
        content.addView(actionButton("Open dialer", ::openDialer))
        content.addView(actionButton("Check call log", ::checkCallLog))
        content.addView(text(
            "If call-log access is unavailable, you can still submit call metadata manually.",
            14,
            Color.rgb(82, 96, 109)
        ))
        timestampInput = input(
            "Call time (ISO 8601, e.g. 2026-11-08T10:30:00Z)",
            Instant.now().toString(),
            InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_NORMAL
        )
        content.addView(timestampInput)
        durationInput = input("Call duration in seconds", "", InputType.TYPE_CLASS_NUMBER)
        content.addView(durationInput)
        content.addView(text("Reported call outcome (self-reported)", 14, Color.rgb(82, 96, 109), true))
        content.addView(text(
            "Duration cannot tell whether a person, voicemail, or forwarding answered. Record what you observed.",
            13,
            Color.rgb(82, 96, 109)
        ))
        outcomeSpinner = Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_item,
                callOutcomes.map { it.label }
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }
        content.addView(outcomeSpinner)
        submitCallButton = actionButton("Submit call metadata", ::submitManualCall)
        content.addView(submitCallButton)

        content.addView(sectionTitle("Status"))
        statusText = text("Ready.", 14, Color.rgb(16, 42, 67))
        statusText.setPadding(dp(12), dp(12), dp(12), dp(12))
        statusText.setBackgroundColor(Color.WHITE)
        content.addView(statusText)
        return scrollView
    }

    private fun loadContacts() {
        saveSettings()
        val endpoint = apiUrl("/api/contacts") ?: return
        setStatus("Loading contacts...")
        runRequest {
            val response = request(endpoint, "GET")
            val contacts = JSONArray(response)
            runOnUiThread {
                contactsContainer.removeAllViews()
                for (index in 0 until contacts.length()) {
                    val contact = contacts.getJSONObject(index)
                    val displayName = contact.optString("name", "Contact")
                    val company = contact.optString("companyName", "Company")
                    val number = contact.optString("phoneNumber", "")
                    contactsContainer.addView(actionButton("$displayName - $company\n$number") {
                        phoneInput.setText(number)
                        setStatus("Selected $displayName. The directory is optional; any number can be entered.")
                    })
                }
                setStatus(if (contacts.length() == 0) {
                    "No contacts found. Enter a phone number manually or add contacts with the admin client."
                } else {
                    "Loaded ${contacts.length()} contacts."
                })
            }
        }
    }

    private fun openDialer() {
        val phone = phoneInput.text.toString().trim()
        if (phone.isBlank()) {
            setStatus("Enter a phone number first.")
            return
        }
        val dpcId = readDpcId() ?: return
        saveSettings()
        pendingDialerCall = null
        outcomeSpinner.setSelection(0)
        submitCallButton.text = "Submit call metadata"
        val now = System.currentTimeMillis()
        pendingCall = PendingCall(phone, now)
        preferences.edit()
            .putString(KEY_PENDING_PHONE, phone)
            .putLong(KEY_PENDING_TIMESTAMP, now)
            .apply()

        try {
            startActivityForResult(
                Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null)),
                REQUEST_DIALER
            )
            setStatus("Dialer opened. Place the call, then return to this app to check the call log.")
        } catch (exception: Exception) {
            pendingCall = null
            clearPendingCall()
            setStatus("Could not open the dialer: ${exception.message}")
        }
        preferences.edit().putString(KEY_DPC_ID, dpcId.toString()).apply()
    }

    private fun checkCallLog() {
        val pending = pendingCall
        if (pending == null) {
            setStatus("Open the dialer first, or enter call time and duration for manual submission.")
            return
        }
        if (checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_CALL_LOG), REQUEST_CALL_LOG)
            return
        }

        setStatus("Checking recent calls...")
        try {
            val call = findMatchingCall(pending)
            if (call == null) {
                setStatus("No matching outgoing call was found. Return after the call completes and check again, or enter the metadata manually.")
                return
            }
            phoneInput.setText(call.phoneNumber)
            timestampInput.setText(Instant.ofEpochMilli(call.timestampMillis).toString())
            durationInput.setText(call.durationSeconds.toString())
            pendingDialerCall = call
            outcomeSpinner.setSelection(0)
            submitCallButton.text = "Submit dialer call"
            setStatus("Call details loaded. Select the reported outcome, then tap Submit dialer call.")
        } catch (securityException: SecurityException) {
            setStatus("Android denied call-log access. Enter the timestamp and duration manually.")
        } catch (exception: Exception) {
            setStatus("Could not read the call log: ${exception.message}")
        }
    }

    private fun findMatchingCall(pending: PendingCall): LoggedCall? {
        val expectedPhone = normalizePhone(pending.phoneNumber)
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )
        val selection = "${CallLog.Calls.DATE} >= ? AND ${CallLog.Calls.TYPE} = ?"
        val selectionArgs = arrayOf(
            (pending.startedAtMillis - CALL_LOG_CLOCK_TOLERANCE_MILLIS).toString(),
            CallLog.Calls.OUTGOING_TYPE.toString()
        )
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        val cursor: Cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        ) ?: return null

        cursor.use {
            val numberColumn = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val dateColumn = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durationColumn = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)
            while (it.moveToNext()) {
                val number = it.getString(numberColumn) ?: continue
                if (normalizePhone(number) == expectedPhone) {
                    return LoggedCall(
                        number,
                        it.getLong(dateColumn),
                        it.getInt(durationColumn)
                    )
                }
            }
        }
        return null
    }

    private fun submitManualCall() {
        val phone = phoneInput.text.toString().trim()
        if (phone.isBlank()) {
            setStatus("Enter a phone number first.")
            return
        }
        val timestamp = try {
            Instant.parse(timestampInput.text.toString().trim())
        } catch (_: Exception) {
            setStatus("Enter a valid ISO 8601 UTC timestamp, for example 2026-11-08T10:30:00Z.")
            return
        }
        val duration = durationInput.text.toString().trim().toIntOrNull()
        if (duration == null || duration < 0) {
            setStatus("Enter a non-negative whole number of seconds.")
            return
        }
        val outcome = callOutcomes[outcomeSpinner.selectedItemPosition]
        val dialerCall = pendingDialerCall
        if (dialerCall != null) {
            submitCall(
                dialerCall.phoneNumber,
                dialerCall.timestampMillis,
                dialerCall.durationSeconds,
                "DIALER",
                outcome
            )
        } else {
            submitCall(phone, timestamp.toEpochMilli(), duration, "MANUAL_METADATA", outcome)
        }
    }

    private fun submitCall(
        phone: String,
        timestampMillis: Long,
        durationSeconds: Int,
        callType: String,
        callOutcome: CallOutcomeOption
    ) {
        val dpcId = readDpcId() ?: return
        val endpoint = apiUrl("/api/call-records") ?: return
        saveSettings()
        val payload = JSONObject()
            .put("dpcId", dpcId)
            .put("phoneNumber", phone)
            .put("callTimestamp", Instant.ofEpochMilli(timestampMillis).toString())
            .put("durationSeconds", durationSeconds)
            .put("callType", callType)
            .put("callOutcome", callOutcome.code)
        setStatus("Submitting call metadata...")
        runRequest {
            val response = JSONObject(request(endpoint, "POST", payload.toString()))
            clearPendingCall()
            pendingCall = null
            pendingDialerCall = null
            runOnUiThread {
                outcomeSpinner.setSelection(0)
                submitCallButton.text = "Submit call metadata"
                setStatus(
                    "Call saved. Status: ${response.optString("verificationStatus")}; " +
                        "outcome: ${callOutcome.label}; record ID: ${response.optLong("id")}."
                )
            }
        }
    }

    private fun apiUrl(path: String): String? {
        val base = backendUrlInput.text.toString().trim().trimEnd('/')
        if (base.isBlank()) {
            setStatus("Enter a backend URL.")
            return null
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            setStatus("Backend URL must start with http:// or https://.")
            return null
        }
        return base + path
    }

    private fun readDpcId(): Long? {
        val dpcId = dpcIdInput.text.toString().trim().toLongOrNull()
        if (dpcId == null || dpcId <= 0L) {
            setStatus("Enter the positive DPC ID printed by the backend demo seeder.")
            return null
        }
        return dpcId
    }

    private fun saveSettings() {
        preferences.edit()
            .putString(KEY_BACKEND_URL, backendUrlInput.text.toString().trim())
            .putString(KEY_DPC_ID, dpcIdInput.text.toString().trim())
            .apply()
    }

    private fun clearPendingCall() {
        preferences.edit()
            .remove(KEY_PENDING_PHONE)
            .remove(KEY_PENDING_TIMESTAMP)
            .apply()
    }

    private fun runRequest(action: () -> Unit) {
        executor.execute {
            try {
                action()
            } catch (exception: Exception) {
                runOnUiThread { setStatus("Request failed: ${exception.message}") }
            }
        }
    }

    private fun request(url: String, method: String, body: String? = null): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = NETWORK_TIMEOUT_MILLIS
        connection.readTimeout = NETWORK_TIMEOUT_MILLIS
        connection.setRequestProperty("Accept", "application/json")
        if (body != null) {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use { output ->
                output.write(body.toByteArray(Charsets.UTF_8))
            }
        }
        try {
            val statusCode = connection.responseCode
            val stream = if (statusCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (statusCode !in 200..299) {
                throw IllegalStateException("Backend returned HTTP $statusCode: $response")
            }
            return response
        } finally {
            connection.disconnect()
        }
    }

    private fun normalizePhone(phoneNumber: String): String = phoneNumber.filter(Char::isDigit)

    private fun setStatus(message: String) {
        if (::statusText.isInitialized) {
            statusText.text = message
        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun input(hintText: String, value: String, inputType: Int): EditText =
        EditText(this).apply {
            hint = hintText
            setText(value)
            this.inputType = inputType
            textSize = 15f
            setSingleLine(inputType != (InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_NORMAL))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(8) }
        }

    private fun actionButton(label: String, action: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            gravity = Gravity.CENTER
            setOnClickListener {
                hideKeyboard()
                action()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(6) }
        }

    private fun sectionTitle(label: String): TextView =
        text(label, 18, Color.rgb(16, 42, 67), true).apply {
            setPadding(0, dp(22), 0, dp(8))
        }

    private fun text(label: String, size: Int, color: Int, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = label
            textSize = size.toFloat()
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

    private fun hideKeyboard() {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class PendingCall(val phoneNumber: String, val startedAtMillis: Long)
    private data class LoggedCall(val phoneNumber: String, val timestampMillis: Long, val durationSeconds: Int)
    private data class CallOutcomeOption(val label: String, val code: String)

    companion object {
        private const val REQUEST_DIALER = 100
        private const val REQUEST_CALL_LOG = 101
        private const val NETWORK_TIMEOUT_MILLIS = 10_000
        private const val CALL_LOG_CLOCK_TOLERANCE_MILLIS = 60_000L
        private const val KEY_BACKEND_URL = "backend-url"
        private const val KEY_DPC_ID = "dpc-id"
        private const val KEY_PENDING_PHONE = "pending-phone"
        private const val KEY_PENDING_TIMESTAMP = "pending-timestamp"
        private const val STATE_PENDING_PHONE = "state-pending-phone"
        private const val STATE_PENDING_TIMESTAMP = "state-pending-timestamp"
    }

    private val callOutcomes = listOf(
        CallOutcomeOption("Unknown", "UNKNOWN"),
        CallOutcomeOption("Spoke with a person", "SPOKE_WITH_PERSON"),
        CallOutcomeOption("Voicemail", "VOICEMAIL"),
        CallOutcomeOption("Forwarded / automated system", "FORWARDED_OR_AUTOMATED"),
        CallOutcomeOption("No answer", "NO_ANSWER")
    )
}
