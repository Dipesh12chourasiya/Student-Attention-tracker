package com.example.irlstudentattentiontracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.irlstudentattentiontracker.models.NinjaQuote
import com.example.irlstudentattentiontracker.repository.NinjaQuoteRepository
import kotlinx.coroutines.launch
import androidx.lifecycle.MutableLiveData

class NinjaQuoteVM : ViewModel() {

    private val repository = NinjaQuoteRepository()
    val quoteLiveData = MutableLiveData<NinjaQuote>()

    fun getQuote() {
        viewModelScope.launch {
            val result = repository.fetchRandomQuote()
            if (result != null) {
                quoteLiveData.postValue(result)
            }
        }
    }
}
