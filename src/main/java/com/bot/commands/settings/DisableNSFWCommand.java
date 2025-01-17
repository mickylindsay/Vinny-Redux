package com.bot.commands.settings;

import com.bot.commands.ModerationCommand;
import com.bot.db.ChannelDAO;
import com.jagrosh.jdautilities.command.CommandEvent;

public class DisableNSFWCommand extends ModerationCommand {

    private ChannelDAO channelDAO;

    public DisableNSFWCommand() {
        this.name = "disablensfw";
        this.arguments = "";
        this.help = "Disables NSFW commands in the text channel it is posted in.";

        this.channelDAO = ChannelDAO.getInstance();
    }

    @Override
    protected void executeCommand(CommandEvent commandEvent) {
        if (channelDAO.setTextChannelNSFW(commandEvent.getTextChannel(), false)) {
            commandEvent.getMessage().addReaction(commandEvent.getClient().getSuccess()).queue();
        } else {
            commandEvent.reply(commandEvent.getClient().getError() + " Something went wrong, please try again later or contact an admin on the support server.");
            metricsManager.markCommandFailed(this, commandEvent.getAuthor(), commandEvent.getGuild());
        }
    }
}
