package com.icm.unscramble.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameViewModelTest {

    @Test
    fun gameInitialization_firstWordLoaded() {
        val viewModel = GameViewModel()
        val state = viewModel.uiState.value

        assertTrue(state.currentScrambledWord.isNotBlank())
        assertEquals(1, state.currentWordCount)
        assertEquals(0, state.score)
        assertFalse(state.isGameOver)
    }

    @Test
    fun skipWord_updatesWordCountAndKeepsScore() {
        val viewModel = GameViewModel()

        viewModel.skipWord()
        val state = viewModel.uiState.value

        assertEquals(2, state.currentWordCount)
        assertEquals(0, state.score)
        assertFalse(state.isGuessedWordWrong)
    }
}
