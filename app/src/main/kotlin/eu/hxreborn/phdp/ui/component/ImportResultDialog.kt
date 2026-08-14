package eu.hxreborn.phdp.ui.component

import android.content.Context
import android.os.Build
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.backup.AdaptNote
import eu.hxreborn.phdp.backup.FailureReason
import eu.hxreborn.phdp.backup.RejectReason
import eu.hxreborn.phdp.backup.RestoreEntry
import eu.hxreborn.phdp.backup.RestoreOutcome
import eu.hxreborn.phdp.backup.RestoreReport

private fun keyList(keys: List<String>) = keys.joinToString(", ")

private fun RejectReason.text(context: Context): String =
    when (this) {
        RejectReason.NotABoolean -> context.getString(R.string.reject_not_a_boolean)
        RejectReason.NotAWholeNumber -> context.getString(R.string.reject_not_a_whole_number)
        RejectReason.NotANumber -> context.getString(R.string.reject_not_a_number)
        RejectReason.NotText -> context.getString(R.string.reject_not_text)
        RejectReason.NotAList -> context.getString(R.string.reject_not_a_list)
        RejectReason.EntriesNotText -> context.getString(R.string.reject_entries_not_text)
        RejectReason.MalformedOffsets -> context.getString(R.string.reject_malformed_offsets)
        is RejectReason.TooLong -> context.getString(R.string.reject_too_long, limit)
        is RejectReason.TooManyEntries -> context.getString(R.string.reject_too_many_entries, limit)
        is RejectReason.NotAllowed -> context.getString(R.string.reject_not_allowed, allowed.joinToString("/"))
        is RejectReason.OffsetOutOfRange -> context.getString(R.string.reject_offset_out_of_range, format(limit))
        is RejectReason.MalformedLegacyKeys -> context.getString(R.string.reject_malformed_legacy, keyList(keys))
    }

private fun AdaptNote.text(context: Context): String =
    when (this) {
        is AdaptNote.ClampedWhole -> context.getString(R.string.adapt_clamped, format(from), format(min), format(max))
        is AdaptNote.ClampedNumber -> context.getString(R.string.adapt_clamped, format(from), format(min), format(max))
        is AdaptNote.CollapsedDuplicates -> context.getString(R.string.adapt_collapsed_duplicates, count)
        is AdaptNote.MigratedFromKeys -> context.getString(R.string.adapt_migrated_from, keyList(keys))
        is AdaptNote.RenamedFromKey -> context.getString(R.string.adapt_renamed_from, key)
    }

private fun FailureReason.text(context: Context): String =
    when (this) {
        FailureReason.NotSaved -> context.getString(R.string.failure_not_saved)
        FailureReason.NotSavedOrRestored -> context.getString(R.string.failure_not_saved_or_restored)
        is FailureReason.WriteFailed -> context.getString(R.string.failure_write_failed, detail)
    }

private fun RestoreEntry.detail(context: Context): String? =
    when (val outcome = outcome) {
        is RestoreOutcome.Migrated -> outcome.notes.joinToString("; ") { it.text(context) }
        is RestoreOutcome.Rejected -> outcome.reason.text(context)
        is RestoreOutcome.Failed -> outcome.reason.text(context)
        else -> null
    }

private val RestoreReport.deviceMismatch: Boolean
    get() = meta.deviceModel.isNotBlank() && meta.deviceModel != Build.MODEL

private val RestoreReport.ignored: List<RestoreEntry>
    get() = unknown + deprecated + excluded

private fun List<RestoreEntry>.group(
    context: Context,
    labelRes: Int,
    icon: ImageVector,
    tone: ResultTone,
): ResultGroup? =
    takeIf { it.isNotEmpty() }?.let { entries ->
        ResultGroup(
            label = context.getString(labelRes),
            icon = icon,
            tone = tone,
            items =
                entries
                    .sortedBy { it.key }
                    .map { ResultItem(name = it.key, detail = it.detail(context)) },
        )
    }

private fun RestoreReport.groups(context: Context): List<ResultGroup> =
    listOfNotNull(
        rejected.group(context, R.string.restore_group_rejected, Icons.Outlined.ErrorOutline, ResultTone.Alert),
        failed.group(context, R.string.restore_group_failed, Icons.Outlined.WarningAmber, ResultTone.Alert),
        applied.group(context, R.string.restore_group_applied, Icons.Outlined.CheckCircle, ResultTone.Positive),
        migrated.group(context, R.string.restore_group_migrated, Icons.Outlined.AutoFixHigh, ResultTone.Positive),
        unchanged.group(context, R.string.restore_group_unchanged, Icons.Outlined.DoneAll, ResultTone.Neutral),
        ignored.group(context, R.string.restore_group_ignored, Icons.Outlined.RemoveCircleOutline, ResultTone.Neutral),
    )

private fun RestoreReport.metadata(context: Context): String {
    val version = meta.appVersionName.ifBlank { "?" }
    val date = meta.exportedAt.substringBefore('T').ifBlank { "?" }
    return meta.deviceModel
        .takeIf { it.isNotBlank() }
        ?.let { context.getString(R.string.restore_source_device, version, date, it) }
        ?: context.getString(R.string.restore_source, version, date)
}

@Composable
fun ImportResultDialog(
    report: RestoreReport,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val groups = remember(report, context) { report.groups(context) }
    val metadata = remember(report, context) { report.metadata(context) }

    ResultDialog(
        icon =
            when {
                report.rejected.isNotEmpty() || report.failed.isNotEmpty() -> Icons.Outlined.WarningAmber
                report.entries.isEmpty() -> Icons.Outlined.Info
                else -> Icons.Outlined.CheckCircle
            },
        title = stringResource(R.string.restore_title),
        headline =
            if (report.entries.isEmpty()) {
                stringResource(R.string.restore_nothing)
            } else {
                stringResource(
                    R.string.restore_headline,
                    report.restoredCount + report.unchanged.size,
                    report.entries.size,
                )
            },
        groups = groups,
        metadata = metadata,
        alert = stringResource(R.string.restore_device_mismatch).takeIf { report.deviceMismatch },
        onDismiss = onDismiss,
    )
}
