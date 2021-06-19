package main.repository;

import main.enums.ModerationStatus;
import main.model.Post;
import main.model.Tag2Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface Tag2PostRepository extends JpaRepository<Tag2Post, Integer> {
    //By Tag
    @Query("SELECT post " +
            "FROM Tag2Post t2p " +
            "WHERE t2p.tag.name = :tagName " +
            "AND t2p.post.isActive = :isActive AND t2p.post.moderationStatus = :modStatus AND t2p.post.time <= :currentTime ")
    Page<Post> getListPostsByTag(ModerationStatus modStatus, short isActive, Date currentTime, Pageable pageable, String tagName);

    @Query("SELECT tag.name " +
            "FROM Tag2Post t2p " +
            "WHERE t2p.post = :post ")
    List<String> findAllTagsNameByPost(Post post);
}
