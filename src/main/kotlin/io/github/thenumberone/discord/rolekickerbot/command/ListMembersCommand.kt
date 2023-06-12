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

package io.github.thenumberone.discord.rolekickerbot.command

import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.legacy.LegacyEmbedCreateSpec
import io.github.thenumberone.discord.rolekickerbot.data.RoleKickRule
import io.github.thenumberone.discord.rolekickerbot.data.TrackedMember
import io.github.thenumberone.discord.rolekickerbot.service.RoleKickService
import io.github.thenumberone.discord.rolekickerbot.util.EmbedHelper
import io.github.thenumberone.discord.rolekickerbot.util.pagedMessage
import io.github.thenumberone.discord.rolekickerbot.util.toAbbreviatedString
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import javax.annotation.Priority

private const val pageCutoff = 20
private const val title = "List Tracked Members"

@Component
@Priority(0)
class ListMembersCommand(private val roleKickService: RoleKickService, private val embedHelper: EmbedHelper) :
    MultipleNamesCommand, AdminCommand {
    override val names: Set<String> = setOf("listmembers", "listmember")

    override suspend fun execIfPrivileged(event: MessageCreateEvent, commandText: String) {
        val guildId = event.guildId.orElse(null) ?: return
        val members = roleKickService.getTrackedMembers(guildId)
        val rules = roleKickService.getRules(guildId).map { it.roleId to it }.toMap()

        val roleIdsToMembers = members.groupBy { it.roleId }

        if (members.size == 0) {
            embedHelper.respondTo(event, title) {
                setDescription("No Members Currently Tracked")
            }
        } else if (members.size < pageCutoff) {
            embedHelper.respondTo(event, title) {
                for ((roleId, roleMembers) in roleIdsToMembers) {
                    fillSpec(rules.getValue(roleId), roleMembers)
                }
            }
        } else {
            val pages = roleIdsToMembers.flatMap { (roleId, trackedMembers) ->
                trackedMembers.chunked(pageCutoff).map { roleId to it }
            }
            val template = embedHelper.withTemplate(title)

            pagedMessage(event.message.channel.awaitSingle(), pages.size) { index, embedCreateSpec: LegacyEmbedCreateSpec ->
                val (roleId, trackedMembers) = pages[index]
                embedCreateSpec.apply {
                    template()
                    setFooter("Page ${index + 1}/${pages.size}", null)
                    fillSpec(rules.getValue(roleId), trackedMembers)
                }
            }.awaitFirstOrNull()
        }
    }

    fun LegacyEmbedCreateSpec.fillSpec(rule: RoleKickRule, trackedMembers: List<TrackedMember>) {
        val now = Instant.now()
        val memberMentions = mutableListOf<String>()
        val timeTilWarnLines = mutableListOf<String>()
        val timeTilKickLines = mutableListOf<String>()

        for (member in trackedMembers) {
            val timeStarted = member.startedTracking

            memberMentions.add(mentionUser(member.memberId))
            val timeTilWarning = rule.timeTilWarning - Duration.between(timeStarted, now)
            val timeTilKick = timeTilWarning + rule.timeTilKick
            timeTilWarnLines.add(
                if (member.triedWarn) "Warned"
                else timeTilWarning.toAbbreviatedString()
            )
            timeTilKickLines.add(
                if (member.triedKick) "Failed to Kick"
                else timeTilKick.toAbbreviatedString()
            )
        }

        addField("Role", mentionRole(rule.roleId), false)
        addField("User", memberMentions.joinToString("\n"), true)
        addField("Time Til Kick", timeTilKickLines.joinToString("\n"), true)
    }

}