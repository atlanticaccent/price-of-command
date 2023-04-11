package com.laird

import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandResult

class Commands: BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        return CommandResult.SUCCESS
    }
}