package com.legacy.fingame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoundEffect
import androidx.compose.ui.platform.SoundEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.legacy.fingame.ui.ClickSound
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Щелчок при нажатии кнопок слушается настройки «Звуки»: [ClickSound.enabled] выключает его у
 * каждого `clickable`, а не только у одного view.
 *
 * Звук подменён счётчиком через [LocalSoundEffect], поэтому тест не зависит от того, включён ли
 * звук касания в настройках самого эмулятора.
 */
@RunWith(AndroidJUnit4::class)
class ClickSoundTest {

    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createComposeRule()

    /** Сколько раз Compose попросил сыграть щелчок. */
    private var clicks = 0

    /** Звук, который вместо щелчка считает, сколько раз его сыграли. */
    private val countingSound = object : SoundEffect {
        override fun playClickSound() {
            clicks++
        }
    }

    /** Флаг глобальный на процесс: следующий тест получает его включённым, как в приложении. */
    @After
    fun restoreClickSound() {
        ClickSound.enabled = true
    }

    /** Показывает одну нажимаемую область с тегом `button`. */
    private fun showButton() {
        compose.setContent {
            CompositionLocalProvider(LocalSoundEffect provides countingSound) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("button")
                        .clickable { }
                )
            }
        }
    }

    @Test
    fun aClickPlaysTheSoundWhileSoundsAreOn() {
        ClickSound.enabled = true
        showButton()

        compose.onNodeWithTag("button").performClick()

        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun aClickIsSilentOnceSoundsAreOff() {
        ClickSound.enabled = false
        showButton()

        compose.onNodeWithTag("button").performClick()

        compose.runOnIdle { assertEquals(0, clicks) }
    }
}
