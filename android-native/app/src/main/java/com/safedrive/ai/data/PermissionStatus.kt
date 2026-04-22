package com.safedrive.ai.data

data class PermissionStatus(
    val location: Boolean = false,
    val notifications: Boolean = false,
    val sms: Boolean = false,
    val phone: Boolean = false,
) {
    val allGranted: Boolean
        get() = location && notifications && sms && phone

    val crashFlowReady: Boolean
        get() = notifications && sms && phone
}
