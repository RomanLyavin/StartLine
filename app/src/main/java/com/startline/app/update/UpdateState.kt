package com.startline.app.update

import java.io.File

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data class Available(val version: String, val apkUrl: String, val size: Long) : UpdateState
    data class Latest(val version: String) : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data class Ready(val version: String, val file: File) : UpdateState
    data class Error(val message: String) : UpdateState
}
