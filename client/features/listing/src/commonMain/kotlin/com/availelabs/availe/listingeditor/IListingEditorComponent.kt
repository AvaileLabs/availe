package com.availelabs.availe.listingeditor

import com.arkivanov.decompose.router.pages.ChildPages
import com.arkivanov.decompose.value.Value
import com.availelabs.availe.ListingEditorState
import com.availelabs.availe.domain.model.ListingCategory
import com.availelabs.availe.domain.model.SupportedCurrency
import com.availelabs.availe.domain.model.TimeZoneIdentifier
import com.availelabs.availe.ui.model.AppBarAction
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

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
    fun onStartDateChanged(startDate: LocalDate?)
    fun onStartTimeChanged(startTime: LocalTime?)
    fun onEndDateChanged(endDate: LocalDate?)
    fun onEndTimeChanged(endTime: LocalTime?)
    fun onTimeZoneIdentifierChanged(timeZoneIdentifier: TimeZoneIdentifier)
    fun onPageSelected(index: Int)
    fun onPublishClicked()
    fun onConfirmPublish()
    fun onCancelPublish()
    fun onConfirmScheduleAdjustment()
    fun onCancelScheduleAdjustment()
    fun onDiscardDraftClicked()
    fun onConfirmDiscardDraft()
    fun onCancelDiscardDraft()
    fun onRequestDeleteDraft()
    fun onCancelDeleteDraft()
    fun onClearStep(config: ListingEditorPageConfig)
    fun onBackClicked()
}