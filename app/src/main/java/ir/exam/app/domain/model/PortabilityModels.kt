package ir.exam.app.domain.model

import kotlinx.serialization.json.JsonObject

data class PortableFile(val fileName: String, val mimeType: String, val content: String)

data class BackupPreview(
    val createdAt: String?,
    val teacherName: String?,
    val examCount: Int,
    val totalQuestionCount: Int,
    val classCount: Int,
    val membershipCount: Int,
    val hasHeader: Boolean,
    val bundle: JsonObject
)

data class RestoreOptions(
    val exams: Boolean = true,
    val classes: Boolean = true,
    val memberships: Boolean = true,
    val header: Boolean = true
)

data class RestoreSummary(
    val examsCreated: Int,
    val classesCreated: Int,
    val membershipsRestored: Int,
    val membershipsMissing: Int,
    val chargedToman: Long,
    val balanceToman: Long
)

data class StorageMaintenanceSummary(
    val dryRun: Boolean,
    val scannedExamObjects: Int,
    val orphanCandidates: Int,
    val scannedApks: Int,
    val apkCandidates: Int,
    val deletedObjects: Int,
    val deletedApks: Int
)
