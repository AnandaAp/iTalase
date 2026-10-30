package com.anlian.italase.shared.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anlian.italase.shared.data.local.entity.QrisConfigEntity
import com.anlian.italase.shared.data.repository.ShopRepository
import com.anlian.italase.shared.media.ImageOptimizer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Singleton

/**
 * ViewModel pengelola konfigurasi pembayaran QRIS dan alur citra Setup-Review (AUD-107).
 *
 * Menghubungkan layar Setup, Review, dan Pengaturan Pembayaran ke [ShopRepository].
 * Buffer [pendingImageBytes] menampung citra mentah sementara dari kamera/galeri
 * tanpa memerlukan serialisasi bundle navigasi yang rentan TransactionTooLargeException.
 */
@Singleton
class QrisViewModel(private val repo: ShopRepository) : ViewModel() {

    internal var computationDispatcher: CoroutineDispatcher = Dispatchers.Default

    val qrisConfig: StateFlow<QrisConfigEntity?> =
        repo.observeQrisConfig().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000L),
            null,
        )

    val pendingImageBytes: MutableStateFlow<ByteArray?> = MutableStateFlow(null)
    val isProcessing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val message: MutableStateFlow<UiMessage?> = MutableStateFlow(null)

    fun setPendingImage(bytes: ByteArray) {
        pendingImageBytes.value = bytes
    }

    fun clearPendingImage() {
        pendingImageBytes.value = null
    }

    fun clearMessage() {
        message.value = null
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                repo.setQrisEnabled(enabled)
            } catch (t: Throwable) {
                message.value = UiMessage.GenericFailure
            }
        }
    }

    fun deleteQris() {
        viewModelScope.launch {
            try {
                repo.deleteQrisConfig()
            } catch (t: Throwable) {
                message.value = UiMessage.DeleteFailed
            }
        }
    }

    fun savePendingQris(onSuccess: () -> Unit = {}) {
        val raw = pendingImageBytes.value ?: return
        if (isProcessing.value) return
        viewModelScope.launch {
            try {
                isProcessing.value = true
                val optimized = withContext(computationDispatcher) {
                    ImageOptimizer.optimize(raw)
                }
                repo.saveQrisConfig(optimized)
                pendingImageBytes.value = null
                onSuccess()
            } catch (t: Throwable) {
                message.value = UiMessage.SaveFailed
            } finally {
                isProcessing.value = false
            }
        }
    }

    fun dismissPrompt() {
        viewModelScope.launch {
            repo.dismissQrisPrompt()
        }
    }
}
