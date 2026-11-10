package eu.hxreborn.phdp.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.hxreborn.phdp.about.Contributor
import eu.hxreborn.phdp.about.loadContributors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ContributorsViewModel(
    application: Application,
) : ViewModel() {
    private val _contributors = MutableStateFlow<List<Contributor>>(emptyList())
    val contributors: StateFlow<List<Contributor>> = _contributors

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _contributors.value = loadContributors(application.assets)
        }
    }

    companion object {
        val Factory =
            viewModelFactory {
                initializer { ContributorsViewModel(this[APPLICATION_KEY] as Application) }
            }
    }
}
