package com.example.macoslauncher

object MainActivityHolder {
    private var ref: MainActivity? = null
    fun set(a: MainActivity) { ref = a }
    fun clear() { ref = null }
    fun get(): MainActivity? = ref
}
