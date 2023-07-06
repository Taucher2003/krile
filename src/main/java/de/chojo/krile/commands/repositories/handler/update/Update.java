package de.chojo.krile.commands.repositories.handler.update;

import de.chojo.jdautil.configuratino.Configuration;
import de.chojo.jdautil.interactions.slash.structure.handler.SlashHandler;
import de.chojo.jdautil.wrapper.EventContext;
import de.chojo.krile.configuration.ConfigFile;
import de.chojo.krile.data.access.Guilds;
import de.chojo.krile.data.dao.tagguild.GuildRepository;
import de.chojo.krile.service.RepoUpdateService;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

public class Update implements SlashHandler {
    private final Guilds guilds;
    private final RepoUpdateService updateService;
    private final Configuration<ConfigFile> configuration;

    public Update(Guilds guilds, RepoUpdateService updateService, Configuration<ConfigFile> configuration) {
        this.guilds = guilds;
        this.updateService = updateService;
        this.configuration = configuration;
    }

    @Override
    public void onSlashCommand(SlashCommandInteractionEvent event, EventContext context) {
        Optional<GuildRepository> repository = guilds.guild(event).repositories().byId(event.getOption("repository", OptionMapping::getAsInt));
        if (repository.isEmpty()) {
            event.reply("Unknown Repository").setEphemeral(true).queue();
            return;
        }
        Instant checked = repository.get().data().get().checked();
        int minCheck = configuration.config().repositories().minCheck();
        if (checked.isAfter(Instant.now().minus(minCheck, ChronoUnit.MINUTES))) {
            event.reply("This repository was already checked in the last %d minutes. Please wait some time".formatted(minCheck)).setEphemeral(true).queue();
            return;
        }
        event.reply("Repository is scheduled for an update").setEphemeral(true).queue();
        updateService.schedule(repository.get());
    }

    @Override
    public void onAutoComplete(CommandAutoCompleteInteractionEvent event, EventContext context) {
        List<Command.Choice> choices = guilds.guild(event)
                .repositories()
                .complete(event.getFocusedOption().getValue())
                .stream()
                .map(v -> new Command.Choice(v.identifier(), v.id()))
                .toList();
        event.replyChoices(choices).queue();
    }
}
