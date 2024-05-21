package com.ketch

import java.util.UUID

interface Request {
    val id: UUID
    val url: String
    val path: String
    val fileName: String
    val tag: String?
}