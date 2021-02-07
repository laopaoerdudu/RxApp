package com.dev

class OperatorUtils {
    companion object {
        fun sleep(time: Long) {
            try {
                Thread.sleep(time)
            } catch (error: InterruptedException) {
                error.printStackTrace()
            }
        }

        fun logThread(name: String) {
            println(name + " from " + Thread.currentThread().name)
        }
    }
}