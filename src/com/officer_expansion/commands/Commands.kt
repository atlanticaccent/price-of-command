package com.officer_expansion.commands

import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.officer_expansion.ConditionManager
import com.officer_expansion.conditions.Fatigue
import com.officer_expansion.conditions.GraveInjury
import com.officer_expansion.conditions.Wound
import com.officer_expansion.conditions.tryInflictAppend
import com.officer_expansion.playerOfficers
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console

class TestCommand : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        return CommandResult.SUCCESS
    }
}

fun MutableList<OfficerDataAPI>.findByName(name: String): PersonAPI? =
    this.find { officer -> officer.person.nameString.contains(name, true) }?.person

class InjureOfficer : BaseCommand {
    override fun runCommand(officer_name: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(officer_name)
        if (officer != null) {
            try {
                ConditionManager.fatigueOfficer(officer)
                val res = ConditionManager.injureOfficer(officer)
                Console.showMessage("Succeeded injuring officer: $res")
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class GravelyInjureOfficer : BaseCommand {
    override fun runCommand(officer_name: String, p1: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(officer_name)
        if (officer != null) {
            try {
                GraveInjury(officer, ConditionManager.now).tryInflictAppend()
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class HealOfficer : BaseCommand {
    override fun runCommand(allArgs: String, ctx: BaseCommand.CommandContext): CommandResult {
        try {
            val args = allArgs.split("\\s+".toRegex())
            val officerName = args.first()
            val count = args.getOrNull(1)?.toIntOrNull() ?: 1

            val officer =
                playerOfficers().findByName(officerName) ?: run {
                    Console.showMessage("Could not find officer by name")
                    return CommandResult.BAD_SYNTAX
                }

            ConditionManager.conditionMap[officer]?.filterIsInstance<Wound>()
                ?.run { subList(0, minOf(this.size, count)) }?.forEach { injury ->
                injury.expired = true
            }
            return CommandResult.SUCCESS
        } catch (e: Exception) {
            Console.showException("Error: ", e)
        }

        return CommandResult.BAD_SYNTAX
    }
}

class FatigueOfficer : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(args)

        if (officer != null) {
            try {
                ConditionManager.fatigueOfficer(officer)
                Console.showMessage("Succeeded fatiguing officer")
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class RestOfficer : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(args)

        if (officer != null) {
            try {
                val fatigue = ConditionManager.conditionMap[officer]?.filterIsInstance<Fatigue>()?.firstOrNull()
                if (fatigue != null) {
                    fatigue.expired = true
                    Console.showMessage("Succeeded resting officer")
                } else {
                    Console.showMessage("Officer is not fatigued")
                }
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}