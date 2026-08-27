package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.local.ExpenseEntity
import com.example.ui.screens.ExpenseScreen
import com.example.ui.theme.MyApplicationTheme
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
  fun expense_screen_screenshot() {
    val sampleExpenses = listOf(
      ExpenseEntity(
        id = 1,
        type = "EXPENSE",
        category = "餐饮美食",
        amount = 38.0,
        note = "生椰拿铁与牛角包",
        dateTimestamp = System.currentTimeMillis()
      ),
      ExpenseEntity(
        id = 2,
        type = "INCOME",
        category = "工资收入",
        amount = 15000.0,
        note = "本月工资结算",
        dateTimestamp = System.currentTimeMillis() - 86400000L
      )
    )

    val sampleStats = listOf(
      CategoryStat(
        category = "餐饮美食",
        totalAmount = 38.0,
        count = 1,
        percentage = 1.0f,
        type = "EXPENSE"
      )
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        ExpenseScreen(
          expenses = sampleExpenses,
          accounts = emptyList(),
          totalExpense = 38.0,
          totalIncome = 15000.0,
          todayExpense = 38.0,
          categoryStats = sampleStats,
          filterType = "ALL",
          filterTime = "ALL",
          searchQuery = "",
          showAddDialogTrigger = false,
          onCloseAddDialogTrigger = {},
          onSetFilterType = {},
          onSetFilterTime = {},
          onSetSearchQuery = {},
          onAddExpense = { _, _, _, _, _, _, _, _, _ -> },
          onUpdateExpense = { _, _ -> },
          onDeleteExpense = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
