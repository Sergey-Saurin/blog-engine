package main.repository;

import main.model.Post;
import main.model.PostVote;
import main.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PostVoteRepository extends JpaRepository<PostVote, Integer> {
    @Query("SELECT COUNT(id) FROM PostVote WHERE value = :value AND post = :post")
    int findAllLikeDislikeByPost(@Param("value") short value, @Param("post") Post post);

    @Query("FROM PostVote WHERE value = :value AND post = :post AND user = :user")
    PostVote findVoiceUserToPost(User user, Post post, short value);
}
