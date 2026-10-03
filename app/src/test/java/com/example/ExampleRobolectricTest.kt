package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.xiu.lockagent.model.AgentActionResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Xiu LockAgent", appName)
  }

  @Test
  fun `verify agent action response contract`() {
    val response = AgentActionResponse(
        actionType = "CLICK_NODE",
        targetIdentifier = "Transfer Sekarang",
        inputValue = null,
        fallbackX = 360,
        fallbackY = 720,
        responseMessage = "Mengeksekusi transfer..."
    )
    assertEquals("CLICK_NODE", response.actionType)
    assertEquals(360, response.fallbackX)
    assertEquals(720, response.fallbackY)
    assertNotNull(response.responseMessage)
  }
}
