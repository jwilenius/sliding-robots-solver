package net.booru.slidingrobots.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ArgumentParser {
    private final Logger iLogger = LoggerFactory.getLogger(ArgumentParser.class);

    private final HashMap<String, Argument> iArguments = new HashMap<>();
    private final HashMap<String, List<String>> iConflicts = new HashMap<>();
    private final List<String> iRequiredArguments = new ArrayList<>();

    public ArgumentParser() {
        withGeneralArgument("--help", List.of(), "Show help message.");
    }

    public ArgumentParser setRequired(final List<String> requiredArguments) {
        if (!requiredArguments.stream().allMatch(iArguments::containsKey)) {
            throw new IllegalArgumentException("All required arguments must be added before setting required state.");
        }
        iRequiredArguments.addAll(requiredArguments);
        return this;
    }

    public ArgumentParser addConflicts(final String argument, final List<String> otherArguments) {
        final List<String> allArguments = new LinkedList<>(otherArguments);
        allArguments.add(argument);
        if (!allArguments.stream().allMatch(iArguments::containsKey)) {
            throw new IllegalArgumentException("All required arguments must be added before setting required state.");
        }

        iConflicts.put(argument, List.copyOf(otherArguments));
        return this;
    }

    public ArgumentParser withSpecificArgument(final String name, final List<String> allowedValues, final String help) {
        if (iArguments.containsKey(name)) {
            throw new IllegalArgumentException("duplicate argument: " + name);
        }
        addArgument(name, new Argument(name, allowedValues, help, false));
        return this;
    }

    public ArgumentParser withGeneralArgument(final String name, final List<String> valueDescription,
                                              final String help) {
        if (iArguments.containsKey(name)) {
            throw new IllegalArgumentException("duplicate argument: " + name);
        }
        addArgument(name, new Argument(name, valueDescription, help, true));
        return this;
    }

    private void addArgument(final String name, final Argument argument) {
        if (!name.startsWith("--")) {
            outputHelpAndExit(name, "Argument must start with --");
        }
        iArguments.put(name, argument);
    }

    public void parseArguments(final String[] arguments) {
        if (Arrays.stream(arguments).anyMatch(s -> s.equals("--help") || s.equals("-h"))) {
            outputHelpAndExit();
        }

        Argument currentArgument = null;
        for (String token : arguments) {
            token = token.trim();
            if (currentArgument == null && token.startsWith("--")) {
                if (!iArguments.containsKey(token)) {
                    outputHelpAndExit(token, "Missing argument");
                }
                currentArgument = iArguments.get(token);

                if (currentArgument.getValueDescriptions().isEmpty()) { // do not expect a value next
                    currentArgument.setValue("");
                    currentArgument = null;
                }

            } else if (currentArgument == null && !token.startsWith("--")) {
                outputHelpAndExit(token, "expected argument, got: " + token);

            } else if (currentArgument != null) {
                if (currentArgument.isGeneralArgument()) {
                    currentArgument.setValue(token);
                } else {
                    if (!currentArgument.getValueDescriptions().contains(token)) {
                        outputHelpAndExit(token,
                                          "Argument " + currentArgument.getName() + " has incorrect value: " + token);
                    }
                    currentArgument.setValue(token);
                }
                currentArgument = null;
            } else {
                outputHelpAndExit(token, "Unexpected argument parser state.");
            }
        }

        for (String requiredArgument : iRequiredArguments) {
            if (!iArguments.get(requiredArgument).hasValue()) {
                outputHelpAndExit(requiredArgument, "missing required argument: " + requiredArgument);
            }
        }

        for (String argument : iConflicts.keySet()) {
            if (iArguments.get(argument).hasValue()) {
                List<String> conflicts = iConflicts.get(argument).stream()
                                                   .filter(a -> iArguments.get(a).hasValue())
                                                   .collect(Collectors.toList());
                if (!conflicts.isEmpty()) {
                    outputHelpAndExit(argument, "has conflict with other argument(s): " + conflicts);
                }
            }
        }

    }

    private void outputHelpAndExit(final String token, final String message) {
        iLogger.info("!! The following argument is problematic: {}", token);
        if (!message.isEmpty()) {
            iLogger.info(message);
        }
        outputHelpAndExit();
    }

    private void outputHelpAndExit() {
        outputHelp();
        System.exit(1);
    }

    public void outputHelp() {
        iLogger.info("--------------------------------------------");
        iLogger.info("Help:");
        final String indent = "    {}";
        for (var entry : iArguments.values()) {
            iLogger.info(indent, entry.toString());
        }
        iLogger.info("Required arguments:");
        iLogger.info(indent, iRequiredArguments);
        iLogger.info("Disjoint arguments:");
        iLogger.info(indent, iConflicts);
        iLogger.info("--------------------------------------------");
    }

    public Optional<Argument> get(final String name) {
        final Argument argument = iArguments.get(name);
        if (argument != null && argument.hasValue()) {
            return Optional.of(argument);
        }
        return Optional.empty();
    }

    public static class Argument {
        private final List<String> iValueDescriptions;
        private final String iHelp;
        private final String iName;
        private String iValue = null;
        private final boolean iIsGeneralArgument;

        public Argument(final String name, final List<String> valueDescriptions, final String help,
                        final boolean isGeneralArgument) {
            iName = name;
            iValueDescriptions = valueDescriptions;
            iHelp = help;
            iIsGeneralArgument = isGeneralArgument;
        }

        public List<String> getValueDescriptions() {
            return iValueDescriptions;
        }

        public String getHelp() {
            return iHelp;
        }

        private void setValue(final String value) {
            iValue = value;
        }

        public String getValue() {
            return iValue;
        }

        public int getValueAsInt() {
            return Integer.parseInt(iValue);
        }

        @Override
        public String toString() {
            if (iValueDescriptions.isEmpty()) {
                return iName + "  : " + iHelp;
            }
            final StringJoiner joiner = new StringJoiner("|", "", "");
            iValueDescriptions.forEach(joiner::add);
            return iName + " " + joiner + "  : " + iHelp;
        }

        public boolean isGeneralArgument() {
            return iIsGeneralArgument;
        }

        public boolean hasValue() {
            return iValue != null;
        }

        public String getName() {
            return iName;
        }
    }

}
