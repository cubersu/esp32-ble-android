package com.example.esp32zero.ui

import com.example.esp32zero.ble.BleConnectionState
import com.example.esp32zero.ble.BleProtocol
import com.example.esp32zero.ble.FakeBleManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BleViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeBleManager: FakeBleManager
    private lateinit var viewModel: BleViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeBleManager = FakeBleManager()
        viewModel = BleViewModel(fakeBleManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `baslangic durumu bagli degil ve log bos`() {
        assertEquals(BleConnectionState.DISCONNECTED, viewModel.connectionState.value)
        assertTrue(viewModel.responseLog.value.isEmpty())
    }

    @Test
    fun `tara ve baglan cihaz bulununca baglaniyor`() = runTest {
        fakeBleManager.deviceFound = true

        viewModel.onScanAndConnectClicked()

        assertEquals(BleConnectionState.CONNECTED, viewModel.connectionState.value)
        assertEquals(1, fakeBleManager.connectCallCount)
    }

    @Test
    fun `baglantidayken ping gonderilince ping komutu yazilir`() = runTest {
        fakeBleManager.deviceFound = true
        viewModel.onScanAndConnectClicked()

        viewModel.onSendPingClicked()

        assertEquals(BleProtocol.buildPingCommand(), fakeBleManager.lastSentCommand)
    }

    @Test
    fun `bagli degilken ping gonderilmez`() = runTest {
        viewModel.onSendPingClicked()

        assertNull(fakeBleManager.lastSentCommand)
    }

    @Test
    fun `gelen pong yaniti log a isaretli sekilde eklenir`() = runTest {
        fakeBleManager.emitResponse("""{"cmd":"pong"}""")

        val entry = viewModel.responseLog.value.single()
        assertTrue(entry.isPong)
    }

    @Test
    fun `pong olmayan yanit log a isaretsiz eklenir`() = runTest {
        fakeBleManager.emitResponse("""{"cmd":"status"}""")

        val entry = viewModel.responseLog.value.single()
        assertTrue(!entry.isPong)
    }
}
