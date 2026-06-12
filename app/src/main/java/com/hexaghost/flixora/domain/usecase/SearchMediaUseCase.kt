package com.hexaghost.flixora.domain.usecase

import com.hexaghost.flixora.domain.model.Media
import com.hexaghost.flixora.domain.repository.MediaRepository
import javax.inject.Inject

class SearchMediaUseCase @Inject constructor(
    private val repository: MediaRepository
) {
    suspend operator fun invoke(query: String, page: Int = 1): Result<List<Media>> = runCatching {
        repository.searchMulti(query, page)
    }
}
