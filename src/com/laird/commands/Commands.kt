package com.laird.commands

import com.fs.starfarer.api.Global
import com.laird.ConditionManager
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console

class TestCommand: BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        return CommandResult.SUCCESS
    }
}

class InjureOfficer: BaseCommand {
    override fun runCommand(officer_name: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officers = Global.getSector().playerFleet.fleetData.officersCopy

        val officer = officers.find { officer -> officer.person.nameString.contains(officer_name, true) }?.person
        if (officer != null) {
            try {
                ConditionManager.injureOfficer(officer)
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class HealOfficer {

}

class FatigueOfficer {

}

class RestOfficer {

}