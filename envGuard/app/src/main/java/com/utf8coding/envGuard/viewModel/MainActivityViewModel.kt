package com.utf8coding.envGuard.viewModel

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {
    companion object {
        const val DOUBLE_BACK_PRESS_DELAY = 2000
    }
    var firstBackPressedTime: Long = 0L
    var currentFragmentNumber: Int = 1
}