package com.availelabs.availe.listingeditor

import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.SupportedCurrency
import com.availelabs.availe.domain.model.TimeZoneIdentifier
import com.availelabs.availe.ui.model.AppBarAction

interface IListingEditorComponent {
    val state: Value<ListingEditorState>
    val actions: Value<List<AppBarAction>>
    val pages: Value<ChildPages<ListingEditorPageConfig, ListingEditorPage>>
    fun onTitleChanged(title: String)
    fun onDescriptionChanged(description: String)
    fun onPriceAmountChanged(amount: String)
    fun onCurrencySelected(currency: SupportedCurrency)
    fun onTypeSelected(type: ListingCategory)
    fun onImageUrlChanged(url: String)
    fun onStartDateMonthChanged(monthString: String)
    fun onStartDateDayChanged(dayString: String)
    fun onStartDateYearChanged(yearString: String)
    fun onStartTimeHourChanged(hourString: String)
    fun onStartTimeMinuteChanged(minuteString: String)
    fun onStartTimeMeridiemToggled()
    fun onEndDateMonthChanged(monthString: String)
    fun onEndDateDayChanged(dayString: String)
    fun onEndDateYearChanged(yearString: String)
    fun onEndTimeHourChanged(hourString: String)
    fun onEndTimeMinuteChanged(minuteString: String)
    fun onEndTimeMeridiemToggled()
    fun onTimeZoneIdentifierChanged(timeZoneIdentifier: TimeZoneIdentifier)
    fun onTwentyFourHourTimeFormatChanged(isTwentyFourHourFormat: Boolean)
    fun onPageSelected(index: Int)
    fun onPublishClicked()
    fun onConfirmPublish()
    fun onCancelPublish()
    fun onDiscardDraftClicked()
    fun onConfirmDiscardDraft()
    fun onCancelDiscardDraft()
    fun onRequestDeleteDraft()
    fun onCancelDeleteDraft()
    fun onClearStep(config: ListingEditorPageConfig)
    fun onBackClicked()
}