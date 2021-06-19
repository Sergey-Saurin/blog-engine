package main.repository;

import main.model.Post;
import main.model.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Integer> {
    @Query("SELECT COUNT(id) FROM PostComment WHERE post = :post")
    int getQuantityAllByPost(@Param("post") Post post);

    List<PostComment> findAllCommentsByPost(Post postModel);
}

