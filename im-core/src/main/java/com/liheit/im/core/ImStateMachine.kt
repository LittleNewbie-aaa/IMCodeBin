package com.liheit.im.core

import android.os.Handler
import android.os.Looper

import com.liheit.im.state.StateMachine

/**
 * Created by daixun on 2018/12/2.
 */

class ImStateMachine : StateMachine {
    protected constructor(name: String) : super(name) {}

    protected constructor(name: String, looper: Looper) : super(name, looper) {}

    protected constructor(name: String, handler: Handler) : super(name, handler) {}
}
