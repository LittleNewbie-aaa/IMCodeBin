package com.liheit.im.utils

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.internal.schedulers.RxThreadFactory
import io.reactivex.internal.schedulers.SingleScheduler


class ImSchedulers {

    companion object {
        @JvmStatic
        private val dispatcher = SingleScheduler(RxThreadFactory("IM-Dispatcher", 10, false))

        @JvmStatic
        private val send = SingleScheduler(RxThreadFactory("IM-Send", 10, false))

        @JvmStatic
        private val fileUpload = SingleScheduler(RxThreadFactory("IM-File-Upload", 5, false))

        @JvmStatic
        private val ui = AndroidSchedulers.mainThread()

        @JvmStatic
        fun dispatcher(): Scheduler {
            return dispatcher
        }

        @JvmStatic
        fun send(): Scheduler {
            return send
        }

        @JvmStatic
        fun fileUpload(): Scheduler {
            return fileUpload
        }

        @JvmStatic
        fun ui(): Scheduler {
            return ui
        }
    }
}

