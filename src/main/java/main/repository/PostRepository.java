package main.repository;

import main.constructorforquery.DateWithQuantityPosts;
import main.enums.ModerationStatus;
import main.model.Post;
import main.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Integer> {
    @Query("SELECT p " +
        "FROM Post p " +
        "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
        "ORDER BY p.time DESC")
    Page<Post> getListRecentPost(ModerationStatus modStatus, short isActive, Date currentTime, Pageable pageable);

    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "ORDER BY p.time")
    Page<Post> getListEarlyPost(ModerationStatus modStatus, short isActive,  Date currentTime, Pageable pageable);

    @Query("SELECT p " +
            "FROM Post p " +
            "LEFT JOIN PostVote pvl on pvl.post = p.id and pvl.value = 1 " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "GROUP BY p.id " +
            "ORDER BY COUNT(pvl) DESC")
    Page<Post> getListBestPost(ModerationStatus modStatus,  short isActive, Date currentTime, Pageable pageable);

    @Query("SELECT p " +
            "FROM Post p " +
            "LEFT JOIN PostComment pc ON pc.post = p.id " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "GROUP BY p.id " +
            "ORDER BY COUNT(pc) DESC")
    Page<Post> getListPopularPost(ModerationStatus modStatus,short isActive, Date currentTime, Pageable pageable);


    ///////By User
    @Query("FROM Post p " +
        "WHERE p.isActive = :isActive AND p.user = :user AND p.time < :currentTime")
    Page<Post> getListInactivePostsByUser(short isActive, User user, Date currentTime, Pageable pageable);

    @Query("FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.user = :user AND p.time < :currentTime")
    Page<Post> getListPostsByUser(short isActive, User user, ModerationStatus modStatus, Date currentTime, Pageable pageable);

    //////////By search query
    /**Метод getListPostsBySearchQuery возвращает все посты по поисковому запросу, поиск осуществляется в заголовке поста и сожержанию текста
     * в случаи если запрос пришел пустой строкой, поиск осуществляется так же, в каждом посте есть пустая строка
     * */
    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "AND (p.title LIKE %:textSearch% OR p.text LIKE %:textSearch%)" +
            "ORDER BY p.time DESC")
    Page<Post> getListPostsBySearchQuery(ModerationStatus modStatus, short isActive, Date currentTime, Pageable pageable, String textSearch);

    //By date
    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "AND p.time BETWEEN :startDate AND :endDay " +
            "ORDER BY p.time DESC")
    Page<Post> getListPostsByDate(ModerationStatus modStatus, short isActive, Date currentTime, Pageable pageable, Date startDate, Date endDay);

    //Moderation
    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.moderator = :moderator ")
    Page<Post> getListPostsModeration(ModerationStatus modStatus, short isActive, Pageable pageable, User moderator);

    @Query("SELECT p " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus ")
    Page<Post> getListNewPostsForModeration(ModerationStatus modStatus, short isActive, Pageable pageable);

    @Query("FROM Post " +
            "WHERE id = :id AND isActive = :isActive AND moderationStatus = :modStatus AND time <= :currentTime ")
    Post findActivePostById(int id, ModerationStatus modStatus, short isActive, Date currentTime);

    //For Calendar
    @Query("SELECT YEAR(time) " +
            "FROM Post " +
            "WHERE isActive = :isActive AND moderationStatus = :modStatus AND time <= :currentTime " +
            "GROUP BY YEAR(time) " +
            "ORDER BY YEAR(time)")
    List<Integer> getListYearsWhenPublishedPosts(ModerationStatus modStatus, short isActive, Date currentTime);

    @Query("SELECT NEW main.constructorforquery.DateWithQuantityPosts(DATE(time), COUNT(*)) " +
            "FROM Post " +
            "WHERE isActive = :isActive AND moderationStatus = :modStatus AND time <= :currentTime " +
            "GROUP BY DATE(time) " +
            "ORDER BY DATE(time)")
    List<DateWithQuantityPosts> getListDateWithQuantityPosts(ModerationStatus modStatus, short isActive, Date currentTime);

    @Query("SELECT NEW main.constructorforquery.DateWithQuantityPosts(DATE(time), COUNT(*)) " +
            "FROM Post " +
            "WHERE isActive = :isActive AND moderationStatus = :modStatus AND time <= :currentTime " +
            "AND YEAR(time) = :year " +
            "GROUP BY DATE(time) " +
            "ORDER BY DATE(time)")
    List<DateWithQuantityPosts> getListDateWithQuantityPostsByDate(ModerationStatus modStatus, short isActive, Date currentTime, Integer year);

    //Statistic(
    @Query("SELECT COUNT(pv) " +
            "FROM Post p " +
            "LEFT JOIN PostVote pv on pv.post = p.id and pv.value = :value " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "AND p.user = :user")
    Long findAllLikeDislikeByUser(ModerationStatus modStatus, short isActive, Date currentTime, User user, short value);

    @Query("SELECT SUM(p.viewCount) " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "AND p.user = :user")
    Long getViewCountPostsUser(ModerationStatus modStatus, short isActive, Date currentTime, User user);

    @Query("SELECT MIN(p.time) " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime " +
            "AND p.user = :user")
    Timestamp getFirstPostPublicationByUser(ModerationStatus modStatus, short isActive, Date currentTime, User user);

    @Query("SELECT COUNT(pv) " +
            "FROM Post p " +
            "LEFT JOIN PostVote pv on pv.post = p.id and pv.value = :value " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime ")
    Long findAllLikeDislikeAll(ModerationStatus modStatus, short isActive, Date currentTime, short value);

    @Query("SELECT SUM(p.viewCount) " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime")
    Long getViewCountPostsAll(ModerationStatus modStatus, short isActive, Date currentTime);

    @Query("SELECT MIN(p.time) " +
            "FROM Post p " +
            "WHERE p.isActive = :isActive AND p.moderationStatus = :modStatus AND p.time <= :currentTime")
    Timestamp getFirstPostPublicationAll(ModerationStatus modStatus, short isActive, Date currentTime);
    //)
}











