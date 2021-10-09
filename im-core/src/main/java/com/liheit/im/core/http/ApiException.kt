package com.liheit.im.core.http

class ApiException(var code: String, var msg: String, var moreInfo:String) : RuntimeException(msg)
