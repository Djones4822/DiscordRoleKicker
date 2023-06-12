/*
 * MIT License
 *
 * Copyright (c) 2020 Rosetta Roberts
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.thenumberone.discord.rolekickerbot.util

import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec
import discord4j.rest.util.Color
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import java.time.Instant
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Component
class EmbedHelper(val self: SelfBotInfo) {
    companion object {
        val logger: Logger = LoggerFactory.getLogger(EmbedHelper::class.java)
    }

    suspend fun withTemplate(
        title: String? = null,
        builder: LegacyEmbedCreateSpec.() -> Unit = {}
    ): LegacyEmbedCreateSpec.() -> Unit {
        val name = self.getBotName()
        val imgUrl = self.getImgUrl()

        return {
            setColor(Color.of(255, 255, 254))
            setAuthor(name, null, imgUrl)
            setTimestamp(Instant.now())
            if (title != null) setTitle(title)
            builder()
        }
    }

    suspend fun respondTo(event: MessageCreateEvent, title: String? = null, builder: LegacyEmbedCreateSpec.() -> Unit) {
        logger.debug("Responding to event.")
        send(event.message.channel.awaitFirstOrNull() ?: return, title, builder)
    }

    suspend fun send(channel: MessageChannel, title: String? = null, builder: LegacyEmbedCreateSpec.() -> Unit) {
        logger.debug("Sending embed to channel.")
        channel.createEmbed(withTemplate(title, builder)).awaitSingle()
    }
}