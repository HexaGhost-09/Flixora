package com.hexaghost.flixora.domain.usecase

import com.hexaghost.flixora.domain.model.MediaDetail
import com.hexaghost.flixora.domain.repository.MediaRepository
import javax.inject.Inject

class GetMediaDetailUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(mediaId: Int, mediaType: String): Result<MediaDetail> = runCatching {
        if (mediaType == "tv") {
            repository.getTvDetail(mediaId)
        } else {
            repository.getMovieDetail(mediaId)
        }
    }
}
