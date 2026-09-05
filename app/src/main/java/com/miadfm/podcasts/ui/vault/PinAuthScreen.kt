package com.miadfm.podcasts.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.miadfm.podcasts.ui.i18n.appStrings
import com.miadfm.podcasts.ui.theme.BlueAccent
import com.miadfm.podcasts.ui.theme.BlueAccentLight
import com.miadfm.podcasts.ui.theme.BlueContainer
import com.miadfm.podcasts.ui.theme.CharcoalBorder
import com.miadfm.podcasts.ui.theme.CharcoalCard
import com.miadfm.podcasts.ui.theme.ErrorRed
import com.miadfm.podcasts.ui.theme.TextMuted
import com.miadfm.podcasts.ui.theme.TextPrimary
import com.miadfm.podcasts.ui.theme.TextSecondary
import com.miadfm.podcasts.viewmodel.PinMode

@Composable
fun PinAuthScreen(
    pinMode: PinMode,
    isPinSet: Boolean,
    errorMessage: String?,
    onSubmitPin: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = appStrings()
    var enteredDigits by remember(pinMode) { mutableStateOf("") }

    val (titleText, subtitleText) = when (pinMode) {
        PinMode.SETUP_ENTER_NEW -> strings.setVaultPinTitle to strings.setVaultPinSubtitle
        PinMode.SETUP_CONFIRM_NEW -> strings.confirmPinTitle to strings.confirmPinSubtitle
        PinMode.UNLOCK -> strings.unlockVaultTitle to strings.unlockVaultSubtitle
        PinMode.CHANGE_VERIFY_CURRENT -> strings.verifyCurrentPinTitle to strings.verifyCurrentPinSubtitle
        PinMode.CHANGE_ENTER_NEW -> strings.changePinNewTitle to strings.changePinNewSubtitle
        PinMode.CHANGE_CONFIRM_NEW -> strings.confirmPinTitle to strings.confirmPinSubtitle
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("pin_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lock Icon
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(BlueContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPinSet) Icons.Default.Lock else Icons.Default.VpnKey,
                    contentDescription = strings.vaultTitle,
                    tint = BlueAccent,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Title and Subtitle
            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            // PIN Dots Indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.height(24.dp)
            ) {
                val displayCount = maxOf(4, enteredDigits.length)
                for (i in 0 until displayCount) {
                    val isFilled = i < enteredDigits.length
                    Box(
                        modifier = Modifier
                            .size(if (isFilled) 14.dp else 12.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) BlueAccent else CharcoalBorder
                            )
                    )
                }
            }

            // Error Message (Must show exactly "Incorrect PIN" when wrong)
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ErrorRed,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("pin_error_text")
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Custom Numeric Keypad (0-9, backspace, submit)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("backspace", "0", "submit")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            when (key) {
                                "backspace" -> {
                                    KeypadIconButton(
                                        icon = Icons.Default.Backspace,
                                        contentDescription = strings.deleteDigit,
                                        testTag = "keypad_backspace",
                                        onClick = {
                                            if (enteredDigits.isNotEmpty()) {
                                                enteredDigits = enteredDigits.dropLast(1)
                                            }
                                        }
                                    )
                                }
                                "submit" -> {
                                    val canSubmit = enteredDigits.length in 4..8
                                    KeypadIconButton(
                                        icon = Icons.Default.Check,
                                        contentDescription = strings.submitPin,
                                        testTag = "keypad_submit",
                                        tint = if (canSubmit) BlueAccentLight else TextMuted,
                                        background = if (canSubmit) BlueContainer else CharcoalCard,
                                        onClick = {
                                            if (canSubmit) {
                                                onSubmitPin(enteredDigits)
                                                enteredDigits = ""
                                            }
                                        }
                                    )
                                }
                                else -> {
                                    KeypadDigitButton(
                                        digit = key,
                                        testTag = "keypad_digit_$key",
                                        onClick = {
                                            if (enteredDigits.length < 8) {
                                                enteredDigits += key
                                                if (pinMode == PinMode.UNLOCK && enteredDigits.length == 8) {
                                                    onSubmitPin(enteredDigits)
                                                    enteredDigits = ""
                                                }
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KeypadDigitButton(
    digit: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = CircleShape,
        color = CharcoalCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
    tint: Color = TextPrimary,
    background: Color = CharcoalCard
) {
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = CircleShape,
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.dp, CharcoalBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
