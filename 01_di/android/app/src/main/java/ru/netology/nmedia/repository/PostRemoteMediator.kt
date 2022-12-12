package ru.netology.nmedia.repository

import androidx.paging.*
import androidx.room.withTransaction
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.PostRemoteKeyEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
class PostRemoteMediator(
    private val apiService: ApiService,
    private val postDao: PostDao,
    private val postRemoteKeyDao: PostRemoteKeyDao,
    private val appDb: AppDb
) : RemoteMediator<Int, PostEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, PostEntity>
    ): MediatorResult {
        try {
            val result = when (loadType) {
                LoadType.REFRESH ->
                    postRemoteKeyDao.max()?.let { id ->
                        apiService.getAfter(id, state.config.pageSize)
                    } ?: apiService.getLatest(state.config.pageSize)

                LoadType.PREPEND -> {
                    val id = postRemoteKeyDao.max() ?: return MediatorResult.Success(
                        endOfPaginationReached = false
                    )
                    apiService.getAfter(id, state.config.pageSize)
                }

                LoadType.APPEND -> {
                    val id = postRemoteKeyDao.min() ?: return MediatorResult.Success(false)
                    apiService.getBefore(id, state.config.pageSize)
                }

            }
            if (!result.isSuccessful) {
                throw ApiError(result.code(), result.message())
            }
            val data = result.body().orEmpty()


            appDb.withTransaction {
                when (loadType) {
                    LoadType.REFRESH -> {
                        postDao.clear()
                        postRemoteKeyDao.clear()
                        postRemoteKeyDao.insert(
                            PostRemoteKeyEntity(
                                PostRemoteKeyEntity.KeyType.AFTER,
                                data.first().id,
                            )
                        )
                        if (postDao.isEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    data.last().id,
                                ),
                            )
                        }
                    }
                    LoadType.APPEND -> {
                        if (data.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.BEFORE,
                                    data.last().id,
                                ),
                            )
                        }
                    }
                    LoadType.PREPEND -> {
                        if (data.isNotEmpty()) {
                            postRemoteKeyDao.insert(
                                PostRemoteKeyEntity(
                                    PostRemoteKeyEntity.KeyType.AFTER,
                                    data.first().id,
                                )
                            )
                        }
                    }
                }
                if (data.isNotEmpty())
                    postDao.insert(data.toEntity())
            }


            return MediatorResult.Success(data.isEmpty())
        } catch (e: IOException) {
            return MediatorResult.Error(e)
        }
    }


}
