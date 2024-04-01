package org.nekoweb.amycatgirl.revolt.models.api.attachments

import kotlinx.serialization.Serializable

@Serializable
data class VideoFile(
    val width: Int,
    val height: Int
) : FileMetadata("Video")
