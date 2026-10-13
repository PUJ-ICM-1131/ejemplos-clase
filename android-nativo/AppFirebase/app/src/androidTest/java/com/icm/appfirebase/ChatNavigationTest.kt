package com.icm.appfirebase

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.icm.appfirebase.navigation.AppScreen
import com.icm.appfirebase.presentation.chat.ChatScreen
import org.junit.Rule
import org.junit.Test

class ChatNavigationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun chatViewModelCanBeCreatedInsideNavigationEntryAndRecreatedAfterBack() {
        compose.setContent {
            val backStack = rememberNavBackStack(AppScreen.Home)
            MaterialTheme {
                NavDisplay(
                    backStack = backStack,
                    entryDecorators = listOf(
                        rememberSaveableStateHolderNavEntryDecorator(),
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    entryProvider = entryProvider {
                        entry<AppScreen.Home> {
                            Button(onClick = { backStack.add(AppScreen.Chat) }) {
                                Text("Ir al chat")
                            }
                        }
                        entry<AppScreen.Chat> {
                            ChatScreen(onNavigateBack = { backStack.removeAt(backStack.lastIndex) })
                        }
                    }
                )
            }
        }
        repeat(2) {
            compose.onNodeWithText("Ir al chat").performClick()
            compose.onNodeWithText("Chat").assertExists()
            compose.onNodeWithContentDescription("Back").performClick()
            compose.onNodeWithText("Ir al chat").assertExists()
        }
    }
}
