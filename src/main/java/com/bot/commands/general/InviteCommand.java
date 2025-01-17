package com.bot.commands.general;

import com.bot.commands.GeneralCommand;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

public class InviteCommand extends GeneralCommand {

    public InviteCommand() {
        this.name = "invite";
        this.help = "Sends a link to invite the bot to your server";
        this.arguments = "";
        this.guildOnly = false;
    }

    @Override
    protected void executeCommand(CommandEvent commandEvent) {

        // No need to check perms here
        User user = commandEvent.getAuthor();
        PrivateChannel privateChannel = user.openPrivateChannel().complete();
        privateChannel.sendMessage("https://discordapp.com/oauth2/authorize?client_id=276855867796881408&scope=bot&permissions=1744305473").queue();
    }
}
