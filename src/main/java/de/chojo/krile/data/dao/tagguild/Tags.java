package de.chojo.krile.data.dao.tagguild;

import de.chojo.krile.data.access.Authors;
import de.chojo.krile.data.access.Categories;
import de.chojo.krile.data.dao.TagGuild;
import de.chojo.krile.data.dao.repository.tags.Tag;
import de.chojo.sadu.wrapper.util.Row;
import org.intellij.lang.annotations.Language;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static de.chojo.krile.data.bind.StaticQueryAdapter.builder;

public class Tags {
    private final TagGuild guild;
    private final Repositories repositories;
    private final Categories categories;
    private final Authors authors;

    public Tags(TagGuild guild, Repositories repositories, Categories categories, Authors authors) {
        this.guild = guild;
        this.repositories = repositories;
        this.categories = categories;
        this.authors = authors;
    }

    public List<CompletedTag> complete(String value) {
        @Language("postgresql")
        var select = """
                with ranked_tags as
                         (SELECT row_number() over (PARTITION BY tag ORDER BY gr.priority * rt.global_prio DESC) as rank,
                                 gr.repository_id,
                                 gr.priority                                                                     as repo_prio,
                                 global_prio,
                                 rt.id,
                                 tag,
                                 rt.prio                                                                     as tag_prio,
                                 r.identifier
                          FROM guild_repository gr
                                   LEFT JOIN repo_tags rt on gr.repository_id = rt.repository_id
                                   LEFT JOIN repository r on gr.repository_id = r.id
                          WHERE global_prio = 1
                            and tag ILIKE ('%' || ? || '%')
                            and gr.guild_id = ?)
                SELECT id, case when rank = 1 then tag else tag || ' (' || identifier || ')' end as name
                FROM ranked_tags;
                """;

        return builder(CompletedTag.class)
                .query(select)
                .parameter(stmt -> stmt.setString(value).setLong(guild.id()))
                .readRow(CompletedTag::build)
                .allSync();
    }

    public Optional<Tag> getById(int tag) {
         @Language("postgresql")
          var select = """
              SELECT repository_id, id, tag_id, tag, content FROM tag t WHERE id = ?""";
        return builder(Tag.class)
                .query(select)
                .parameter(stmt -> stmt.setInt(tag))
                .readRow(row -> Tag.build(row, repositories.byId(row.getInt("repository_id")).get(), categories, authors))
                .firstSync();
    }

    public void used(Tag tag) {
         @Language("postgresql")
          var insert = """
              INSERT INTO tag_stat as s (guild_id, tag_id)
              VALUES (?, ?)
              ON CONFLICT (guild_id, tag_id)
              DO UPDATE SET views = s.views + 1""";
         builder()
                 .query(insert)
                 .parameter(stmt -> stmt.setLong(guild.id()).setInt(tag.id()))
                 .insert()
                 .send();

    }

    public record CompletedTag(int id, String name) {
        public static CompletedTag build(Row row) throws SQLException {
            return new CompletedTag(row.getInt("id"), row.getString("name"));
        }
    }
}
