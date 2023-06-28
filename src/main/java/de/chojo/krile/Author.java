package de.chojo.krile;

import org.eclipse.jgit.lib.PersonIdent;

public record  Author(String name, String mail) {
    public static Author of(PersonIdent authorIdent) {
        return new Author(authorIdent.getName(), authorIdent.getEmailAddress());
    }
}
