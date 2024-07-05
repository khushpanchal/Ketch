package com.ketch

data class Request(
    val id: Int,
    val url: String,
    val path: String,
    val fileName: String,
    val tag: String
)