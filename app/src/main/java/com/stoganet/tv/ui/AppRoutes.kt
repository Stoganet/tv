package com.stoganet.tv.ui

import android.net.Uri

object AppRoutes {
    const val HOME = "home"
    const val LIBRARY_MOVIES = "library/movies"
    const val LIBRARY_TV = "library/tv"
    const val DETAIL = "detail/{id}"
    const val PLAYER = "player/{id}"

    fun detail(id: String) = "detail/${Uri.encode(id)}"
    fun player(id: String) = "player/${Uri.encode(id)}"
}
