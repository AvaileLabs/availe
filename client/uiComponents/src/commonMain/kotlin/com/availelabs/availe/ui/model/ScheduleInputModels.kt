package com.availelabs.availe.ui.model

data class SegmentedDateState(
    val monthInputString: String = "",
    val dayInputString: String = "",
    val yearInputString: String = ""
)

enum class MeridiemIndicator {
    AnteMeridiem,
    PostMeridiem
}

data class SegmentedTimeState(
    val hourInputString: String = "",
    val minuteInputString: String = "",
    val meridiemIndicator: MeridiemIndicator = MeridiemIndicator.AnteMeridiem
)