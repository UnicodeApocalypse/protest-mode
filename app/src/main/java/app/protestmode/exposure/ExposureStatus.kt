package app.protestmode.exposure

import app.protestmode.setting.ExposureLabel
import app.protestmode.setting.RiskLevel
import app.protestmode.setting.SettingId
import app.protestmode.setting.SettingState
import app.protestmode.setting.isSatisfiedBy

enum class ExposureStatus { EXPOSED, PARTIAL, DISCREET }

data class Exposure(val status: ExposureStatus, val remainingAdvice: Int, val securedLabels: Set<ExposureLabel>) {
    companion object {
        fun evaluate(states: Map<SettingId, SettingState>, confirmed: Set<SettingId> = emptySet()): Exposure {
            val unprotected = states.filterNot { (id, state) -> isProtected(id, state, id in confirmed) }.keys
            val risks = unprotected.map { it.risk }.toSet()
            val status =
                when {
                    RiskLevel.CRITICAL in risks -> ExposureStatus.EXPOSED
                    RiskLevel.HIGH in risks -> ExposureStatus.PARTIAL
                    else -> ExposureStatus.DISCREET
                }
            val (advised, blocking) = unprotected.partition { it.risk == RiskLevel.MODERATE }
            return Exposure(
                status = status,
                remainingAdvice = advised.size,
                securedLabels = ExposureLabel.entries.toSet() - blocking.flatMap { it.labels }.toSet()
            )
        }

        fun isProtected(id: SettingId, state: SettingState, confirmed: Boolean) =
            id.isSatisfiedBy(state) || (state == SettingState.UNKNOWN && confirmed)
    }
}
