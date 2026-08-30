package com.example

import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.EditorialPreviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BudgetConfig
import com.example.ui.viewmodel.BudgetProgressInfo
import com.example.ui.viewmodel.CategoryStat
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun accounts_screen_screenshot() {
    val sampleAccounts = listOf(
      AccountEntity(
        id = 1,
        name = "微信钱包",
        type = "WECHAT",
        balance = 2860.50,
        cardSuffix = "",
        colorHex = "#07C160"
      ),
      AccountEntity(
        id = 2,
        name = "招商银行卡",
        type = "BANK_CARD",
        balance = 52300.00,
        cardSuffix = "8899",
        colorHex = "#E60012"
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        AccountsScreen(
          accounts = sampleAccounts,
          expenses = emptyList(),
          totalNetAssets = 55160.50,
          totalPositiveAssets = 55160.50,
          totalDebts = 0.0,
          onAddAccount = { _, _, _, _, _, _ -> },
          onUpdateAccount = { _, _, _ -> },
          onDeleteAccount = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/accounts.png")
  }
}
