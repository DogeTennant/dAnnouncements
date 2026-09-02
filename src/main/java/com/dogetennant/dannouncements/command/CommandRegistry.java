package com.dogetennant.dannouncements.command;

import com.dogetennant.dannouncements.command.subcommand.SubCommand;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class CommandRegistry {

    private final Map<String, SubCommand> commands = new LinkedHashMap<>();

    public void register(SubCommand command) {
        commands.put(command.getName().toLowerCase(), command);
    }

    public Optional<SubCommand> get(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase()));
    }

    public Collection<SubCommand> getAll() {
        return Collections.unmodifiableCollection(commands.values());
    }
}
