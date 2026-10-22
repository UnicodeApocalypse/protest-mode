package app.protestmode.ui

import android.app.Application
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import app.protestmode.data.ConfirmationStore
import app.protestmode.exposure.Exposure
import app.protestmode.exposure.ExposureStatus
import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.Reading
import app.protestmode.setting.SettingCatalog
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingSource
import app.protestmode.setting.SettingState
import java.security.MessageDigest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingRow(val id: SettingId, val state: SettingState, val confirmed: Boolean, val detail: String? = null) {
    val isProtected get() = Exposure.isProtected(id, state, confirmed)
}

data class MainUiState(
    val exposure: ExposureStatus = ExposureStatus.EXPOSED,
    val remainingAdvice: Int = 0,
    val securedLabels: Set<ExposureLabel> = emptySet(),
    val selectedLabel: ExposureLabel? = null,
    val rows: List<SettingRow> = emptyList()
)

class MainViewModel
@JvmOverloads
constructor(
    application: Application,
    private val savedState: SavedStateHandle,
    private val readDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val source: SettingSource = SettingSource.Device
) : AndroidViewModel(application) {
    private val context = application.applicationContext

    private val guideChannel = Channel<Intent>(Channel.BUFFERED)

    val guideRequests: Flow<Intent> = guideChannel.receiveAsFlow()

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    private var readings: Map<SettingId, Reading> = emptyMap()

    private val confirmationStore = ConfirmationStore(application)

    /** Confirmations with a fingerprint are stored across launches; the others only last for the session. */
    private var confirmations: Map<SettingId, String>
        get() {
            val session =
                savedState
                    .get<HashMap<String, String>>(KEY_CONFIRMED)
                    .orEmpty()
                    .mapNotNull { (name, key) -> idOf(name)?.let { it to key } }
                    .toMap()
            return confirmationStore.load() + session
        }
        set(value) {
            savedState[KEY_CONFIRMED] = HashMap(value.filterValues { it.isEmpty() }.mapKeys { it.key.name })
            confirmationStore.save(value)
        }

    private var selectedLabel: ExposureLabel?
        get() = savedState.get<String>(KEY_LABEL)?.let { name ->
            ExposureLabel.entries.firstOrNull { it.name == name }
        }
        set(value) {
            savedState[KEY_LABEL] = value?.name
        }

    private var refreshJob: Job? = null

    // Quick settings and floating panels change settings without pausing the activity, so the keys
    // read by the settings are observed directly.
    private val settingsObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                refresh(OBSERVER_DELAY_MS)
            }
        }

    init {
        SettingCatalog.all.flatMap { it.observedUris }.distinct().forEach {
            context.contentResolver.registerContentObserver(it, false, settingsObserver)
        }
        refresh()
    }

    override fun onCleared() {
        context.contentResolver.unregisterContentObserver(settingsObserver)
    }

    /** Called on ON_RESUME and window focus gain, which often follow each other: both share one read. */
    fun onResume() {
        refresh(RESUME_DELAY_MS)
    }

    fun onRowTapped(row: SettingRow) {
        val setting = SettingCatalog[row.id]
        viewModelScope.launch {
            val protectedAutomatically =
                withContext(readDispatcher) {
                    row.state != SettingState.UNKNOWN && setting.tryProtect(context)
                }
            if (!protectedAutomatically) {
                guideChannel.trySend(withContext(readDispatcher) { SettingCatalog.guideIntent(setting, context) })
            }
            refresh()
        }
    }

    fun confirmRow(id: SettingId) {
        val reading = readings[id] ?: return
        if (reading.state != SettingState.UNKNOWN) return
        confirmations = confirmations + (id to fingerprint(reading.confirmationKey))
        publish()
    }

    fun toggleLabel(label: ExposureLabel) {
        selectedLabel = if (selectedLabel == label) null else label
        publish()
    }

    private fun refresh(delayMs: Long = 0) {
        refreshJob?.cancel()
        refreshJob =
            viewModelScope.launch {
                if (delayMs > 0) delay(delayMs)
                readings = withContext(readDispatcher) { source.readAll(context) }
                publish()
            }
    }

    private fun publish() {
        val confirmedIds = validConfirmations()
        val exposure = Exposure.evaluate(readings.mapValues { it.value.state }, confirmedIds)
        val label = selectedLabel
        _state.value =
            MainUiState(
                exposure = exposure.status,
                remainingAdvice = exposure.remainingAdvice,
                securedLabels = exposure.securedLabels,
                selectedLabel = label,
                rows =
                readings
                    .filterKeys { label == null || label in it.labels }
                    .map { (id, reading) -> SettingRow(id, reading.state, id in confirmedIds, reading.detail) }
            )
    }

    private fun validConfirmations(): Set<SettingId> {
        val stored = confirmations
        val valid = stored.filter { (id, key) ->
            readings[id]?.let { key == fingerprint(it.confirmationKey) } ==
                true
        }
        if (valid.size != stored.size) confirmations = valid
        return valid.keys
    }

    private companion object {
        const val KEY_CONFIRMED = "confirmed"
        const val KEY_LABEL = "label"
        const val OBSERVER_DELAY_MS = 300L
        const val RESUME_DELAY_MS = 100L

        fun idOf(name: String) = SettingId.entries.firstOrNull { it.name == name }

        fun fingerprint(value: String?): String {
            if (value == null) return ""
            val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }
    }
}
