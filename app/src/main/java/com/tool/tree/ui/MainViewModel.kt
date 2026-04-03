package com.tool.tree.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.omarea.krscript.model.NodeInfoBase
import com.omarea.krscript.model.PageNode
import com.omarea.krscript.config.PageConfigReader
import com.omarea.krscript.config.PageConfigSh
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {

    private val _favoritesItems = MutableLiveData<ArrayList<NodeInfoBase>>()
    val favoritesItems: LiveData<ArrayList<NodeInfoBase>> = _favoritesItems

    private val _pagesItems = MutableLiveData<ArrayList<NodeInfoBase>>()
    val pagesItems: LiveData<ArrayList<NodeInfoBase>> = _pagesItems

    // Load items từ PageNode
    suspend fun loadItems(pageNode: PageNode, isFavorites: Boolean) {
        val items = withContext(Dispatchers.IO) {
            var result: ArrayList<NodeInfoBase>? = null
            if (pageNode.pageConfigSh.isNotEmpty()) {
                result = PageConfigSh(null, pageNode.pageConfigSh, null).execute()
            }
            if (result == null && pageNode.pageConfigPath.isNotEmpty()) {
                result = PageConfigReader(null, pageNode.pageConfigPath, null).readConfigXml()
            }
            result
        }

        items?.let {
            if (isFavorites) _favoritesItems.postValue(it) 
            else _pagesItems.postValue(it)
        }
    }
}