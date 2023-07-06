/*
 *     SPDX-License-Identifier: AGPL-3.0-only
 *
 *     Copyright (C) RainbowDashLabs and Contributor
 */

package de.chojo.krile;

import de.chojo.krile.tagimport.repo.RawRepository;
import de.chojo.krile.tagimport.tag.entities.FileMeta;
import de.chojo.krile.tagimport.tag.entities.RawAuthor;
import de.chojo.krile.tagimport.tag.entities.RawTagMeta;
import de.chojo.krile.tagimport.tag.parsing.TagParser;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TagParserTest {
    private static RawRepository repo;
    private static TagParser testTag;
    private static TagParser longTag;

    @BeforeAll
    static void beforeAll() throws GitAPIException, IOException {
        repo = TestRepository.root();
        testTag = TagParser.parse(repo, repo.tagPath().resolve("test_tag.md"));
        longTag = TagParser.parse(repo, repo.tagPath().resolve("long tag.md"));
    }

    @Test
    void parse() {
        TagParser testTag = TagParser.parse(repo, repo.tagPath().resolve("test_tag.md"));
    }

    @Test
    void getAuthors() throws GitAPIException {
        Collection<RawAuthor> rawAuthors = testTag.getAuthors();
        Assertions.assertEquals(1, rawAuthors.size());
    }

    @Test
    @Disabled
    void fileMeta() throws GitAPIException {
        FileMeta meta = testTag.fileMeta();
        Assertions.assertNotEquals(meta.created().when(), meta.modified().when());
    }

    @Test
    void tagContent() throws IOException {
        String content = testTag.tagContent();
        Matcher matcher = Pattern.compile("^---$").matcher(content);
        Assertions.assertFalse(matcher.find());
        Assertions.assertTrue(content.contains("This is example text for a text tag"));
    }

    @Test
    void splitContent() throws IOException, GitAPIException {
        List<String> content = longTag.tag().splitText();
        Assertions.assertTrue(content.size() > 1);
        Assertions.assertEquals(8 , content.size());
        for (String text : content) {
            Assertions.assertFalse(text.contains("<new_page>"));
        }
    }

    @Test
    void tagMeta() throws IOException {
        RawTagMeta meta = testTag.tagMeta();
        Assertions.assertEquals("test tag", meta.id());
        Assertions.assertEquals("test", meta.tag());
        Assertions.assertEquals(List.of("test1", "test2"), meta.alias());
        Assertions.assertEquals(List.of("test"), meta.category());
        Assertions.assertEquals("https://avatars.githubusercontent.com/u/46890129?s=48&v=4", meta.image());
    }

    @Test
    void tag() {
        Assertions.assertDoesNotThrow(() -> testTag.tag());
    }

    @AfterAll
    static void afterAll() throws IOException {
        // repo.close();
    }
}
