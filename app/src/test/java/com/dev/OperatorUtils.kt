package com.dev

import java.lang.Exception

class OperatorUtils {
    companion object {
        fun sleep(time: Long) {
            try {
                Thread.sleep(time)
            } catch (error: InterruptedException) {
                error.printStackTrace()
            }
        }
    }
}