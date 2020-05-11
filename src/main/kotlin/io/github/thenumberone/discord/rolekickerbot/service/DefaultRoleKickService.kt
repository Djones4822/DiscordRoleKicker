package io.github.thenumberone.discord.rolekickerbot.service

import discord4j.rest.util.Snowflake
import io.github.thenumberone.discord.rolekickerbot.data.RoleKickRule
import io.github.thenumberone.discord.rolekickerbot.repository.RoleKickRuleRepository
import io.github.thenumberone.discord.rolekickerbot.service.RoleKickService.AddedOrUpdated
import org.springframework.stereotype.Component

@Component
class DefaultRoleKickService(val roleKickRuleRepository: RoleKickRuleRepository) : RoleKickService {
    override suspend fun addRole(spec: RoleKickRule) {
        roleKickRuleRepository.addRule(spec)
    }

    override suspend fun updateRole(spec: RoleKickRule) {
        roleKickRuleRepository.updateRule(spec)
    }

    override suspend fun removeRole(server: Snowflake, role: Snowflake) {
        roleKickRuleRepository.removeRule(server, role)
    }

    override suspend fun removeServer(server: Snowflake) {
        roleKickRuleRepository.removeServer(server)
    }

    override suspend fun addOrUpdateRole(roleRuleSpec: RoleKickRule): AddedOrUpdated {
        return roleKickRuleRepository.addOrUpdateRole(roleRuleSpec)
    }
}