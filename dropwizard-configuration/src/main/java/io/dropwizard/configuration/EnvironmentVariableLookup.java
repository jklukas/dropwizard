package io.dropwizard.configuration;

import org.apache.commons.lang3.text.StrLookup;

/**
 * A custom {@link org.apache.commons.lang3.text.StrLookup} implementation using environment variables as lookup source.
 * A switch syntax is also supported that allows injection of arbitrary values based on the environment variable's value.
 */
public class EnvironmentVariableLookup extends StrLookup<Object> {
    private final boolean strict;

    /**
     * Create a new instance with strict behavior.
     */
    public EnvironmentVariableLookup() {
        this(true);
    }

    /**
     * Create a new instance.
     *
     * @param strict {@code true} if looking up undefined environment variables should throw a
     *               {@link UndefinedEnvironmentVariableException}, {@code false} otherwise.
     * @throws UndefinedEnvironmentVariableException if the environment variable doesn't exist and strict behavior
     *                                               is enabled.
     */
    public EnvironmentVariableLookup(boolean strict) {
        this.strict = strict;
    }

    /**
     * {@inheritDoc}
     *
     * @throws UndefinedEnvironmentVariableException if the environment variable doesn't exist and strict behavior
     *                                               is enabled.
     */
    @Override
    public String lookup(String key) {
        if (key.contains("=")) {
            // '=' is not a valid character in POSIX, Linux, or Windows environment variable names,
            // so this must be a switch statement.
            return lookupSwitch(key);
        }

        final String value = System.getenv(key);

        if (value == null && strict) {
            throw new UndefinedEnvironmentVariableException("The environment variable '" + key
                    + "' is not defined; could not substitute the expression '${"
                    + key + "}'.");
        }

        return value;
    }

    private String lookupSwitch(String statement) {
        // The key is separated from the cases by whitespace.
        final String[] keyAndSwitch = statement.split("\\s", 2);
        final String key = keyAndSwitch[0];
        if (keyAndSwitch.length == 1) {
            throw new UndefinedEnvironmentVariableException("Environment variable names "
                + "may not contain '='; could not substitute the expression '${"
                + key + "}'.");
        }
        final String switchBody = keyAndSwitch[1];

        final String value = System.getenv(key);

        if (value == null && strict) {
            throw new UndefinedEnvironmentVariableException("The environment variable '" + key
                + "' is not defined; could not substitute the expression '${"
                + statement + "}'.");
        }

        // TODO: Use a StrTokenizer here rather than a regex to handle quoted values
        final String[] cases = switchBody.split("\\s");
        for (String c: cases) {
            final String[] tokens = c.split("=");
            if (tokens[0].equals(value)) {
                return tokens[1];
            }
        }

        return null;
    }
}
